/*
 * CoolReader for Android
 * Copyright (C) 2010-2014 Vadim Lopatin <coolreader.org@gmail.com>
 * Copyright (C) 2018 S-trace <S-trace@list.ru>
 * Copyright (C) 2020-2022 Aleksey Chernov <valexlin@gmail.com>
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.coolreader.crengine;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.HandlerThread;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.s_trace.motion_watchdog.MotionWatchdogHandler;

import org.coolreader.CoolReader;
import org.coolreader.R;
import org.coolreader.tts.OnTTSStatusListener;
import org.coolreader.tts.TTSControlBinder;
import org.coolreader.tts.TTSControlService;
import org.coolreader.tts.TTSControlServiceAccessor;

import java.io.File;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TTSToolbarDlg implements Settings {
	public static final Logger log = L.create("ttsdlg");

	public static final int MEDIA_COVER_WIDTH = 300;
	public static final int MEDIA_COVER_HEIGHT = 400;

	private final PopupWindow mWindow;
	private final CoolReader mCoolReader;
	private final Engine mEngine;
	private final TtsDocumentSnapshot documentSnapshot;
	private final TtsDocumentHandler documentHandler;
	private final LinearLayout glassPanel;
	private final LinearLayout toolbarBody;
	private final TTSControlServiceAccessor mTTSControl;
	private final ImageButton mPlayPauseButton;
	private final ImageButton backButton;
	private final ImageButton forwardButton;
	private final ImageButton stopButton;
	private final ImageButton optionsButton;
	private final TextView mAudioProgressTextView;
	private final TextView mVolumeTextView;
	private final TextView mSpeedTextView;
	private final SeekBar mSbSpeed;
	private final SeekBar mSbVolume;
	private final ImageButton btnDecVolume;
	private final ImageButton btnIncVolume;
	private final ImageButton btnDecSpeed;
	private final ImageButton btnIncSpeed;
	private final CloseableTaskGate workLifecycle =
			new CloseableTaskGate();
	private final Handler audioBookPosHandler =
			new Handler(Looper.getMainLooper());
	private final MotionWatchdogSlotState motionWatchdogSlot =
			new MotionWatchdogSlotState();
	private final TtsToolbarSessionState sessionState =
			new TtsToolbarSessionState();
	private final WordTimingCalcHandlerState wordTimingHandlerState =
			new WordTimingCalcHandlerState();
	private ReaderViewModeState.Lease viewModeLease;
	private Selection mCurrentSelection;
	private boolean isSpeaking;
	private boolean isToolbarHidden;
	private int mMotionTimeout;
	private boolean mAutoSetDocLang;
	private String mBookAuthors;
	private String mBookTitle;
	private Bitmap mBookCover;
	private String mBookLanguage;
	private String mForcedLanguage;
	private String mForcedVoice;
	private String mCurrentLanguage;
	private String mCurrentVoiceName;
	private boolean mGoogleTTSAbbreviationWorkaround;
	private boolean allowUseAudiobook;
	private int mTTSSpeedPercent = 50;		// 50% (normal)

	private final TtsAudiobookFilesState audiobookFiles =
			new TtsAudiobookFilesState();
	private WordTimingAudiobookMatcher wordTimingAudiobookMatcher;
	private SentenceInfo currentSentenceInfo;

	static TTSToolbarDlg showDialog(
			CoolReader coolReader,
			View anchor,
			Engine engine,
			TtsDocumentSnapshot documentSnapshot,
			TtsDocumentHandler documentHandler,
			TTSControlServiceAccessor ttsacc) {
		TTSToolbarDlg dlg = new TTSToolbarDlg(
				coolReader,
				anchor,
				engine,
				documentSnapshot,
				documentHandler,
				ttsacc);
		log.d("popup: " + dlg.mWindow.getWidth() + "x" + dlg.mWindow.getHeight());
		return dlg;
	}

	public void setOnCloseListener(Runnable handler) {
		sessionState.setOnCloseListener(handler);
	}

	public void stopAndClose() {
		if (!workLifecycle.close())
			return;
		isSpeaking = false;
		stopMotionWatchdog();
		stopAudiobookWork();
		boolean stopRequested =
				mTTSControl.bind(ttsbinder ->
						ttsbinder.stop(
								result -> finishClose()));
		if (!stopRequested)
			finishClose();
	}

	private void finishClose() {
		BackgroundThread.instance().postGUI(() -> {
			if (!sessionState.beginFinishClose())
				return;
			mTTSControl.unbind();
			Intent intent = new Intent(
					mCoolReader, TTSControlService.class);
			mCoolReader.stopService(intent);
			cleanupDocument();
			Runnable onClose = sessionState.takeOnCloseListener();
			if (onClose != null)
				onClose.run();
			sessionState.close();
			motionWatchdogSlot.close();
			wordTimingHandlerState.close();
			audiobookFiles.close();
			if (mWindow.isShowing())
				mWindow.dismiss();
		});
	}

	void stopAndCloseForDocumentChange() {
		BackgroundThread.ensureGUI();
		cleanupDocument();
		stopAndClose();
	}

	private void cleanupDocument() {
		BackgroundThread.ensureGUI();
		if (!sessionState.beginDocumentCleanup())
			return;
		restoreReaderMode();
		documentHandler.clearSelection();
		documentHandler.savePosition();
	}

	private void stopAudiobookWork() {
		audioBookPosHandler.removeCallbacksAndMessages(null);
		WordTimingCalcHandlerState.Running running =
				wordTimingHandlerState.takeRunning();
		if (running != null) {
			if (running.handler != null)
				running.handler.removeCallbacksAndMessages(null);
			if (running.thread != null)
				running.thread.quit();
		}
	}

	private void postGuiIfOpen(Runnable task) {
		BackgroundThread.instance().postGUI(() -> {
			if (isDocumentOpen())
				task.run();
		});
	}

	private boolean isDocumentOpen() {
		return !workLifecycle.isClosed()
				&& documentHandler.isActive();
	}

	private boolean isDocumentWorkActive(
			CloseableTaskGate.Token token) {
		return workLifecycle.isActive(token)
				&& documentHandler.isActive();
	}

	public void pause() {
		mTTSControl.bind(ttsbinder -> {
			ttsbinder.pause(null);
		});
	}

	public void hideSystemNavBar(View view){
		try{
			view.setSystemUiVisibility(
						0
						| View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
						| View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
						| View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
						| View.SYSTEM_UI_FLAG_FULLSCREEN
						| View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
			);
		}catch(Exception e){
			log.e("Failed to hide system navigation bar", e);
		}
	}
	private void setReaderMode() {
		viewModeLease =
				documentHandler.enterReaderMode();
		moveSelection(ReaderCommand.DCMD_SELECT_FIRST_SENTENCE, null);
	}

	private void restoreReaderMode() {
		ReaderViewModeState.Lease lease = viewModeLease;
		viewModeLease = null;
		documentHandler.restoreReaderMode(lease);
	}

	private SentenceInfo fetchSelectedSentenceInfo() {
		if(wordTimingAudiobookMatcher != null && mCurrentSelection != null){
			SentenceInfo cur = wordTimingAudiobookMatcher.getSentence(mCurrentSelection.startPos);
			if(cur == null){
				// use the previous sentenceInfo if current selection is not in sentenceinfo cache
				log.i("WARNING: reusing previous sentenceinfo for missing selection\n");
				cur = currentSentenceInfo;
			}
			currentSentenceInfo = cur;
		}else{
			currentSentenceInfo = null;
		}
		return currentSentenceInfo;
	}

	private String formatDurationHHHMMSS(double duration) {
		return String.format(Locale.getDefault(), "%3d:%02d:%02d",
			((int) duration) / 60 / 60,
			((int) duration) / 60 % 60,
			((int) duration) % 60);
	}

	private void setAudioBookProgressDisplay(SentenceInfo sentenceInfo) {
		if(sentenceInfo == null){
			mAudioProgressTextView.setVisibility(View.GONE);
			mAudioProgressTextView.setText("");

			mSbSpeed.setVisibility(View.VISIBLE);
		}else{
			mAudioProgressTextView.setVisibility(View.VISIBLE);
			mAudioProgressTextView.setText(String.format(Locale.getDefault(),
				"%s / %s",
				formatDurationHHHMMSS(sentenceInfo.sentenceTiming.startTimeInBook),
				formatDurationHHHMMSS(sentenceInfo.sentenceTiming.totalBookDuration)));

			mSbSpeed.setVisibility(View.GONE);
		}
	}

	/**
	 * Select next or previous sentence. ONLY the selection changes and the specified callback is called!
	 * Not affected to speech synthesis process.
	 * @param cmd move command. DCMD_SELECT_NEXT_SENTENCE, DCMD_SELECT_PREV_SENTENCE, DCMD_SELECT_FIRST_SENTENCE.
	 * @param callback optional completion callback
	 */
	private void moveSelection(
			ReaderCommand cmd,
			TtsDocumentHandler.SelectionHandler callback)
	{
		if (!isDocumentOpen())
			return;
		documentHandler.moveSelection(
				cmd,
				new TtsDocumentHandler.SelectionHandler() {

			@Override
			public void onNewSelection(Selection selection) {
				if (!isDocumentOpen())
					return;
				log.d("onNewSelection: " + selection.text + " : " + selection.startY + " x " + selection.startX);
				mCurrentSelection = selection;
				if(allowUseAudiobook){
					SentenceInfo sentenceInfo = fetchSelectedSentenceInfo();
					setAudioBookProgressDisplay(sentenceInfo);
					if(sentenceInfo != null && sentenceInfo.sentenceTiming.audioFile != null){
						mTTSControl.bind(ttsbinder -> {
							ttsbinder.setAudioFile(sentenceInfo.sentenceTiming.audioFile, sentenceInfo.sentenceTiming.startTime);
						});
					}
				}else{
					setAudioBookProgressDisplay(null);
				}
				if (null != callback)
					callback.onNewSelection(mCurrentSelection);
			}

			@Override
			public void onFail() {
				if (!isDocumentOpen())
					return;
				log.e("fail()");
				if (isSpeaking) {
					mTTSControl.bind(ttsbinder ->
							ttsbinder.stop(result ->
									log.e("speech synthesis process stopped!")));
				}
				if (null != callback)
					callback.onFail();
			}
		});
	}

	private String preprocessUtterance(String utterance) {
		String newUtterance = utterance;
		if (mGoogleTTSAbbreviationWorkaround) {
			// Add space before last char if it's dot.
			int len = newUtterance.length();
			if (len > 1) {
				if (newUtterance.charAt(len - 1) == '.') {
					newUtterance = newUtterance.substring(0, len - 1);
					newUtterance += " .";
				}
			}
		}
		return newUtterance;
	}

	@TargetApi(Build.VERSION_CODES.ECLAIR)
	private synchronized void startMotionWatchdog() {
		String TAG = "MotionWatchdog";
		log.d("startMotionWatchdog() enter");

		stopMotionWatchdog();
		if (mMotionTimeout <= 0 || !isDocumentOpen()) {
			Log.d(TAG, "startMotionWatchdog() early exit - timeout is 0");
			return;
		}

		HandlerThread thread =
				new HandlerThread("MotionWatchdog");
		thread.start();
		MotionWatchdogHandler previous = motionWatchdogSlot.install(
				new MotionWatchdogHandler(
						this, mCoolReader, thread, mMotionTimeout));
		if (previous != null)
			previous.close();
		Log.d(TAG, "startMotionWatchdog() exit");
	}

	private synchronized void stopMotionWatchdog() {
		MotionWatchdogHandler watchdog = motionWatchdogSlot.take();
		if (watchdog != null)
			watchdog.close();
	}

	/**
	 * Convert speech speed percentage to speech rate value.
	 * @param percent speech rate percentage
	 * @return speech rate value
	 *
	 * 0%  - 0.30
	 * 10% - 0.44
	 * 20% - 0.58
	 * 30% - 0.72
	 * 40% - 0.86
	 * 50% - 1.00
	 * 60% - 1.50
	 * 70% - 2.00
	 * 80% - 2.50
	 * 90% - 3.00
	 * 100%- 3.50
	 */
	private float speechRateFromPercent(int percent) {
		float rate;
		if ( percent < 50 )
			rate = 0.3f + 0.7f * percent / 50f;
		else
			rate = 1.0f + 2.5f * (percent - 50) / 50f;
		return rate;
	}

	public void setAppSettings(Properties newSettings, Properties oldSettings) {
		log.v("setAppSettings()");
		BackgroundThread.ensureGUI();
		boolean initialSetup;
		if (oldSettings == null){
			oldSettings = new Properties();
			initialSetup = true;
		}else{
			initialSetup = false;
		}
		int oldTTSSpeed = mTTSSpeedPercent;
		boolean oldAllowUseAudiobook = this.allowUseAudiobook;
		Properties changedSettings = newSettings.diff(oldSettings);
		for (Map.Entry<Object, Object> entry : changedSettings.entrySet()) {
			String key = (String) entry.getKey();
			String value = (String) entry.getValue();
			processAppSetting(key, value);
		}
		// Apply settings
		setupTTSVoice();
		if (oldTTSSpeed != mTTSSpeedPercent) {
			mTTSControl.bind(ttsbinder -> {
				ttsbinder.setSpeechRate(speechRateFromPercent(mTTSSpeedPercent), result -> {
					if (result)
						BackgroundThread.instance().postGUI(() -> mSbSpeed.setProgress(mTTSSpeedPercent));
				});
			});
		}
		boolean newAllowUseAudiobook = allowUseAudiobook;
		if (!initialSetup && oldAllowUseAudiobook && !newAllowUseAudiobook){
			mTTSControl.bind(ttsbinder -> {
				ttsbinder.stop(null);
				ttsbinder.setAudioFile(null, 0);
				initAudiobookWordTimings(new InitAudiobookWordTimingsCallback(){
					public void onComplete(){
						moveSelection(
								ReaderCommand
										.DCMD_SELECT_FIRST_SENTENCE,
								new TtsDocumentHandler
										.SelectionHandler() {
							@Override
							public void onNewSelection(Selection selection) {
								if (isSpeaking) {
									ttsbinder.say(preprocessUtterance(selection.text), null);
								} else {
									ttsbinder.setCurrentUtterance(preprocessUtterance(selection.text));
								}
							}

							@Override
							public void onFail() {
							}
						});
					}
				});
			});
		}else if(!initialSetup && !oldAllowUseAudiobook && newAllowUseAudiobook){
			mTTSControl.bind(ttsbinder -> {
				ttsbinder.stop(null);
				SentenceInfo sentenceInfo = fetchSelectedSentenceInfo();
				if(sentenceInfo != null){
					ttsbinder.setAudioFile(sentenceInfo.sentenceTiming.audioFile, sentenceInfo.sentenceTiming.startTime);
				}
				initAudiobookWordTimings(null);
				moveSelection(ReaderCommand.DCMD_SELECT_FIRST_SENTENCE, null);
			});
		}
	}

	private void processAppSetting(String key, String value) {
		boolean flg = "1".equals(value);
		switch (key) {
			case PROP_APP_MOTION_TIMEOUT:
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ECLAIR) {
					mMotionTimeout = Utils.parseInt(value, 0, 0, 100);
					mMotionTimeout = mMotionTimeout * 60 * 1000; // Convert minutes to msecs
				}
				break;
			case PROP_APP_TTS_SPEED:
				mTTSSpeedPercent = Utils.parseInt(value, 50, 0, 100);
				break;
			case PROP_APP_TTS_ENGINE:
				// handled in CoolReader
				break;
			case PROP_APP_TTS_USE_DOC_LANG:
				mAutoSetDocLang = flg;
				break;
			case PROP_APP_TTS_FORCE_LANGUAGE:
				mForcedLanguage = value;
				break;
			case PROP_APP_TTS_VOICE:
				mForcedVoice = value;
				break;
			case PROP_APP_TTS_GOOGLE_END_OF_SENTENCE_ABBR:
				mGoogleTTSAbbreviationWorkaround = flg;
				break;
			case PROP_APP_TTS_USE_AUDIOBOOK:
				allowUseAudiobook = flg;
				break;
		}
	}

	private void setupTTSVoice() {
		if (mAutoSetDocLang) {
			// set language for TTS based on book's language
			if (null != mBookLanguage && mBookLanguage.length() > 0 && !mBookLanguage.equals(mCurrentLanguage)) {
				log.d("trying to set TTS language to \"" + mBookLanguage + "\"");
				mTTSControl.bind(ttsbinder -> {
					ttsbinder.setLanguage(mBookLanguage, result -> {
						mCurrentLanguage = mBookLanguage;
						if (result)
							log.d("setting TTS language to \"" + mBookLanguage + "\" successful.");
						else
							log.d("Failed to set TTS language to \"" + mBookLanguage + "\".");
					});
				});
			} else {
				log.e("Failed to detect book's language, will be used system default!");
			}
		} else {
			if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
				if (null != mForcedVoice && mForcedVoice.length() > 0 && !mForcedVoice.equals(mCurrentVoiceName)) {
					mTTSControl.bind(ttsbinder -> {
						ttsbinder.setVoice(mForcedVoice, result -> {
							mCurrentVoiceName = mForcedVoice;
							if (result) {
								log.d("Set voice \"" + mForcedVoice + "\" successful");
							} else {
								log.e("Failed to set voice \"" + mForcedVoice + "\"!");
							}
						});
					});
				}
			}
		}
	}

	private void setupSpeechStatusHandler() {
		mTTSControl.bind(ttsbinder ->
				ttsbinder.setStatusListener(
						new OnTTSStatusListener() {
			@Override
			public void onUtteranceStart() {
				if (isDocumentOpen())
					isSpeaking = true;
			}

			@Override
			public void onUtteranceDone() {
			}

			@Override
			public void onError(int errorCode) {
				postGuiIfOpen(() ->
						mCoolReader.showToast(R.string.tts_failed));
			}

			@Override
			public void onStateChanged(
					TTSControlService.State state) {
				if (!isDocumentOpen())
					return;
				switch (state) {
					case PLAYING:
						isSpeaking = true;
						postGuiIfOpen(() ->
								mPlayPauseButton.setImageResource(
										Utils.resolveResourceIdByAttr(
												mCoolReader,
												R.attr.ic_media_pause_drawable,
												R.drawable.ic_media_pause)));
						if (Build.VERSION.SDK_INT
								>= Build.VERSION_CODES.ECLAIR
								&& mMotionTimeout > 0) {
							startMotionWatchdog();
						}
						break;
					case PAUSED:
					case STOPPED:
						isSpeaking = false;
						postGuiIfOpen(() ->
								mPlayPauseButton.setImageResource(
										Utils.resolveResourceIdByAttr(
												mCoolReader,
												R.attr.ic_media_play_drawable,
												R.drawable.ic_media_play)));
						stopMotionWatchdog();
						break;
				}
			}

			@Override
			public void onVolumeChanged(
					int currentVolume, int maxVolume) {
				postGuiIfOpen(() -> {
					mSbVolume.setMax(maxVolume);
					mSbVolume.setProgress(currentVolume);
				});
			}

			@Override
			public void onAudioFocusLost() {
			}

			@Override
			public void onAudioFocusRestored() {
			}

			@Override
			public void onCurrentSentenceRequested(
					TTSControlBinder ttsbinder) {
				if (!isDocumentOpen())
					return;
				if (mCurrentSelection != null) {
					ttsbinder.say(
							preprocessUtterance(
									mCurrentSelection.text),
							null);
				}
			}

			@Override
			public void onNextSentenceRequested(
					TTSControlBinder ttsbinder) {
				if (!isDocumentOpen())
					return;
				moveSelection(
						ReaderCommand.DCMD_SELECT_NEXT_SENTENCE,
						createSpeechSelectionCallback(ttsbinder));
			}

			@Override
			public void onPreviousSentenceRequested(
					TTSControlBinder ttsbinder) {
				if (!isDocumentOpen())
					return;
				moveSelection(
						ReaderCommand.DCMD_SELECT_PREV_SENTENCE,
						createSpeechSelectionCallback(ttsbinder));
			}

			@Override
			public void onStopRequested(
					TTSControlBinder ttsbinder) {
				stopAndClose();
			}
		}));
	}

	private TtsDocumentHandler.SelectionHandler
			createSpeechSelectionCallback(
					TTSControlBinder ttsbinder) {
		return new TtsDocumentHandler.SelectionHandler() {
			@Override
			public void onNewSelection(Selection selection) {
				if (!isDocumentOpen())
					return;
				String utterance =
						preprocessUtterance(selection.text);
				if (isSpeaking)
					ttsbinder.say(utterance, null);
				else
					ttsbinder.setCurrentUtterance(utterance);
			}

			@Override
			public void onFail() {
			}
		};
	}

	@SuppressLint("ClickableViewAccessibility")
	TTSToolbarDlg(
			CoolReader coolReader,
			View anchor,
			Engine engine,
			TtsDocumentSnapshot documentSnapshot,
			TtsDocumentHandler documentHandler,
			TTSControlServiceAccessor ttsacc) {
		if (anchor == null
				|| engine == null
				|| documentSnapshot == null
				|| documentHandler == null
				|| !documentHandler.isActive())
			throw new IllegalArgumentException(
					"active TTS document handler is required");
		mCoolReader = coolReader;
		mEngine = engine;
		this.documentSnapshot = documentSnapshot;
		this.documentHandler = documentHandler;
		mTTSControl = ttsacc;
		mBookAuthors = documentSnapshot.getAuthors();
		mBookTitle = documentSnapshot.getTitle();
		mBookLanguage = documentSnapshot.getLanguage();

		//Context context = mCoolReader.getApplicationContext();
		Context context = anchor.getContext();
		LayoutInflater inflater = LayoutInflater.from(context);
		View panel = inflater.inflate(R.layout.tts_toolbar, null);

		glassPanel = panel.findViewById(R.id.tts_glass_panel);
		toolbarBody = panel.findViewById(R.id.tts_toolbar_body);

                glassPanel.setOnClickListener(v -> {
                    isToolbarHidden = !isToolbarHidden;
                    toolbarBody.setVisibility(isToolbarHidden ? View.INVISIBLE: View.VISIBLE);
                });

		mPlayPauseButton = panel.findViewById(R.id.tts_play_pause);
		mPlayPauseButton.setImageResource(Utils.resolveResourceIdByAttr(mCoolReader, R.attr.ic_media_play_drawable, R.drawable.ic_media_play));
		backButton = panel.findViewById(R.id.tts_back);
		forwardButton = panel.findViewById(R.id.tts_forward);
		stopButton = panel.findViewById(R.id.tts_stop);
		optionsButton = panel.findViewById(R.id.tts_options);

		mWindow = new PopupWindow( context );
		mWindow.setBackgroundDrawable(new BitmapDrawable());
		mPlayPauseButton.setOnClickListener(
				v -> mCoolReader.sendBroadcast(new Intent(TTSControlService.TTS_CONTROL_ACTION_PLAY_PAUSE)
						.setPackage(mCoolReader.getPackageName())));
		backButton.setOnClickListener(
				v -> mCoolReader.sendBroadcast(new Intent(TTSControlService.TTS_CONTROL_ACTION_PREV)
						.setPackage(mCoolReader.getPackageName())));
		forwardButton.setOnClickListener(
				v -> mCoolReader.sendBroadcast(new Intent(TTSControlService.TTS_CONTROL_ACTION_NEXT)
						.setPackage(mCoolReader.getPackageName())));
		optionsButton.setOnClickListener(v -> mTTSControl.bind(ttsbinder -> {
			OptionsDialog dlg = new OptionsDialog(
					mCoolReader,
					mEngine,
					OptionsDialog.Mode.TTS,
					null,
					ttsbinder);
			dlg.show();
		}));
		stopButton.setOnClickListener(v -> stopAndClose());


		// setup audiobook speed && volume seek bars
		mAudioProgressTextView = panel.findViewById(R.id.tts_lbl_audio_progress);
		mAudioProgressTextView.setVisibility(View.GONE);

		// setup speed && volume seek bars
		mVolumeTextView = panel.findViewById(R.id.tts_lbl_volume);
		mSpeedTextView = panel.findViewById(R.id.tts_lbl_speed);
		mSpeedTextView.setText(String.format(Locale.getDefault(), "%s (x%.2f)", context.getString(R.string.tts_rate), speechRateFromPercent(50)));

		mSbSpeed = panel.findViewById(R.id.tts_sb_speed);
		mSbVolume = panel.findViewById(R.id.tts_sb_volume);

		mSbSpeed.setMax(100);
		mSbSpeed.setProgress(50);
		mSbVolume.setMax(100);
		mSbVolume.setProgress(0);
		mSbSpeed.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
			int mProgress;
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				mProgress = progress;
				float rate = speechRateFromPercent(progress);
				mSpeedTextView.setText(String.format(Locale.getDefault(), "%s (x%.2f)", context.getString(R.string.tts_rate), rate));
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				mCoolReader.setSetting(PROP_APP_TTS_SPEED, String.valueOf(mProgress), true);
			}
		});
		btnDecVolume = panel.findViewById(R.id.btn_dec_volume);
		btnDecVolume.setOnTouchListener(new RepeatOnTouchListener(500, 150,
				view -> mSbVolume.setProgress(mSbVolume.getProgress() - 1)));
		btnIncVolume = panel.findViewById(R.id.btn_inc_volume);
		btnIncVolume.setOnTouchListener(new RepeatOnTouchListener(500, 150, view -> mSbVolume.setProgress(mSbVolume.getProgress() + 1)));

		btnDecSpeed = panel.findViewById(R.id.btn_dec_speed);
		btnDecSpeed.setOnTouchListener(new RepeatOnTouchListener(500, 150, view -> {
			mSbSpeed.setProgress(mSbSpeed.getProgress() - 1);
			mCoolReader.setSetting(PROP_APP_TTS_SPEED, String.valueOf(mSbSpeed.getProgress()), true);
		}));
		btnIncSpeed = panel.findViewById(R.id.btn_inc_speed);
		btnIncSpeed.setOnTouchListener(new RepeatOnTouchListener(500, 150, view -> {
			mSbSpeed.setProgress(mSbSpeed.getProgress() + 1);
			mCoolReader.setSetting(PROP_APP_TTS_SPEED, String.valueOf(mSbSpeed.getProgress()), true);
		}));

		panel.measure(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		panel.setFocusable(true);
		panel.setEnabled(true);
		panel.setOnKeyListener((v, keyCode, event) -> {
			if ( event.getAction()==KeyEvent.ACTION_UP ) {
				switch ( keyCode ) {
				case KeyEvent.KEYCODE_VOLUME_DOWN:
				case KeyEvent.KEYCODE_VOLUME_UP:
					return true;
				case KeyEvent.KEYCODE_BACK:
					stopAndClose();
					return true;
				}
			} else if ( event.getAction()==KeyEvent.ACTION_DOWN ) {
				switch ( keyCode ) {
				case KeyEvent.KEYCODE_VOLUME_DOWN: {
					int p = mSbVolume.getProgress() - 1;
					if ( p<0 )
						p = 0;
					mSbVolume.setProgress(p);
					return true;
				}
				case KeyEvent.KEYCODE_VOLUME_UP:
					int p = mSbVolume.getProgress() + 1;
					if ( p > mSbVolume.getMax() )
						p = mSbVolume.getMax();
					mSbVolume.setProgress(p);
					return true;
				}
				if ( keyCode == KeyEvent.KEYCODE_BACK) {
					return true;
				}
			}
			return false;
		});

		mWindow.setOnDismissListener(() -> {
			if (!workLifecycle.isClosed())
				stopAndClose();
		});

		mWindow.setBackgroundDrawable(new BitmapDrawable());
		mWindow.setWidth(WindowManager.LayoutParams.FILL_PARENT);
		mWindow.setHeight(WindowManager.LayoutParams.FILL_PARENT);
		mWindow.setFocusable(true);
		mWindow.setTouchable(true);
		mWindow.setOutsideTouchable(true);
		mWindow.setContentView(panel);

		hideSystemNavBar(panel);

		int [] location = new int[2];
		anchor.getLocationOnScreen(location);

		mWindow.showAtLocation(anchor, Gravity.TOP | Gravity.CENTER_HORIZONTAL, location[0], location[1] + anchor.getHeight() - panel.getHeight());

		setReaderMode();

		if (null == mBookTitle)
			mBookTitle = "";
		if (null == mBookAuthors)
			mBookAuthors = "";
		if (null == mBookLanguage) {
			log.e("Failed to detect book's language!");
		}

		// Start the foreground service to make this app also foreground,
		// even if the main activity is in the background.
		// https://developer.android.com/about/versions/oreo/background#services
		Intent intent = new Intent(TTSControlService.TTS_CONTROL_ACTION_PREPARE, Uri.EMPTY, coolReader, TTSControlService.class);
		Bundle data = new Bundle();
		data.putString("bookAuthors", mBookAuthors);
		data.putString("bookTitle", mBookTitle);
		intent.putExtras(data);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
			coolReader.startForegroundService(intent);
		else
			coolReader.startService(intent);

		panel.requestFocus();

		// All tasks below after service start.
		audiobookFiles.clear();
		String pathName = documentSnapshot.getPath();
		if (pathName != null) {
			mBookCover = Bitmap.createBitmap(
					MEDIA_COVER_WIDTH,
					MEDIA_COVER_HEIGHT,
					Bitmap.Config.RGB_565);
			documentHandler.drawCover(
					mBookCover,
					bitmap -> mTTSControl.bind(
							ttsbinder -> ttsbinder.setMediaItemInfo(
									mBookAuthors,
									mBookTitle,
									bitmap)));
			String wordTimingPath =
					pathName.replaceAll(
							"\\.\\w+$", ".wordtiming");
			String sentenceInfoPath =
					pathName.replaceAll(
							"\\.\\w+$", ".sentenceinfo");
			String sentenceTimingCachePath =
					pathName.replaceAll(
							"\\.\\w+$",
							".sentencetimingcache");
			if (wordTimingPath.matches(
					".*\\.wordtiming$")) {
				audiobookFiles.set(
						new File(wordTimingPath),
						new File(sentenceInfoPath),
						new File(sentenceTimingCachePath));
			}
		}
		// Show volume
		mTTSControl.bind(ttsbinder -> ttsbinder.retrieveVolume((current, max) -> {
			mSbVolume.setMax(max);
			mSbVolume.setProgress(current);
		}));
		mSbVolume.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
										  boolean fromUser) {
				if (mSbVolume.getMax() < 1)
					return;
				mTTSControl.bind(ttsbinder -> ttsbinder.setVolume(progress));
				mVolumeTextView.setText(String.format(Locale.getDefault(), "%s (%d%%)", context.getString(R.string.tts_volume), 100*progress/seekBar.getMax()));
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
			}
		});
		// And finally, setup status change handler
		setupSpeechStatusHandler();
	}

	public void initAudiobookWordTimings(
			InitAudiobookWordTimingsCallback callback) {
		if (!documentHandler.isActive())
			return;
		CloseableTaskGate.Token token = workLifecycle.replace();
		if (token == null)
			return;
		audioBookPosHandler.removeCallbacksAndMessages(null);
		Handler existingWordTiming = wordTimingHandlerState.get();
		if (existingWordTiming != null)
			existingWordTiming.removeCallbacksAndMessages(null);

		TtsAudiobookFilesState.Snapshot audiobookSnapshot =
				audiobookFiles.snapshot();
		final File timingFile = audiobookSnapshot.wordTimingFile;
		final File infoFile = audiobookSnapshot.sentenceInfoFile;
		final File timingCacheFile =
				audiobookSnapshot.sentenceTimingCacheFile;
		if (!allowUseAudiobook
				|| timingFile == null
				|| !timingFile.exists()) {
			wordTimingAudiobookMatcher = null;
			return;
		}

		Handler wordTimingCalcHandler = wordTimingHandlerState.get();
		if (wordTimingCalcHandler == null) {
			HandlerThread wordTimingCalcHandlerThread =
					new HandlerThread("word-timing-calc-handler");
			wordTimingCalcHandlerThread.start();
			Handler createdHandler =
					new Handler(wordTimingCalcHandlerThread.getLooper());
			wordTimingCalcHandler = wordTimingHandlerState.ensure(
					wordTimingCalcHandlerThread, createdHandler);
			if (wordTimingCalcHandler == null) {
				// permanently closed while installing
				wordTimingCalcHandlerThread.quit();
				return;
			}
			if (wordTimingCalcHandler != createdHandler) {
				// lost install race: quit orphan thread
				wordTimingCalcHandlerThread.quit();
			}
		}

		mPlayPauseButton.setVisibility(View.GONE);
		backButton.setVisibility(View.GONE);
		forwardButton.setVisibility(View.GONE);
		stopButton.setVisibility(View.GONE);
		optionsButton.setVisibility(View.GONE);

		mCoolReader.showToast("matching audiobook word timings");
		wordTimingCalcHandler.post(() -> {
			if (!isDocumentWorkActive(token))
				return;
			List<SentenceInfo> allSentences =
					SentenceInfoCache.maybeReadCache(infoFile);
			if (!isDocumentWorkActive(token))
				return;
			if (allSentences == null) {
				allSentences =
						documentHandler.getAllSentences();
				if (!isDocumentWorkActive(token))
					return;
				if (allSentences == null)
					return;
				SentenceInfoCache.maybeWriteCache(
						infoFile, allSentences);
			}
			if (!isDocumentWorkActive(token))
				return;

			WordTimingAudiobookMatcher matcher =
					new WordTimingAudiobookMatcher(
							timingFile, allSentences);
			matcher.maybeReadSentenceTimingCache(timingCacheFile);
			if (!isDocumentWorkActive(token))
				return;
			if (!matcher.isSentenceTimingReady()) {
				// This can be very long. Its result must not escape
				// the task generation that requested it.
				matcher.parseWordTimingsFile();
				if (!isDocumentWorkActive(token))
					return;
				matcher.maybeWriteSentenceTimingCache(
						timingCacheFile);
			}
			if (!isDocumentWorkActive(token))
				return;

			BackgroundThread.instance().postGUI(() ->
					finishAudiobookInitialization(
							token, matcher, callback));
		});
	}

	private void finishAudiobookInitialization(
			CloseableTaskGate.Token token,
			WordTimingAudiobookMatcher matcher,
			InitAudiobookWordTimingsCallback callback) {
		if (!isDocumentWorkActive(token))
			return;
		wordTimingAudiobookMatcher = matcher;
		moveSelection(
				ReaderCommand.DCMD_SELECT_FIRST_SENTENCE, null);
		scheduleAudiobookPositionPoll(token);
		mPlayPauseButton.setVisibility(View.VISIBLE);
		backButton.setVisibility(View.VISIBLE);
		forwardButton.setVisibility(View.VISIBLE);
		stopButton.setVisibility(View.VISIBLE);
		optionsButton.setVisibility(View.VISIBLE);
		if (callback != null)
			callback.onComplete();
	}

	private void scheduleAudiobookPositionPoll(
			CloseableTaskGate.Token token) {
		if (isDocumentWorkActive(token)) {
			audioBookPosHandler.postDelayed(
					() -> pollAudiobookPosition(token), 500);
		}
	}

	private void pollAudiobookPosition(
			CloseableTaskGate.Token token) {
		if (!isDocumentWorkActive(token))
			return;
		try {
			SentenceInfo currentSentence =
					fetchSelectedSentenceInfo();
			if (currentSentence != null) {
				mTTSControl.bind(ttsbinder -> {
					if (!isDocumentWorkActive(token))
						return;
					ttsbinder.isAudioBookPlaybackAfterSentence(
							currentSentence,
							isAfter ->
									BackgroundThread.instance()
											.postGUI(() -> {
												if (isAfter
														&& isDocumentWorkActive(
																token)) {
													moveSelection(
															ReaderCommand
																	.DCMD_SELECT_NEXT_SENTENCE,
															null);
												}
											}));
				});
			}
		} finally {
			scheduleAudiobookPositionPoll(token);
		}
	}
}
