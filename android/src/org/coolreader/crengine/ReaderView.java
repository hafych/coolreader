/*
 * CoolReader for Android
 * Copyright (C) 2010-2015,2020 Vadim Lopatin <coolreader.org@gmail.com>
 * Copyright (C) 2011 a_lone
 * Copyright (C) 2011 alexstsv
 * Copyright (C) 2012 Michael Berganovsky <mike0berg@gmail.com>
 * Copyright (C) 2012 Jasper Poppe <jpoppe@ebay.com>
 * Copyright (C) 2012,2013 Jeff Doozan <jeff@doozan.com>
 * Copyright (C) 2012 Daniel Savard <daniels@xsoli.com>
 * Copyright (C) 2012,2014 klush
 * Copyright (C) 2018 norbi24 <norbert.bartalsky@gmail.com>
 * Copyright (C) 2018 Yuri Plotnikov <plotnikovya@gmail.com>
 * Copyright (C) 2018 S-trace <S-trace@list.ru>
 * Copyright (C) 2018-2021 Aleksey Chernov <valexlin@gmail.com>
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

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.text.ClipboardManager;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnKeyListener;
import android.view.View.OnTouchListener;

import org.coolreader.CoolReader;
import org.coolreader.R;
import org.coolreader.crengine.InputDialog.InputHandler;
import org.coolreader.db.CRDBService;
import org.coolreader.genrescollection.GenresCollection;
import org.coolreader.tts.TTSControlServiceAccessor;
import org.koekak.android.ebookdownloader.SonyBookSelector;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ReaderView implements android.view.SurfaceHolder.Callback, Settings, DocProperties, OnKeyListener, OnTouchListener, OnFocusChangeListener {

	public static final Logger log = L.create("rv", Log.VERBOSE);
	public static final Logger alog = L.create("ra", Log.WARN);

	private static final int EINK_FOCUS_REFRESH_DELAY_MS = 400;
	private final ReaderSurfaceState readerSurfaceState =
			new ReaderSurfaceState();
	private final ReaderNativeLifecycle readerNativeLifecycle =
			new ReaderNativeLifecycle();
	private final ReaderViewModeState readerViewModeState =
			new ReaderViewModeState();
	private final DelayedExecutor einkRefreshScheduler =
			DelayedExecutor.createGUI("eink-focus-refresh");
	private final SurfaceView surface;
	private final BookView bookView;

	public SurfaceView getSurface() {
		return surface;
	}

	public interface BookView {
		void draw();

		void draw(boolean isPartially);

		void invalidate();

		void onPause();

		void onResume();
	}

	public class ReaderSurface extends SurfaceView implements BookView {

		public ReaderSurface(Context context) {
			super(context);
			// TODO Auto-generated constructor stub
		}

		@Override
		public void onPause() {

		}

		@Override
		public void onResume() {

		}

		@Override
		protected void onDraw(Canvas canvas) {
			if (readerSurfaceState.isClosed())
				return;
			try {
				log.d("onDraw() called");
				draw();
			} catch (Exception e) {
				log.e("exception while drawing", e);
			}
		}

		@Override
		protected void onDetachedFromWindow() {
			super.onDetachedFromWindow();
			log.d("View.onDetachedFromWindow() is called");
		}

		@Override
		public boolean onTrackballEvent(MotionEvent event) {
			if (readerSurfaceState.isClosed())
				return super.onTrackballEvent(event);
			log.d("onTrackballEvent(" + event + ")");
			if (readerSettingsState.getBool(
					PROP_APP_TRACKBALL_DISABLED, false)) {
				log.d("trackball is disabled in settings");
				return true;
			}
			mActivity.onUserActivity();
			return super.onTrackballEvent(event);
		}

		@Override
		protected void onSizeChanged(final int w, final int h, int oldw, int oldh) {
			super.onSizeChanged(w, h, oldw, oldh);
			if (readerSurfaceState.isClosed())
				return;
			log.i("onSizeChanged(" + w + ", " + h + ")"
					+ " activity.isDialogActive="
					+ getActivity().isDialogActive());
			requestResize(w, h);
		}

		@Override
		public void onWindowVisibilityChanged(int visibility) {
			if (readerSurfaceState.isClosed()) {
				super.onWindowVisibilityChanged(visibility);
				return;
			}
			boolean visible = visibility == VISIBLE;
			boolean refresh =
					readerSurfaceState.changeVisibility(visible);
			if (!visible || refresh)
				einkRefreshScheduler.cancel();
			if (refresh)
				refreshEinkScreenIfReady();
			if (visible) {
				startStats();
				checkSize();
			} else
				stopStats();
			super.onWindowVisibilityChanged(visibility);
		}

		@Override
		public void onWindowFocusChanged(boolean hasWindowFocus) {
			if (readerSurfaceState.isClosed()) {
				super.onWindowFocusChanged(hasWindowFocus);
				return;
			}
			ReaderSurfaceState.FocusRefresh refresh =
					readerSurfaceState.changeFocus(
							hasWindowFocus);
			if (hasWindowFocus) {
				if (DeviceInfo.EINK_SCREEN && refresh != null) {
					einkRefreshScheduler.postDelayed(
							() -> applyEinkFocusRefresh(
									refresh),
							EINK_FOCUS_REFRESH_DELAY_MS);
				}
				startStats();
				checkSize();
			} else {
				einkRefreshScheduler.cancel();
				stopStats();
			}
			super.onWindowFocusChanged(hasWindowFocus);
		}

		protected void doDraw(Canvas canvas) {
			BitmapInfo currentPage;
			synchronized (pageBitmapLifetime) {
				currentPage = mCurrentPageInfo;
			}
			try {
				log.d("doDraw() called");
				ReaderProgressState.Snapshot progress =
						progressState.snapshot();
				if (progress.isActive()) {
					log.d("onDraw() -- drawing progress "
							+ (progress.getPosition() / 100));
					drawPageBackground(canvas);
					doDrawProgress(
							canvas,
							progress.getPosition(),
							progress.getTitle());
				} else if (readerNativeLifecycle.isInitialized()
						&& currentPage != null
						&& currentPage.bitmap != null) {
					log.d("onDraw() -- drawing page image");

					AutoScrollAnimation autoScroll =
							autoScrollSessions.readySession();
					ViewAnimationControl animation =
							animationState.current();
					if (autoScroll != null
							&& autoScroll.isReadySession()) {
						autoScroll.draw(canvas);
					} else if (animation != null) {
						animation.draw(canvas);
					} else {
						Rect dst = new Rect(0, 0, canvas.getWidth(), canvas.getHeight());
						Rect src = new Rect(0, 0, currentPage.bitmap.getWidth(), currentPage.bitmap.getHeight());
						if (dontStretchWhileDrawing) {
							if (dst.right > src.right)
								dst.right = src.right;
							if (dst.bottom > src.bottom)
								dst.bottom = src.bottom;
							if (src.right > dst.right)
								src.right = dst.right;
							if (src.bottom > dst.bottom)
								src.bottom = dst.bottom;
							if (centerPageInsteadOfResizing) {
								int ddx = (canvas.getWidth() - dst.width()) / 2;
								int ddy = (canvas.getHeight() - dst.height()) / 2;
								dst.left += ddx;
								dst.right += ddx;
								dst.top += ddy;
								dst.bottom += ddy;
							}
						}
						if (dst.width() != canvas.getWidth() || dst.height() != canvas.getHeight())
							canvas.drawColor(Color.rgb(32, 32, 32));
						drawDimmedBitmap(canvas, currentPage.bitmap, src, dst);
					}
					if (progress.isCloudActive()) {
						// draw progressbar on top
						doDrawCloudSyncProgress(
								canvas,
								progress.getCloudPosition());
					}
				} else {
					log.d("onDraw() -- drawing empty screen");
					drawPageBackground(canvas);
					if (progress.isCloudActive()) {
						// draw progressbar on top
						doDrawCloudSyncProgress(
								canvas,
								progress.getCloudPosition());
					}
				}
			} catch (Exception e) {
				log.e("exception while drawing", e);
			}
		}

		@Override
		public void draw() {
			draw(false);
		}

		@Override
		public void draw(boolean isPartially) {
			drawCallback(this::doDraw, null, isPartially);
		}

		@Override
		public void invalidate() {
			super.invalidate();
		}

	}

	private DocView doc;

	// additional key codes for Nook
	public static final int NOOK_KEY_PREV_LEFT = 96;
	public static final int NOOK_KEY_PREV_RIGHT = 98;
	public static final int NOOK_KEY_NEXT_RIGHT = 97;
	public static final int NOOK_KEY_SHIFT_UP = 101;
	public static final int NOOK_KEY_SHIFT_DOWN = 100;

	// nook 1 & 2
	public static final int NOOK_12_KEY_NEXT_LEFT = 95;

	// Nook touch buttons
	public static final int KEYCODE_PAGE_BOTTOMLEFT = 0x5d; // fwd = 93 (
	//    public static final int KEYCODE_PAGE_BOTTOMRIGHT = 158; // 0x5f; // fwd = 95
	public static final int KEYCODE_PAGE_TOPLEFT = 0x5c; // back = 92
	public static final int KEYCODE_PAGE_TOPRIGHT = 0x5e; // back = 94

	public static final int SONY_DPAD_UP_SCANCODE = 105;
	public static final int SONY_DPAD_DOWN_SCANCODE = 106;
	public static final int SONY_DPAD_LEFT_SCANCODE = 125;
	public static final int SONY_DPAD_RIGHT_SCANCODE = 126;

	public static final int KEYCODE_ESCAPE = 111; // KeyEvent constant since API 11

	//    public static final int SONY_MENU_SCANCODE = 357;
//    public static final int SONY_BACK_SCANCODE = 158;
//    public static final int SONY_HOME_SCANCODE = 102;

	public static final int PAGE_ANIMATION_NONE = 0;
	public static final int PAGE_ANIMATION_PAPER = 1;
	public static final int PAGE_ANIMATION_SLIDE = 2;
	public static final int PAGE_ANIMATION_SLIDE2 = 3;
	public static final int PAGE_ANIMATION_MAX = 3;

	public static final int SEL_CMD_SELECT_FIRST_SENTENCE_ON_PAGE = 1;
	public static final int SEL_CMD_NEXT_SENTENCE = 2;
	public static final int SEL_CMD_PREV_SENTENCE = 3;

	// Double tap selections within this radius are are assumed to be attempts to select a single point
	public static final int DOUBLE_TAP_RADIUS = 60;

	private final static int BRIGHTNESS_TYPE_COMMON = 0;
	private final static int BRIGHTNESS_TYPE_WARM = 1;
	private final static int BRIGHTNESS_TYPE_BOTH = 2;

	private void execute(Engine.EngineTask task) {
		mEngine.execute(task);
	}

	private void post(Engine.EngineTask task) {
		mEngine.post(task);
	}

	private abstract class Task implements Engine.EngineTask {

		public void done() {
			// override to do something useful
		}

		public void fail(Exception e) {
			// do nothing, just log exception
			// override to do custom action
			log.e("Task " + this.getClass().getSimpleName() + " is failed with exception " + e.getMessage(), e);
		}
	}

	private final CoolReader mActivity;
	private final Engine mEngine;
	private final Scanner mScanner;
	private final History mHistory;
	private final CoverpageManager mCoverpageManager;
	private final GenresCollection mGenresCollection;
	private final DocumentFileCache mDocumentCache;
	private final ServiceLifecycle mServiceLifecycle;
	private final EinkScreen mEinkScreen;
	private final DocumentLoadLifecycle documentLoadLifecycle;

	private volatile BookInfo mBookInfo;

	private final ReaderSettingsState readerSettingsState =
			new ReaderSettingsState(new Properties());
	private final ReaderDimmingState dimmingState =
			new ReaderDimmingState();
	private final CloseableTaskGate settingsSyncLifecycle =
			new CloseableTaskGate();

	public Engine getEngine() {
		return mEngine;
	}

	public CoverpageManager getCoverpageManager() {
		return mCoverpageManager;
	}

	public CoolReader getActivity() {
		return mActivity;
	}

	public boolean isBookLoaded() {
		return mOpened;
	}

	public int getOrientation() {
		int angle = readerSettingsState.getInt(
				PROP_APP_SCREEN_ORIENTATION, 0);
		if (angle == 4)
			angle = mActivity.getOrientationFromSensor();
		return angle;
	}

	private int overrideKey(int keyCode) {
		return keyCode;
	}

	public int getTapZone(int x, int y, int dx, int dy) {
		return TapZoneGeometry.zoneAt(x, y, dx, dy);
	}

	public ReaderAction findTapZoneAction(int zone, int tapActionType) {
		return findTapZoneAction(
				zone,
				tapActionType,
				ReaderInputSettings.capture(
						readerSettingsState.snapshot()));
	}

	private ReaderAction findTapZoneAction(
			int zone,
			int tapActionType,
			ReaderInputSettings inputSettings) {
		ReaderAction action = ReaderAction.NONE;
		boolean isSecondaryAction =
				inputSettings.secondaryTapActionType()
						== tapActionType;
		if (tapActionType == TAP_ACTION_TYPE_SHORT) {
			action = findTapAction(
					zone,
					ReaderAction.NORMAL,
					inputSettings);
		} else {
			if (isSecondaryAction)
				action = findTapAction(
						zone,
						ReaderAction.LONG,
						inputSettings);
			else if (inputSettings
							.isDoubleTapSelectionEnabled()
					|| tapActionType
							== TAP_ACTION_TYPE_LONGPRESS)
				action = ReaderAction.START_SELECTION;
		}
		return action;
	}

	private static ReaderAction findTapAction(
			int zone,
			int type,
			ReaderInputSettings settings) {
		return ReaderAction.findById(
				settings.tapActionId(zone, type));
	}

	private static ReaderAction findKeyAction(
			int keyCode,
			int type,
			ReaderInputSettings settings) {
		return ReaderAction.findById(
				settings.keyActionId(keyCode, type));
	}

	public FileInfo getOpenedFileInfo() {
		if (isBookLoaded() && mBookInfo != null)
			return mBookInfo.getFileInfo();
		return null;
	}

	public final int LONG_KEYPRESS_TIME = 900;
	public final int AUTOREPEAT_KEYPRESS_TIME = 700;
	public final int DOUBLE_CLICK_INTERVAL = 400;
	private static final long KEY_DOWN_TIME_TOLERANCE = 300L;
	private final KeyDoubleClickState<ReaderAction>
			keyDoubleClickState = new KeyDoubleClickState<>();
	private final DelayedExecutor keyDoubleClickScheduler =
			DelayedExecutor.createGUI("key-double-click");
	private final KeyRepeatState<ReaderAction> keyRepeatState =
			new KeyRepeatState<>();
//	boolean VOLUME_KEYS_ZOOM = false;

	//private boolean backKeyDownHere = false;


	private final ReadingTimeTracker readingTimeTracker =
			new ReadingTimeTracker();

	public void startStats() {
		if (readingTimeTracker.start(
				android.os.SystemClock.uptimeMillis())) {
			log.d("stats: started reading");
		}
	}

	public void stopStats() {
		if (readingTimeTracker.stop(
				android.os.SystemClock.uptimeMillis())) {
			log.d("stats: stopped reading");
		}
	}

	public long getTimeElapsed() {
		return readingTimeTracker.elapsed(
				android.os.SystemClock.uptimeMillis());
	}

	public void setTimeElapsed(long timeElapsed) {
		readingTimeTracker.setElapsed(timeElapsed);
	}

	public void onAppPause() {
		stopTracking();
		if (isAutoScrollActive())
			stopAutoScroll();
		timeTickLifecycle.cancel();
		final BookInfo expectedBook = mBookInfo;
		final DocumentLoadLifecycle.Interaction interaction =
				documentLoadLifecycle.interaction();
		cancelPositionSave();
		Bookmark bookmark =
				captureCurrentPositionBookmarkSync(
						expectedBook, interaction);
		if (bookmark != null)
			savePositionBookmark(
					expectedBook, bookmark);
		if (animationTiming.hasSamples()) {
			setSetting(
					PROP_APP_VIEW_ANIM_DURATION,
					String.valueOf(animationTiming.averageDrawDuration()),
					false,
					true,
					false);
		}
		log.i("calling bookView.onPause()");
		bookView.onPause();
	}

	public void onAppResume() {
		viewportResizeState.recordResume(
				android.os.SystemClock.uptimeMillis());
		log.i("calling bookView.onResume()");
		bookView.onResume();
	}

	private KeyRepeatState.Press<ReaderAction> startTrackingKey(
			KeyEvent event, ReaderAction repeatAction) {
		if (event.getRepeatCount() != 0)
			return null;
		stopTracking();
		return keyRepeatState.begin(
				event.getKeyCode(),
				event.getDownTime(),
				repeatAction);
	}

	private void stopTracking() {
		keyRepeatState.cancel();
		cancelKeyDoubleClick();
		TapHandler handler = tapHandlerState.current();
		if (handler != null)
			handler.cancel();
	}

	private void cancelKeyDoubleClick() {
		keyDoubleClickState.cancel();
		keyDoubleClickScheduler.cancel();
	}

	private void applyDeferredKeyAction(
			KeyDoubleClickState.Pending<ReaderAction> pending) {
		BackgroundThread.ensureGUI();
		ReaderAction action =
				keyDoubleClickState.claimSingle(pending);
		if (action == null || !mServiceLifecycle.isActive())
			return;
		log.d("onKeyUp: single click action "
				+ action.id + " found");
		onAction(action);
	}

	private KeyRepeatState.Release releaseTrackedKey(
			KeyEvent event) {
		KeyRepeatState.Release release =
				keyRepeatState.release(
						event.getKeyCode(),
						event.getDownTime(),
						event.getEventTime(),
						KEY_DOWN_TIME_TOLERANCE,
						LONG_KEYPRESS_TIME);
		if (!release.isTracked()) {
			log.v("releaseTrackedKey: unmatched event "
					+ event);
			stopTracking();
		}
		return release;
	}

	private void runKeyRepeatAction(
			KeyRepeatState.Repeat<ReaderAction> repeat) {
		if (repeat == null)
			return;
		ReaderAction action = repeat.action();
		log.v("running repeat action : " + action);
		onAction(action, () -> {
			if (keyRepeatState.completeRepeat(repeat))
				log.v("repeat action is completed : "
						+ action);
		});
	}

	private int translateKeyCode(int keyCode) {
		if (DeviceInfo.REVERT_LANDSCAPE_VOLUME_KEYS && (mActivity.getScreenOrientation() & 1) != 0) {
			if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)
				return KeyEvent.KEYCODE_VOLUME_UP;
			if (keyCode == KeyEvent.KEYCODE_VOLUME_UP)
				return KeyEvent.KEYCODE_VOLUME_DOWN;
		}
		return keyCode;
	}

	private final CloseableTaskGate selectionUpdateLifecycle =
			new CloseableTaskGate();

	private void updateSelection(
			int startX, int startY, int endX, int endY,
			final boolean isUpdateEnd) {
		final BookInfo expectedBook = mBookInfo;
		final DocumentLoadLifecycle.Interaction interaction =
				documentLoadLifecycle.interaction();
		updateSelection(
				startX, startY, endX, endY, isUpdateEnd,
				expectedBook, interaction);
	}

	private void updateSelection(
			int startX, int startY, int endX, int endY,
			final boolean isUpdateEnd,
			final BookInfo expectedBook,
			final DocumentLoadLifecycle.Interaction interaction) {
		if (!isDocumentInteractionCurrent(
				expectedBook, interaction))
			return;
		final ReaderRenderRequest renderRequest =
				ReaderRenderRequest.fromInteraction(
						expectedBook, interaction);
		final Selection sel = new Selection();
		final CloseableTaskGate.Token owner =
				selectionUpdateLifecycle.replace();
		if (owner == null)
			return;
		sel.startX = startX;
		sel.startY = startY;
		sel.endX = endX;
		sel.endY = endY;
		mEngine.execute(new Task() {
			@Override
			public void work() throws Exception {
				if (!selectionUpdateLifecycle.isActive(owner)
						|| !isDocumentInteractionCurrent(
								expectedBook, interaction))
					return;
				doc.updateSelection(sel);
				if (!selectionUpdateLifecycle.isActive(owner)
						|| !isDocumentInteractionCurrent(
								expectedBook, interaction))
					return;
				if (!sel.isEmpty()) {
					pageInvalidationState.invalidate();
					BitmapInfo bi =
							preparePageImage(
									0, renderRequest);
					if (bi != null) {
						bookView.draw(true);
					}
				}
			}

			@Override
			public void done() {
				if (!selectionUpdateLifecycle.complete(owner))
					return;
				if (!isDocumentInteractionCurrent(
						expectedBook, interaction))
					return;
				if (isUpdateEnd) {
					String text = sel.text;
					if (text != null && text.length() > 0) {
						onSelectionComplete(
								sel, expectedBook, interaction);
					} else {
						clearSelection(
								expectedBook, interaction);
					}
				}
			}

			@Override
			public void fail(Exception e) {
				selectionUpdateLifecycle.complete(owner);
				super.fail(e);
			}
		});
	}

	private void cancelSelectionUpdates() {
		selectionUpdateLifecycle.cancel();
	}

	private void closeSelectionUpdates() {
		selectionUpdateLifecycle.close();
	}

	public static boolean isMultiSelection(Selection sel) {
		String str = sel.text;
		if (str != null) {
			for (int i = 0; i < str.length(); i++) {
				if (Character.isWhitespace(str.charAt(i))) {
					return true;
				}
			}
		}
		return false;
	}

	private void onSelectionComplete(
			Selection sel, BookInfo expectedBook,
			DocumentLoadLifecycle.Interaction interaction) {
		if (!isDocumentInteractionCurrent(
				expectedBook, interaction))
			return;
		ReaderInputSettings inputSettings =
				ReaderInputSettings.capture(
						readerSettingsState.snapshot());
		int iSelectionAction =
				inputSettings.selectionAction(
						isMultiSelection(sel));

		switch (iSelectionAction) {
			case SELECTION_ACTION_TOOLBAR:
				SelectionToolbarDlg.showDialog(
						mActivity, surface, sel,
						selectionToolbarHandler(
								expectedBook, interaction));
				break;
			case SELECTION_ACTION_COPY:
				copyToClipboard(sel.text);
				clearSelection(expectedBook, interaction);
				break;
			case SELECTION_ACTION_DICTIONARY:
				mActivity.findInDictionary(sel.text);
				if (!getSettings().getBool(PROP_APP_SELECTION_PERSIST, false))
					clearSelection(expectedBook, interaction);
				break;
			case SELECTION_ACTION_BOOKMARK:
				clearSelection(expectedBook, interaction);
				showNewBookmarkDialog(
						sel, expectedBook, interaction);
				break;
			case SELECTION_ACTION_FIND:
				clearSelection(expectedBook, interaction);
				showSearchDialog(
						sel.text, expectedBook, interaction);
				break;
			default:
				clearSelection(expectedBook, interaction);
				break;
		}

	}

	private SelectionToolbarHandler selectionToolbarHandler(
			final BookInfo expectedBook,
			final DocumentLoadLifecycle.Interaction interaction) {
		return new SelectionToolbarHandler() {
			@Override
			public boolean isActive() {
				return isDocumentInteractionCurrent(
						expectedBook, interaction);
			}

			@Override
			public ReaderViewModeState.Lease enterAdjustmentMode() {
				if (!isActive())
					return null;
				return acquireTemporaryScrollMode();
			}

			@Override
			public void restoreAdjustmentMode(
					ReaderViewModeState.Lease lease) {
				releaseTemporaryScrollMode(lease);
			}

			@Override
			public void moveSelectionBound(
					boolean start,
					int delta,
					SelectionUpdateHandler updateHandler) {
				ReaderCommand command =
						start
								? ReaderCommand
										.DCMD_SELECT_MOVE_LEFT_BOUND_BY_WORDS
								: ReaderCommand
										.DCMD_SELECT_MOVE_RIGHT_BOUND_BY_WORDS;
				moveSelection(
						command,
						delta,
						new MoveSelectionCallback() {
							@Override
							public void onNewSelection(
									Selection selection) {
								updateHandler.onNewSelection(
										selection);
							}

							@Override
							public void onFail() {
								updateHandler.onFail();
							}
						},
						expectedBook,
						interaction);
			}

			@Override
			public void clearSelection() {
				ReaderView.this.clearSelection(
						expectedBook, interaction);
			}

			@Override
			public void copyToClipboard(String text) {
				if (isActive())
					ReaderView.this.copyToClipboard(text);
			}

			@Override
			public boolean shouldPersistSelection() {
				return isActive()
						&& getSettings().getBool(
								PROP_APP_SELECTION_PERSIST,
								false);
			}

			@Override
			public void showNewBookmark(Selection selection) {
				ReaderView.this.showNewBookmarkDialog(
						selection, expectedBook, interaction);
			}

			@Override
			public void showBookmarks() {
				ReaderView.this.showBookmarksDialog(
						expectedBook, interaction);
			}

			@Override
			public void sendQuotation(Selection selection) {
				sendQuotationInEmail(
						selection, expectedBook, interaction);
			}

			@Override
			public void showSearch(String initialText) {
				showSearchDialog(
						initialText, expectedBook, interaction);
			}

			@Override
			public void scrollBy(int delta) {
				if (isActive())
					doEngineCommand(
							ReaderCommand.DCMD_SCROLL_BY,
							delta);
			}
		};
	}

	public void showNewBookmarkDialog(Selection sel) {
		BackgroundThread.ensureGUI();
		showNewBookmarkDialog(
				sel, mBookInfo,
				documentLoadLifecycle.interaction());
	}

	boolean showNewBookmarkDialog(
			Selection sel, BookInfo expectedBook,
			DocumentLoadLifecycle.Interaction interaction) {
		BackgroundThread.ensureGUI();
		if (sel == null || !isDocumentInteractionCurrent(
				expectedBook, interaction))
			return false;
		Bookmark bmk = new Bookmark();
		bmk.setType(Bookmark.TYPE_COMMENT);
		bmk.setPosText(sel.text);
		bmk.setStartPos(sel.startPos);
		bmk.setEndPos(sel.endPos);
		bmk.setPercent(sel.percent);
		bmk.setTitleText(sel.chapter);
		showBookmarkEditDialog(
				bmk, true, expectedBook, interaction);
		return true;
	}

	private boolean showBookmarkEditDialog(
			Bookmark bookmark, boolean isNew,
			BookInfo expectedBook,
			DocumentLoadLifecycle.Interaction interaction) {
		BackgroundThread.ensureGUI();
		if (bookmark == null || !isDocumentInteractionCurrent(
				expectedBook, interaction))
			return false;
		BookmarkEditDialog dlg = new BookmarkEditDialog(
				mActivity,
				bookmarkInteractionHandler(
						expectedBook, interaction),
				bookmark, isNew);
		dlg.show();
		return true;
	}

	private void sendQuotationInEmail(
			Selection sel,
			BookInfo expectedBook,
			DocumentLoadLifecycle.Interaction interaction) {
		BackgroundThread.ensureGUI();
		if (sel == null || !isDocumentInteractionCurrent(
				expectedBook, interaction)
				|| expectedBook.getFileInfo() == null)
			return;
		StringBuilder buf = new StringBuilder();
		if (expectedBook.getFileInfo().authors != null)
			buf.append("|")
					.append(expectedBook.getFileInfo().authors)
					.append("\n");
		if (expectedBook.getFileInfo().title != null)
			buf.append("|")
					.append(expectedBook.getFileInfo().title)
					.append("\n");
		if (sel.chapter != null && sel.chapter.length() > 0)
			buf.append("|").append(sel.chapter).append("\n");
		buf.append(sel.text).append("\n");
		mActivity.sendBookFragment(
				expectedBook, buf.toString());
	}

	public void copyToClipboard(String text) {
		if (text != null && text.length() > 0) {
			ClipboardManager cm = mActivity.getClipboardmanager();
			cm.setText(text);
			log.i("Setting clipboard text: " + text);
			mActivity.showToast("Selection text copied to clipboard");
		}
	}

//	private void cancelSelection() {
//		//
//		selectionInProgress = false;
//		clearSelection();
//	}

	private boolean isTouchScreenEnabled = true;
	private final SelectionModeState selectionModeState =
			new SelectionModeState();

	public void toggleSelectionMode() {
		boolean active = selectionModeState.toggle();
		mActivity.showToast(
				active
						? R.string.action_toggle_selection_mode_on
						: R.string.action_toggle_selection_mode_off);
	}

	private final ReaderImageViewerState<ImageViewer> imageViewerState =
			new ReaderImageViewerState<>();

	private class ImageViewer extends SimpleOnGestureListener {
		private final BookInfo expectedBook;
		private final DocumentLoadLifecycle.Interaction interaction;
		private final GestureDetector detector;
		private final int oldOrientation;

		ImageViewer(
				BookInfo expectedBook,
				DocumentLoadLifecycle.Interaction interaction) {
			this.expectedBook = expectedBook;
			this.interaction = interaction;
			oldOrientation = lockOrientation();
			detector = new GestureDetector(this);
		}

		private ImageInfo prepareInitialImage(
				ImageInfo initialImage) {
			if (initialImage == null)
				return null;
			ImageInfo image = new ImageInfo(initialImage);
			if (image.height > 0
					&& image.width > 0
					&& image.bufHeight / image.height >= 2
					&& image.bufWidth / image.width >= 2) {
				image.scaledHeight *= 2;
				image.scaledWidth *= 2;
			}
			centerIfLessThanScreen(image);
			return image;
		}

		private void abandonStart() {
			unlockOrientation();
		}

		private int lockOrientation() {
			int orientation = mActivity.getScreenOrientation();
			if (orientation == 4)
				mActivity.setScreenOrientation(mActivity.getOrientationFromSensor());
			return orientation;
		}

		private void unlockOrientation() {
			if (oldOrientation == 4)
				mActivity.setScreenOrientation(oldOrientation);
		}

		private void centerIfLessThanScreen(ImageInfo image) {
			if (image.scaledHeight < image.bufHeight)
				image.y = (image.bufHeight - image.scaledHeight) / 2;
			if (image.scaledWidth < image.bufWidth)
				image.x = (image.bufWidth - image.scaledWidth) / 2;
		}

		private void fixScreenBounds(ImageInfo image) {
			if (image.scaledHeight > image.bufHeight) {
				if (image.y < image.bufHeight - image.scaledHeight)
					image.y = image.bufHeight - image.scaledHeight;
				if (image.y > 0)
					image.y = 0;
			}
			if (image.scaledWidth > image.bufWidth) {
				if (image.x < image.bufWidth - image.scaledWidth)
					image.x = image.bufWidth - image.scaledWidth;
				if (image.x > 0)
					image.x = 0;
			}
		}

		private void updateImage(ImageInfo image) {
			if (!isActive())
				return;
			centerIfLessThanScreen(image);
			fixScreenBounds(image);
			if (imageViewerState.update(this, image))
				drawPage(
						null,
						false,
						ReaderRenderRequest.fromInteraction(
								expectedBook, interaction));
		}

		public void zoomIn() {
			ImageInfo image = imageViewerState.snapshot(this);
			if (image == null || image.width <= 0 || image.height <= 0)
				return;
			if (image.scaledHeight >= image.height) {
				int scale = image.scaledHeight / image.height;
				if (scale < 4)
					scale++;
				image.scaledHeight = image.height * scale;
				image.scaledWidth = image.width * scale;
			} else {
				int scale = image.height / image.scaledHeight;
				if (scale > 1)
					scale--;
				image.scaledHeight = image.height / scale;
				image.scaledWidth = image.width / scale;
			}
			updateImage(image);
		}

		public void zoomOut() {
			ImageInfo image = imageViewerState.snapshot(this);
			if (image == null || image.width <= 0 || image.height <= 0)
				return;
			if (image.scaledHeight > image.height) {
				int scale = image.scaledHeight / image.height;
				if (scale > 1)
					scale--;
				image.scaledHeight = image.height * scale;
				image.scaledWidth = image.width * scale;
			} else {
				int scale = image.height / image.scaledHeight;
				if (image.scaledHeight > image.bufHeight || image.scaledWidth > image.bufWidth)
					scale++;
				image.scaledHeight = image.height / scale;
				image.scaledWidth = image.width / scale;
			}
			updateImage(image);
		}

		public int getStep() {
			ImageInfo image = imageViewerState.snapshot(this);
			if (image == null)
				return 0;
			int max = image.bufHeight;
			if (max < image.bufWidth)
				max = image.bufWidth;
			return Math.max(1, max / 10);
		}

		public void moveBy(int dx, int dy) {
			ImageInfo image = imageViewerState.snapshot(this);
			if (image == null)
				return;
			image.x += dx;
			image.y += dy;
			updateImage(image);
		}

		public boolean onKeyDown(int keyCode, final KeyEvent event) {
			if (!isActive())
				return false;
			if (keyCode == 0)
				keyCode = event.getScanCode();
			switch (keyCode) {
				case KeyEvent.KEYCODE_VOLUME_UP:
					zoomIn();
					return true;
				case KeyEvent.KEYCODE_VOLUME_DOWN:
					zoomOut();
					return true;
				case KeyEvent.KEYCODE_DPAD_CENTER:
				case KeyEvent.KEYCODE_BACK:
				case KeyEvent.KEYCODE_ENDCALL:
					close();
					return true;
				case KeyEvent.KEYCODE_DPAD_LEFT:
					moveBy(getStep(), 0);
					return true;
				case KeyEvent.KEYCODE_DPAD_RIGHT:
					moveBy(-getStep(), 0);
					return true;
				case KeyEvent.KEYCODE_DPAD_UP:
					moveBy(0, getStep());
					return true;
				case KeyEvent.KEYCODE_DPAD_DOWN:
					moveBy(0, -getStep());
					return true;
			}
			return false;
		}

		public boolean onKeyUp(int keyCode, final KeyEvent event) {
			if (!isActive())
				return false;
			if (keyCode == 0)
				keyCode = event.getScanCode();
			switch (keyCode) {
				case KeyEvent.KEYCODE_BACK:
				case KeyEvent.KEYCODE_ENDCALL:
					close();
					return true;
			}
			return false;
		}

		public boolean onTouchEvent(MotionEvent event) {
			if (!isActive())
				return false;
//			int aindex = event.getActionIndex();
//			if (event.getAction() == MotionEvent.ACTION_POINTER_DOWN) {
//				log.v("ACTION_POINTER_DOWN");
//			}
			return detector.onTouchEvent(event);
		}


		@Override
		public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
							   float velocityY) {
			log.v("onFling()");
			return true;
		}

		@Override
		public boolean onScroll(MotionEvent e1, MotionEvent e2,
								float distanceX, float distanceY) {
			log.v("onScroll() " + distanceX + ", " + distanceY);
			int dx = (int) distanceX;
			int dy = (int) distanceY;
			moveBy(-dx, -dy);
			return true;
		}

		@Override
		public boolean onSingleTapConfirmed(MotionEvent e) {
			log.v("onSingleTapConfirmed()");
			ImageInfo image = imageViewerState.snapshot(this);
			if (image == null)
				return false;

			int x = (int) e.getX();
			int y = (int) e.getY();

			int zone = 0;
			int zw = mActivity.getDensityDpi() / 2;
			int w = image.bufWidth;
			int h = image.bufHeight;
			if (image.rotation == 0) {
				if (x < zw && y > h - zw)
					zone = 1;
				if (x > w - zw && y > h - zw)
					zone = 2;
			} else {
				if (x < zw && y < zw)
					zone = 1;
				if (x < zw && y > h - zw)
					zone = 2;
			}
			if (zone != 0) {
				if (zone == 1)
					zoomIn();
				else
					zoomOut();
				return true;
			}

			close();
			return super.onSingleTapConfirmed(e);
		}

		@Override
		public boolean onDown(MotionEvent e) {
			return true;
		}

		public void close() {
			close(true);
		}

		private void close(boolean redraw) {
			if (!imageViewerState.finish(this))
				return;
			unlockOrientation();
			post(new Task() {
				@Override
				public void work() {
					BackgroundThread.ensureBackground();
					if (readerNativeLifecycle.isInitialized())
						doc.closeImage();
				}

				@Override
				public void done() {
					BackgroundThread.ensureGUI();
					if (redraw)
						drawPage(
								null,
								false,
								ReaderRenderRequest.fromInteraction(
										expectedBook,
										interaction));
				}
			});
		}

		public BitmapInfo prepareImage(
				ReaderRenderRequest renderRequest,
				ViewportResizeState.Size viewport) {
			// called from background thread
			if (!isActive()
					|| !isRenderRequestCurrent(renderRequest))
				return null;
			ImageInfo img =
					imageViewerState.snapshotForBuffer(
							this,
							viewport.width(),
							viewport.height());
			if (img == null || !isActive())
				return null;
			if (mCurrentPageInfo != null) {
				if (img.equals(mCurrentPageInfo.imageInfo)
						&& isRenderRequestCurrent(
								renderRequest))
					return mCurrentPageInfo;
			}
			PositionProperties currpos = doc.getPositionProps(null, false);
			if (!isRenderRequestCurrent(renderRequest))
				return null;
			BitmapInfo bi = new BitmapInfo();
			bi.imageInfo = new ImageInfo(img);
			bi.bitmap = factory.get(
					viewport.width(), viewport.height());
			bi.position = currpos;
			if (!doc.drawImage(bi.bitmap, bi.imageInfo)
					|| !isActive()
					|| !isRenderRequestCurrent(renderRequest)) {
				bi.recycle();
				return null;
			}
			return publishCurrentPageCandidate(
					bi, renderRequest);
		}

		private boolean isActive() {
			return imageViewerState.isActive(this)
					&& readerNativeLifecycle.isInitialized()
					&& mOpened
					&& isDocumentInteractionCurrent(
							expectedBook, interaction);
		}
	}

	private void startImageViewer(
			ImageInfo image,
			BookInfo expectedBook,
			DocumentLoadLifecycle.Interaction interaction) {
		BackgroundThread.ensureGUI();
		if (image == null
				|| imageViewerState.current() != null
				|| !isDocumentInteractionCurrent(
						expectedBook, interaction))
			return;
		ImageViewer viewer =
				new ImageViewer(
						expectedBook, interaction);
		ImageInfo initialImage =
				viewer.prepareInitialImage(image);
		if (!isDocumentInteractionCurrent(
				expectedBook, interaction)
				|| !imageViewerState.startIfIdle(
						viewer, initialImage)) {
			viewer.abandonStart();
			return;
		}
		drawPage(
				null,
				false,
				ReaderRenderRequest.fromInteraction(
						expectedBook, interaction));
	}

	private boolean isImageViewMode() {
		ImageViewer viewer = imageViewerState.current();
		return viewer != null && viewer.isActive();
	}

	private void stopImageViewer() {
		ImageViewer viewer = imageViewerState.current();
		if (viewer != null)
			viewer.close(false);
	}

	private final CloseableTaskGate tapGestureLifecycle =
			new CloseableTaskGate();
	private final DelayedExecutor tapGestureScheduler =
			DelayedExecutor.createGUI("tap-gesture");
	private final TapBounceState tapBounceState =
			new TapBounceState();
	private final TapHandlerState<TapHandler> tapHandlerState =
			new TapHandlerState<>();

	private void scheduleTapGestureTimeout(
			TapHandler handler,
			Runnable timeout,
			long delay) {
		CloseableTaskGate.Token owner =
				tapGestureLifecycle.replace();
		if (owner == null)
			return;
		tapGestureScheduler.postDelayed(() -> {
			if (!tapGestureLifecycle.complete(owner)
					|| !tapHandlerState.isCurrent(handler)
					|| !mServiceLifecycle.isActive())
				return;
			timeout.run();
		}, delay);
	}

	private void cancelTapGestureTimeout() {
		tapGestureLifecycle.cancel();
		tapGestureScheduler.cancel();
	}

	private void closeGestureTimeouts() {
		keyRepeatState.close();
		keyDoubleClickState.close();
		keyDoubleClickScheduler.cancel();
		tapGestureLifecycle.close();
		tapGestureScheduler.cancel();
		tapBounceState.close();
		tapHandlerState.close();
		selectionModeState.close();
	}

	public class TapHandler {

		private final static int STATE_INITIAL = 0; // no events yet
		private final static int STATE_DOWN_1 = 1; // down first time
		private final static int STATE_SELECTION = 3; // selection is started
		private final static int STATE_FLIPPING = 4; // flipping is in progress
		private final static int STATE_WAIT_FOR_DOUBLE_CLICK = 5; // flipping is in progress
		private final static int STATE_DONE = 6; // done: no more tracking
		private final static int STATE_BRIGHTNESS = 7; // brightness change in progress
		private final static int STATE_FLIP_TRACKING = 8; // pages flip tracking in progress

		private final static int EXPIRATION_TIME_MS = 180000;

		int state = STATE_INITIAL;
		int brightness_type = BRIGHTNESS_TYPE_COMMON;

		int start_x = 0;
		int start_y = 0;
		int width = 0;
		int height = 0;
		ReaderAction shortTapAction = ReaderAction.NONE;
		ReaderAction longTapAction = ReaderAction.NONE;
		ReaderAction doubleTapAction = ReaderAction.NONE;
		private ReaderInputSettings inputSettings;
		long firstDown;

		/// handle unexpected event for state: stop tracking
		private boolean unexpectedEvent() {
			cancel();
			return true; // ignore
		}

		public boolean isInitialState() {
			return state == STATE_INITIAL;
		}

		public void checkExpiration() {
			if (state != STATE_INITIAL && Utils.timeInterval(firstDown) > EXPIRATION_TIME_MS)
				cancel();
		}

		/// cancel current action and reset touch tracking state
		private boolean cancel() {
			if (!tapHandlerState.isCurrent(this))
				return true;
			cancelTapGestureTimeout();
			if (state == STATE_INITIAL)
				return true;
			switch (state) {
				case STATE_DOWN_1:
				case STATE_SELECTION:
					clearSelection();
					break;
				case STATE_FLIPPING:
					stopAnimation(-1, -1);
					break;
				case STATE_WAIT_FOR_DOUBLE_CLICK:
				case STATE_DONE:
				case STATE_BRIGHTNESS:
				case STATE_FLIP_TRACKING:
					stopBrightnessControl(-1, -1, brightness_type);
					break;
			}
			state = STATE_DONE;
			unhiliteTapZone();
			tapHandlerState.replace(
					this,
					new TapHandler());
			return true;
		}

		private void adjustStartValuesOnDrag(int swipeDistance, int distanceForFlip) {
			if (Math.abs(swipeDistance) < distanceForFlip) {
				return; // Nothing to do
			}
			int direction = swipeDistance > 0 ? 1 : -1; // Left-to-right or right-to-left swipe?
			int value = direction * distanceForFlip;
			while (Math.abs(swipeDistance) >= distanceForFlip) {
				if (isPageMode()) {
					start_x += value;
				} else {
					start_y += value;
				}
				swipeDistance -= value;
			}
		}

		private void updatePageFlipTracking(final int x, final int y) {
			if (!mOpened)
				return;
			final int pageFlipsPerFullSwipe =
					inputSettings.pageFlipsPerFullSwipe();
			if (pageFlipsPerFullSwipe <= 1)
				return;
			final int swipeDistance =
					isPageMode() ? x - start_x : y - start_y;
			final int distanceForFlip =
					Math.max(
							1,
							surface.getWidth()
									/ pageFlipsPerFullSwipe);
			int pagesToFlip = swipeDistance / distanceForFlip;
			if (pagesToFlip == 0) {
				return; // Nothing to do
			}
			adjustStartValuesOnDrag(swipeDistance, distanceForFlip);
			ReaderAction action = pagesToFlip > 0 ? ReaderAction.PAGE_DOWN : ReaderAction.PAGE_UP;
			while (pagesToFlip != 0) {
				onAction(action);
				if (pagesToFlip > 0) {
					pagesToFlip--;
				} else {
					pagesToFlip++;
				}
			}
		}

		private boolean isBacklightFlickEdge(
				int flick, int dragThreshold) {
			int edgeWidth = dragThreshold * 170 / 100;
			return (flick == BACKLIGHT_CONTROL_FLICK_LEFT
							&& start_x < edgeWidth)
					|| (flick
									== BACKLIGHT_CONTROL_FLICK_RIGHT
							&& start_x > width - edgeWidth);
		}

		/// perform action and reset touch tracking state
		private boolean performAction(final ReaderAction action, boolean checkForLinks) {
			if (!tapHandlerState.isCurrent(this))
				return true;
			cancelTapGestureTimeout();
			log.d("performAction on touch: " + action);
			state = STATE_DONE;

			tapHandlerState.replace(
					this,
					new TapHandler());

			if (!checkForLinks) {
				onAction(action);
				return true;
			}

			// check link before executing action
			final BookInfo gestureBook = mBookInfo;
			final DocumentLoadLifecycle.Interaction
					gestureInteraction =
							documentLoadLifecycle.interaction();
			mEngine.execute(new Task() {
				String link;
				ImageInfo image;
				Bookmark bookmark;
				boolean internalLinkMoved;

				public void work() {
					if (!isDocumentInteractionCurrent(
							gestureBook,
							gestureInteraction))
						return;
					ViewportResizeState.Size viewport =
							viewportResizeState
									.appliedOrRequestedSize();
					image = new ImageInfo();
					image.bufWidth = viewport.width();
					image.bufHeight = viewport.height();
					image.bufDpi = mActivity.getDensityDpi();
					if (doc.checkImage(start_x, start_y, image)) {
						return;
					}
					image = null;
					link = doc.checkLink(start_x, start_y, mActivity.getPalmTipPixels() / 2);
					if (link != null) {
						if (link.startsWith("#")) {
							log.d("go to " + link);
							internalLinkMoved =
									doc.goLink(link) != 0;
							if (internalLinkMoved
									&& isDocumentInteractionCurrent(
											gestureBook,
											gestureInteraction))
								updateCurrentPositionStatus(
										gestureBook,
										gestureInteraction);
						}
						return;
					}
					bookmark = doc.checkBookmark(start_x, start_y);
					if (bookmark != null && bookmark.getType() == Bookmark.TYPE_POSITION)
						bookmark = null;
				}

				public void done() {
					if (!isDocumentInteractionCurrent(
							gestureBook,
							gestureInteraction))
						return;
					if (bookmark != null)
						bookmark = gestureBook.findBookmark(
								bookmark);
					if (link == null && image == null && bookmark == null) {
						onAction(action);
					} else if (image != null) {
						startImageViewer(
								image,
								gestureBook,
								gestureInteraction);
					} else if (bookmark != null) {
						showBookmarkEditDialog(
								bookmark, false,
								gestureBook,
								gestureInteraction);
					} else if (internalLinkMoved) {
						drawPage();
						scheduleSaveCurrentPositionBookmark(
								DEF_SAVE_POSITION_INTERVAL,
								gestureBook,
								gestureInteraction);
					} else if (!link.startsWith("#")) {
						log.d("external link " + link);
						if (link.startsWith("http://") || link.startsWith("https://")) {
							mActivity.openURL(link);
						} else {
							// absolute path to file
							FileInfo fi = new FileInfo(link);
							if (fi.exists()) {
								mActivity.loadDocument(fi, true);
								return;
							}
							File baseDir = null;
							if (gestureBook.getFileInfo() != null) {
								if (!gestureBook.getFileInfo().isArchive) {
									// relatively to base directory
									File f = new File(
											gestureBook.getFileInfo()
													.getBasePath());
									baseDir = f.getParentFile();
									String url = link;
									while (baseDir != null && url != null && url.startsWith("../")) {
										baseDir = baseDir.getParentFile();
										url = url.substring(3);
									}
									if (baseDir != null && url != null && url.length() > 0) {
										fi = new FileInfo(baseDir.getAbsolutePath() + "/" + url);
										if (fi.exists()) {
											mActivity.loadDocument(fi, true);
											return;
										}
									}
								} else {
									// from archive
									fi = new FileInfo(
											gestureBook.getFileInfo()
													.getArchiveName()
													+ FileInfo.ARC_SEPARATOR
													+ link);
									if (fi.exists()) {
										mActivity.loadDocument(fi, true);
										return;
									}
								}
							}
							mActivity.showToast("Cannot open link " + link);
						}
					}
				}
			});
			return true;
		}

		private boolean startSelection() {
			if (!tapHandlerState.isCurrent(this))
				return true;
			cancelTapGestureTimeout();
			state = STATE_SELECTION;
			// check link before executing action
			final BookInfo gestureBook = mBookInfo;
			final DocumentLoadLifecycle.Interaction
					gestureInteraction =
							documentLoadLifecycle.interaction();
			mEngine.execute(new Task() {
				ImageInfo image;
				Bookmark bookmark;

				public void work() {
					if (!isDocumentInteractionCurrent(
							gestureBook,
							gestureInteraction))
						return;
					ViewportResizeState.Size viewport =
							viewportResizeState
									.appliedOrRequestedSize();
					image = new ImageInfo();
					image.bufWidth = viewport.width();
					image.bufHeight = viewport.height();
					image.bufDpi = mActivity.getDensityDpi();
					if (!doc.checkImage(start_x, start_y, image))
						image = null;
					bookmark = doc.checkBookmark(start_x, start_y);
					if (bookmark != null && bookmark.getType() == Bookmark.TYPE_POSITION)
						bookmark = null;
				}

				public void done() {
					if (!tapHandlerState.isCurrent(
							TapHandler.this)
							|| !isDocumentInteractionCurrent(
									gestureBook,
									gestureInteraction))
						return;
					if (bookmark != null)
						bookmark = gestureBook.findBookmark(
								bookmark);
					if (image != null) {
						cancel();
						startImageViewer(
								image,
								gestureBook,
								gestureInteraction);
					} else if (bookmark != null) {
						cancel();
						showBookmarkEditDialog(
								bookmark, false,
								gestureBook,
								gestureInteraction);
					} else {
						updateSelection(
								start_x, start_y,
								start_x, start_y, false,
								gestureBook,
								gestureInteraction);
					}
				}
			});
			return true;
		}

		private boolean trackDoubleTap() {
			state = STATE_WAIT_FOR_DOUBLE_CLICK;
			scheduleTapGestureTimeout(this, () -> {
				if (tapHandlerState.isCurrent(TapHandler.this)
						&& state == STATE_WAIT_FOR_DOUBLE_CLICK)
					performAction(shortTapAction, false);
			}, DOUBLE_CLICK_INTERVAL);
			return true;
		}

		private boolean trackLongTap() {
			scheduleTapGestureTimeout(this, () -> {
				if (tapHandlerState.isCurrent(TapHandler.this)
						&& state == STATE_DOWN_1) {
					if (longTapAction == ReaderAction.START_SELECTION)
						startSelection();
					else
						performAction(longTapAction, true);
				}
			}, LONG_KEYPRESS_TIME);
			return true;
		}

		public boolean onTouchEvent(MotionEvent event) {
			int x = (int) event.getX();
			int y = (int) event.getY();
			if ((DeviceInfo.getSDKLevel() >= 19) && mActivity.isFullscreen() && (event.getAction() == MotionEvent.ACTION_DOWN)) {
				if ((y < 30) || (y > (getSurface().getHeight() - 30)))
					return unexpectedEvent();
			}

			if (state == STATE_INITIAL && event.getAction() != MotionEvent.ACTION_DOWN)
				return unexpectedEvent(); // ignore unexpected event

			if (state == STATE_INITIAL)
				inputSettings =
						ReaderInputSettings.capture(
								readerSettingsState
										.snapshot());

			if (!inputSettings.isDoubleTapSelectionEnabled()
					&& inputSettings
							.secondaryTapActionType()
							!= TAP_ACTION_TYPE_DOUBLE) {
				// filter bounce (only when double taps not enabled)
				if (event.getAction() == MotionEvent.ACTION_DOWN) {
					if (state == STATE_INITIAL
							&& tapBounceState.shouldReject(
									Utils.timeStamp(),
									inputSettings
											.bounceTapIntervalMs()))
						return unexpectedEvent(); // ignore bounced taps
				}
			}

			if (event.getAction() == MotionEvent.ACTION_UP) {
				long duration = Utils.timeInterval(firstDown);
				switch (state) {
					case STATE_DOWN_1:
						if (inputSettings
								.isTapZoneHighlightEnabled()) {
							TapHighlightState.Show highlight =
									showTapHighlight(
											x, y, width, height);
							scheduleUnhilite(
									highlight,
									LONG_KEYPRESS_TIME);
						}
						if (duration > LONG_KEYPRESS_TIME) {
							if (longTapAction == ReaderAction.START_SELECTION)
								return startSelection();
							return performAction(longTapAction, true);
						}
						if (doubleTapAction.isNone())
							return performAction(shortTapAction, false);
						// start possible double tap tracking
						return trackDoubleTap();
					case STATE_FLIPPING:
						stopAnimation(x, y);
						state = STATE_DONE;
						return cancel();
					case STATE_BRIGHTNESS:
						stopBrightnessControl(x, y, brightness_type);
						state = STATE_DONE;
						return cancel();
					case STATE_SELECTION:
						// If the second tap is within a radius of the first tap point, assume the user is trying to double tap on the same point
						if (start_x - x <= DOUBLE_TAP_RADIUS && x - start_x <= DOUBLE_TAP_RADIUS && y - start_y <= DOUBLE_TAP_RADIUS && start_y - y <= DOUBLE_TAP_RADIUS)
							updateSelection(start_x, start_y, start_x, start_y, true);
						else
							updateSelection(start_x, start_y, x, y, true);
						selectionModeState.consume();
						state = STATE_DONE;
						return cancel();
					case STATE_FLIP_TRACKING:
						updatePageFlipTracking(x, y);
						state = STATE_DONE;
						return cancel();
				}
			} else if (event.getAction() == MotionEvent.ACTION_DOWN) {
				switch (state) {
					case STATE_INITIAL:
						start_x = x;
						start_y = y;
						width = surface.getWidth();
						height = surface.getHeight();
						int zone = getTapZone(x, y, width, height);
						shortTapAction = findTapZoneAction(
								zone,
								TAP_ACTION_TYPE_SHORT,
								inputSettings);
						longTapAction = findTapZoneAction(
								zone,
								TAP_ACTION_TYPE_LONGPRESS,
								inputSettings);
						doubleTapAction = findTapZoneAction(
								zone,
								TAP_ACTION_TYPE_DOUBLE,
								inputSettings);
						firstDown = Utils.timeStamp();
						tapBounceState.recordTap(firstDown);
						if (selectionModeState.isActive()) {
							startSelection();
						} else {
							state = STATE_DOWN_1;
							trackLongTap();
						}
						return true;
					case STATE_DOWN_1:
					case STATE_BRIGHTNESS:
					case STATE_FLIPPING:
					case STATE_SELECTION:
					case STATE_FLIP_TRACKING:
						return unexpectedEvent();
					case STATE_WAIT_FOR_DOUBLE_CLICK:
						if (doubleTapAction == ReaderAction.START_SELECTION)
							return startSelection();
						return performAction(doubleTapAction, true);
				}
			} else if (event.getAction() == MotionEvent.ACTION_MOVE) {
				int dx = x - start_x;
				int dy = y - start_y;
				int adx = dx > 0 ? dx : -dx;
				int ady = dy > 0 ? dy : -dy;
				int distance = adx + ady;
				int dragThreshold = mActivity.getPalmTipPixels();
				switch (state) {
					case STATE_DOWN_1:
						if (distance < dragThreshold)
							return true;
						int backlightControlFlick =
								inputSettings
										.backlightControlFlick();
						if ((!DeviceInfo.EINK_SCREEN
								|| DeviceInfo.EINK_HAVE_FRONTLIGHT)
								&& backlightControlFlick
										!= BACKLIGHT_CONTROL_FLICK_NONE
								&& ady > adx) {
							// backlight control enabled
							if (isBacklightFlickEdge(
									backlightControlFlick,
									dragThreshold)) {
								// brightness
								cancelTapGestureTimeout();
								state = STATE_BRIGHTNESS;
								brightness_type =
										inputSettings
												.isColdWarmBacklightControlTogether()
												? BRIGHTNESS_TYPE_BOTH
												: BRIGHTNESS_TYPE_COMMON;
								startBrightnessControl(start_x, start_y, brightness_type);
								return true;
							}
						}
						int warmBacklightControlFlick =
								inputSettings
										.warmBacklightControlFlick();
						if (DeviceInfo.EINK_HAVE_NATURAL_BACKLIGHT
								&& warmBacklightControlFlick
										!= BACKLIGHT_CONTROL_FLICK_NONE
								&& ady > adx) {
							// warm backlight control enabled
							if (isBacklightFlickEdge(
									warmBacklightControlFlick,
									dragThreshold)) {
								// warm backlight brightness
								cancelTapGestureTimeout();
								state = STATE_BRIGHTNESS;
								brightness_type = BRIGHTNESS_TYPE_WARM;
								startBrightnessControl(start_x, start_y, brightness_type);
								return true;
							}
						}
						int dir = isPageMode()
								? x - start_x : y - start_y;
						int pageFlipsPerFullSwipe =
								inputSettings
										.pageFlipsPerFullSwipe();
						if (pageFlipsPerFullSwipe == 1) {
							ReaderPageAnimationState.Snapshot
									animationSettings =
											pageAnimationState
													.snapshot();
							if (!animationSettings.isEnabled()
									|| DeviceInfo.EINK_SCREEN) {
								// no animation
								return performAction(dir < 0 ? ReaderAction.PAGE_DOWN : ReaderAction.PAGE_UP, false);
							}
							startAnimation(
									start_x, start_y,
									width, height, x, y,
									animationSettings);
							updateAnimation(x, y);
							cancelTapGestureTimeout();
							state = STATE_FLIPPING;
						}
						if (pageFlipsPerFullSwipe > 1) {
							cancelTapGestureTimeout();
							state = STATE_FLIP_TRACKING;
							updatePageFlipTracking(start_x, start_y);
						}
						return true;
					case STATE_FLIPPING:
						updateAnimation(x, y);
						return true;
					case STATE_BRIGHTNESS:
						updateBrightnessControl(x, y, brightness_type);
						return true;
					case STATE_FLIP_TRACKING:
						updatePageFlipTracking(x, y);
						return true;
					case STATE_WAIT_FOR_DOUBLE_CLICK:
						return true;
					case STATE_SELECTION:
						updateSelection(start_x, start_y, x, y, false);
						break;
				}

			} else if (event.getAction() == MotionEvent.ACTION_OUTSIDE
					|| event.getAction()
							== MotionEvent.ACTION_CANCEL) {
				return unexpectedEvent();
			}
			return true;
		}
	}


	public void showTOC() {
		BackgroundThread.ensureGUI();
		final BookInfo expectedBook = mBookInfo;
		final DocumentLoadLifecycle.Interaction interaction =
				documentLoadLifecycle.interaction();
		if (!isDocumentInteractionCurrent(
				expectedBook, interaction))
			return;
		mEngine.post(new Task() {
			TOCItem toc;
			PositionProperties pos;

			public void work() {
				BackgroundThread.ensureBackground();
				if (!isDocumentInteractionCurrent(
						expectedBook, interaction))
					return;
				toc = doc.getTOC();
				pos = doc.getPositionProps(null, false);
			}

			public void done() {
				BackgroundThread.ensureGUI();
				if (!isDocumentInteractionCurrent(
						expectedBook, interaction))
					return;
				if (toc != null && pos != null) {
					TOCDlg dlg = new TOCDlg(
							mActivity, toc, pos.pageNumber,
							pageNumber -> goToPage(
									pageNumber,
									expectedBook,
									interaction));
					dlg.show();
				} else {
					mActivity.showToast("No Table of Contents found");
				}
			}
		});
	}

	public void showSearchDialog(String initialText) {
		showSearchDialog(
				initialText, mBookInfo,
				documentLoadLifecycle.interaction());
	}

	void showSearchDialog(
			String initialText, BookInfo expectedBook,
			DocumentLoadLifecycle.Interaction interaction) {
		BackgroundThread.ensureGUI();
		if (!isDocumentInteractionCurrent(
				expectedBook, interaction))
			return;
		if (initialText != null && initialText.length() > 40)
			initialText = initialText.substring(0, 40);
		SearchDlg dlg = new SearchDlg(
				mActivity, expectedBook, initialText,
				new SearchDlg.SearchHandler() {
					@Override
					public boolean isActive() {
						return isDocumentInteractionCurrent(
								expectedBook, interaction);
					}

					@Override
					public void find(
							String pattern, boolean reverse,
							boolean caseInsensitive) {
						findText(
								pattern, reverse, caseInsensitive,
								expectedBook, interaction);
					}
				});
		dlg.show();
	}

	public void findText(final String pattern, final boolean reverse, final boolean caseInsensitive) {
		findText(
				pattern, reverse, caseInsensitive,
				mBookInfo, documentLoadLifecycle.interaction());
	}

	private void findText(
			final String pattern, final boolean reverse,
			final boolean caseInsensitive,
			final BookInfo expectedBook,
			final DocumentLoadLifecycle.Interaction interaction) {
		BackgroundThread.ensureGUI();
		if (!isDocumentInteractionCurrent(
				expectedBook, interaction))
			return;
		mEngine.execute(new Task() {
			private boolean found;

			public void work() throws Exception {
				BackgroundThread.ensureBackground();
				if (!isDocumentInteractionCurrent(
						expectedBook, interaction))
					return;
				found = doc.findText(
						pattern, 1, reverse ? 1 : 0,
						caseInsensitive ? 1 : 0);
				if (!found && isDocumentInteractionCurrent(
						expectedBook, interaction))
					found = doc.findText(
							pattern, -1, reverse ? 1 : 0,
							caseInsensitive ? 1 : 0);
				if (!found && isDocumentInteractionCurrent(
						expectedBook, interaction)) {
					doc.clearSelection();
				}
			}

			public void done() {
				BackgroundThread.ensureGUI();
				if (!isDocumentInteractionCurrent(
						expectedBook, interaction))
					return;
				if (!found) {
					mActivity.showToast("Pattern not found");
					return;
				}
				drawPage();
				FindNextDlg.showDialog(
						mActivity, surface,
						new FindNextDlg.SearchNavigationHandler() {
							@Override
							public boolean isActive() {
								return isDocumentInteractionCurrent(
										expectedBook, interaction);
							}

							@Override
							public void findNext(boolean reverse) {
								ReaderView.this.findNext(
										pattern, reverse,
										caseInsensitive,
										expectedBook, interaction);
							}

							@Override
							public void clearSelection() {
								ReaderView.this.clearSelection(
										expectedBook, interaction);
							}
						});
			}

			public void fail(Exception e) {
				BackgroundThread.ensureGUI();
				if (isDocumentInteractionCurrent(
						expectedBook, interaction))
					mActivity.showToast("Pattern not found");
			}

		});
	}

	public void findNext(final String pattern, final boolean reverse, final boolean caseInsensitive) {
		findNext(
				pattern, reverse, caseInsensitive,
				mBookInfo, documentLoadLifecycle.interaction());
	}

	private void findNext(
			final String pattern, final boolean reverse,
			final boolean caseInsensitive,
			final BookInfo expectedBook,
			final DocumentLoadLifecycle.Interaction interaction) {
		BackgroundThread.ensureGUI();
		if (!isDocumentInteractionCurrent(
				expectedBook, interaction))
			return;
		mEngine.execute(new Task() {
			private boolean found;

			public void work() throws Exception {
				BackgroundThread.ensureBackground();
				if (!isDocumentInteractionCurrent(
						expectedBook, interaction))
					return;
				found = doc.findText(
						pattern, 1, reverse ? 1 : 0,
						caseInsensitive ? 1 : 0);
				if (!found && isDocumentInteractionCurrent(
						expectedBook, interaction))
					found = doc.findText(
							pattern, -1, reverse ? 1 : 0,
							caseInsensitive ? 1 : 0);
				if (!found && isDocumentInteractionCurrent(
						expectedBook, interaction)) {
					doc.clearSelection();
				}
			}

			public void done() {
				BackgroundThread.ensureGUI();
				if (!found
						|| !isDocumentInteractionCurrent(
								expectedBook, interaction))
					return;
//				drawPage();
				drawPage(true);
			}
		});
	}

	public void clearSelection() {
		clearSelection(
				mBookInfo, documentLoadLifecycle.interaction());
	}

	void clearSelection(
			final BookInfo expectedBook,
			final DocumentLoadLifecycle.Interaction interaction) {
		BackgroundThread.ensureGUI();
		if (!isDocumentInteractionCurrent(
				expectedBook, interaction))
			return;
		cancelSelectionUpdates();
		mEngine.post(new Task() {
			public void work() throws Exception {
				if (!isDocumentInteractionCurrent(
						expectedBook, interaction))
					return;
				doc.clearSelection();
				pageInvalidationState.invalidate();
			}

			public void done() {
				if (isDocumentInteractionCurrent(
						expectedBook, interaction)
						&& surface.isShown())
					drawPage(true);
			}
		});
	}

	public void highlightBookmarks() {
		highlightBookmarks(
				mBookInfo, documentLoadLifecycle.interaction());
	}

	private void highlightBookmarks(
			final BookInfo expectedBook,
			final DocumentLoadLifecycle.Interaction interaction) {
		BackgroundThread.ensureGUI();
		if (!isDocumentInteractionCurrent(
				expectedBook, interaction))
			return;
		int count = expectedBook.getBookmarkCount();
		boolean highlightEnabled =
				!"0".equals(
						readerSettingsState.getProperty(
								PROP_APP_HIGHLIGHT_BOOKMARKS,
								"0"));
		final Bookmark[] list =
				count > 0 && highlightEnabled
						? new Bookmark[count]
						: null;
		for (int i = 0; i < count && highlightEnabled; i++)
			list[i] = expectedBook.getBookmark(i);
		mEngine.post(new Task() {
			public void work() throws Exception {
				if (!isDocumentInteractionCurrent(
						expectedBook, interaction))
					return;
				doc.hilightBookmarks(list);
				pageInvalidationState.invalidate();
			}

			public void done() {
				if (isDocumentInteractionCurrent(
						expectedBook, interaction)
						&& surface.isShown())
					drawPage(true);
			}
		});
	}

	public void goToBookmark(Bookmark bm) {
		BackgroundThread.ensureGUI();
		final BookInfo expectedBook = mBookInfo;
		final DocumentLoadLifecycle.Interaction interaction =
				documentLoadLifecycle.interaction();
		goToBookmark(bm, expectedBook, interaction);
	}

	private boolean goToBookmark(
			final Bookmark bookmark,
			final BookInfo expectedBook,
			final DocumentLoadLifecycle.Interaction interaction) {
		BackgroundThread.ensureGUI();
		if (bookmark == null || !isDocumentInteractionCurrent(
				expectedBook, interaction))
			return false;
		final String pos = bookmark.getStartPos();
		mEngine.execute(new Task() {
			public void work() {
				BackgroundThread.ensureBackground();
				if (!isDocumentInteractionCurrent(
						expectedBook, interaction))
					return;
				doc.goToPosition(pos, true);
				updateCurrentPositionStatus(
						expectedBook, interaction);
			}

			public void done() {
				BackgroundThread.ensureGUI();
				if (!isDocumentInteractionCurrent(
						expectedBook, interaction))
					return;
				drawPage();
				scheduleSaveCurrentPositionBookmark(
						DEF_SAVE_POSITION_INTERVAL,
						expectedBook, interaction);
			}
		});
		return true;
	}

	public boolean goToBookmark(final int shortcut) {
		BackgroundThread.ensureGUI();
		final BookInfo expectedBook = mBookInfo;
		final DocumentLoadLifecycle.Interaction interaction =
				documentLoadLifecycle.interaction();
		if (!isDocumentInteractionCurrent(
				expectedBook, interaction))
			return false;
		Bookmark bm =
				expectedBook.findShortcutBookmark(shortcut);
		if (bm == null) {
			addBookmark(shortcut, expectedBook, interaction);
			return true;
		}
		goToBookmark(bm, expectedBook, interaction);
		return false;
	}

	public Bookmark removeBookmark(final Bookmark bookmark) {
		BackgroundThread.ensureGUI();
		return removeBookmark(
				bookmark, mBookInfo,
				documentLoadLifecycle.interaction());
	}

	private Bookmark removeBookmark(
			final Bookmark bookmark,
			final BookInfo expectedBook,
			final DocumentLoadLifecycle.Interaction interaction) {
		BackgroundThread.ensureGUI();
		if (bookmark == null || !isDocumentInteractionCurrent(
				expectedBook, interaction))
			return null;
		Bookmark removed =
				expectedBook.removeBookmark(bookmark);
		if (removed != null) {
			if (removed.getId() != null
					&& mActivity.getDB() != null) {
				mActivity.getDB().deleteBookmark(removed);
			}
			highlightBookmarks(expectedBook, interaction);
		}
		return removed;
	}

	public Bookmark updateBookmark(final Bookmark bookmark) {
		BackgroundThread.ensureGUI();
		return updateBookmark(
				bookmark, mBookInfo,
				documentLoadLifecycle.interaction());
	}

	private Bookmark updateBookmark(
			final Bookmark bookmark,
			final BookInfo expectedBook,
			final DocumentLoadLifecycle.Interaction interaction) {
		BackgroundThread.ensureGUI();
		if (bookmark == null || !isDocumentInteractionCurrent(
				expectedBook, interaction))
			return null;
		Bookmark bm = expectedBook.updateBookmark(bookmark);
		if (bm != null) {
			scheduleSaveCurrentPositionBookmark(
					DEF_SAVE_POSITION_INTERVAL,
					expectedBook, interaction);
			highlightBookmarks(expectedBook, interaction);
		}
		return bm;
	}

	public void addBookmark(final Bookmark bookmark) {
		BackgroundThread.ensureGUI();
		addBookmark(
				bookmark, mBookInfo,
				documentLoadLifecycle.interaction());
	}

	private boolean addBookmark(
			final Bookmark bookmark,
			final BookInfo expectedBook,
			final DocumentLoadLifecycle.Interaction interaction) {
		BackgroundThread.ensureGUI();
		if (bookmark == null || !isDocumentInteractionCurrent(
				expectedBook, interaction))
			return false;
		expectedBook.addBookmark(bookmark);
		highlightBookmarks(expectedBook, interaction);
		scheduleSaveCurrentPositionBookmark(
				DEF_SAVE_POSITION_INTERVAL,
				expectedBook, interaction);
		return true;
	}

	public void addBookmark(final int shortcut) {
		BackgroundThread.ensureGUI();
		addBookmark(
				shortcut, mBookInfo,
				documentLoadLifecycle.interaction());
	}

	private boolean addBookmark(
			final int shortcut,
			final BookInfo expectedBook,
			final DocumentLoadLifecycle.Interaction interaction) {
		BackgroundThread.ensureGUI();
		if (!isDocumentInteractionCurrent(
				expectedBook, interaction))
			return false;
		// set bookmark instead
		mEngine.execute(new Task() {
			Bookmark bm;

			public void work() {
				BackgroundThread.ensureBackground();
				if (isDocumentInteractionCurrent(
						expectedBook, interaction)) {
					Bookmark current =
							doc.getCurrentPageBookmark();
					if (current == null)
						return;
					bm = current;
					bm.setShortcut(shortcut);
				}
			}

			public void done() {
				if (bm != null
						&& isDocumentInteractionCurrent(
								expectedBook, interaction)) {
					if (shortcut == 0)
						expectedBook.addBookmark(bm);
					else
						expectedBook.setShortcutBookmark(
								shortcut, bm);
					if (mActivity.getDB() != null)
						mActivity.getDB().saveBookInfo(
								expectedBook);
					String s;
					if (shortcut == 0)
						s = mActivity.getString(R.string.toast_position_bookmark_is_set);
					else {
						s = mActivity.getString(R.string.toast_shortcut_bookmark_is_set);
						s.replace("$1", String.valueOf(shortcut));
					}
					highlightBookmarks(
							expectedBook, interaction);
					mActivity.showToast(s);
					scheduleSaveCurrentPositionBookmark(
							DEF_SAVE_POSITION_INTERVAL,
							expectedBook, interaction);
				}
			}
		});
		return true;
	}

	public void showBookmarksDialog() {
		BackgroundThread.ensureGUI();
		showBookmarksDialog(
				mBookInfo,
				documentLoadLifecycle.interaction());
	}

	void showBookmarksDialog(
			BookInfo expectedBook,
			DocumentLoadLifecycle.Interaction interaction) {
		BackgroundThread.ensureGUI();
		if (!isDocumentInteractionCurrent(
				expectedBook, interaction))
			return;
		BookmarksDlg dlg = new BookmarksDlg(
				mActivity, expectedBook,
				bookmarkInteractionHandler(
						expectedBook, interaction));
		dlg.show();
	}

	private BookmarkInteractionHandler bookmarkInteractionHandler(
			final BookInfo expectedBook,
			final DocumentLoadLifecycle.Interaction interaction) {
		return new BookmarkInteractionHandler() {
			@Override
			public boolean isActive() {
				return isDocumentInteractionCurrent(
						expectedBook, interaction);
			}

			@Override
			public boolean addBookmark(Bookmark bookmark) {
				return ReaderView.this.addBookmark(
						bookmark, expectedBook, interaction);
			}

			@Override
			public boolean addBookmark(int shortcut) {
				return ReaderView.this.addBookmark(
						shortcut, expectedBook, interaction);
			}

			@Override
			public boolean removeBookmark(Bookmark bookmark) {
				return ReaderView.this.removeBookmark(
						bookmark, expectedBook, interaction)
						!= null;
			}

			@Override
			public boolean updateBookmark(Bookmark bookmark) {
				return ReaderView.this.updateBookmark(
						bookmark, expectedBook, interaction)
						!= null;
			}

			@Override
			public boolean goToBookmark(Bookmark bookmark) {
				return ReaderView.this.goToBookmark(
						bookmark, expectedBook, interaction);
			}

			@Override
			public boolean goToBookmark(int shortcut) {
				if (!isActive())
					return false;
				Bookmark bookmark =
						expectedBook.findShortcutBookmark(
								shortcut);
				if (bookmark == null)
					return ReaderView.this.addBookmark(
							shortcut, expectedBook,
							interaction);
				return ReaderView.this.goToBookmark(
						bookmark, expectedBook, interaction);
			}
		};
	}

	public boolean onMenuItem(final int itemId) {
		BackgroundThread.ensureGUI();
		ReaderAction action = ReaderAction.findByMenuId(itemId);
		if (action.isNone())
			return false;
		onAction(action);
		return true;
	}

	public void onAction(final ReaderAction action) {
		onAction(action, null);
	}

	public void onAction(final ReaderAction action, final Runnable onFinishHandler) {
		BackgroundThread.ensureGUI();
		if (action.cmd != ReaderCommand.DCMD_NONE)
			onCommand(action.cmd, action.param, onFinishHandler);
	}

	public void toggleDayNightMode() {
		Properties settings = getSettings();
		OptionsDialog.toggleDayNightMode(settings);
		//setSettings(settings, mActivity.settings());
		mActivity.setSettings(settings, 60000, true);
		pageInvalidationState.invalidate();
	}

	public boolean isNightMode() {
		return readerSettingsState.getBool(
				PROP_NIGHT_MODE, false);
	}

	public String getSetting(String name) {
		return readerSettingsState.getProperty(name);
	}

	public void setSetting(String name, String value, boolean invalidateImages, boolean save, boolean apply) {
		mActivity.setSetting(name, value, apply);
		pageInvalidationState.invalidate();
	}

	public void setSetting(String name, String value) {
		setSetting(name, value, true, false, true);
	}

	private ReaderViewModeState.Lease acquireTemporaryScrollMode() {
		ReaderViewModeState.Acquisition acquisition =
				readerViewModeState.acquireScrollMode();
		if (acquisition == null)
			return null;
		postViewModeTransition(acquisition.transition());
		return acquisition.lease();
	}

	private void releaseTemporaryScrollMode(
			ReaderViewModeState.Lease lease) {
		postViewModeTransition(readerViewModeState.release(lease));
	}

	private void resetTemporaryViewMode() {
		postViewModeTransition(readerViewModeState.reset());
	}

	private void postViewModeTransition(
			final ReaderViewModeState.Transition transition) {
		if (transition == null)
			return;
		post(new Task() {
			@Override
			public void work() {
				BackgroundThread.ensureBackground();
				if (!readerNativeLifecycle.isInitialized())
					return;
				log.v("Switching temporary reader mode to "
						+ (transition.isPageMode()
								? "pages" : "scroll"));
				doc.doCommand(
						ReaderCommand
								.DCMD_TOGGLE_PAGE_SCROLL_VIEW.nativeId,
						0);
			}
		});
	}

	private boolean isPageMode() {
		return readerViewModeState.isPageMode();
	}

	public void saveSetting(String name, String value) {
		setSetting(name, value, true, true, true);
	}

	public void toggleScreenOrientation() {
		int orientation = mActivity.getScreenOrientation();
		orientation = (orientation == 0) ? 1 : 0;
		saveSetting(PROP_APP_SCREEN_ORIENTATION, String.valueOf(orientation));
		mActivity.setScreenOrientation(orientation);
	}

	public void toggleFullscreen() {
		boolean newBool = !mActivity.isFullscreen();
		String newValue = newBool ? "1" : "0";
		saveSetting(PROP_APP_FULLSCREEN, newValue);
		mActivity.setFullscreen(newBool);
	}

	public void showReadingPositionPopup() {
		if (mBookInfo == null)
			return;
		final BookInfo expectedBook = mBookInfo;
		final DocumentLoadLifecycle.Interaction interaction =
				documentLoadLifecycle.interaction();
		if (!isDocumentInteractionCurrent(
				expectedBook, interaction))
			return;
		final StringBuilder buf = new StringBuilder();
//		if (mActivity.isFullscreen()) {
		buf.append(Utils.formatTime(mActivity, System.currentTimeMillis()) + " ");
		buf.append(" [" + batteryStatus.getChargeLevel() + "%]\n");
//		}
		execute(new Task() {
			Bookmark bm;

			@Override
			public void work() {
				if (!isDocumentInteractionCurrent(
						expectedBook, interaction))
					return;
				bm = doc.getCurrentPageBookmark();
				if (bm != null) {
					PositionProperties prop = doc.getPositionProps(bm.getStartPos(), true);
					if (prop.pageMode != 0) {
						buf.append(""
								+ DocumentPositionPolicy.displayPageNumber(
										prop.pageNumber,
										prop.pageCount)
								+ " / "
								+ prop.pageCount
								+ "   ");
					}
					buf.append(DocumentPositionPolicy.formatPercent(
							prop.getPercent()));

					// Show chapter details if book has more than one chapter
					TOCItem toc = doc.getTOC();
					if (toc != null && toc.getChildCount() > 1) {
						TOCItem chapter = toc.getChapterAtPage(prop.pageNumber);

						String chapterName = chapter.getName();
						if (chapterName != null && chapterName.length() > 30)
							chapterName = chapterName.substring(0, 30) + "...";

						TOCItem nextChapter = chapter.getNextChapter();
						int iChapterEnd = (nextChapter != null) ? nextChapter.getPage() : prop.pageCount;

						String chapterPos = null;
						if (prop.pageMode != 0) {
							int iChapterStart = chapter.getPage();
							int iChapterLen = iChapterEnd - iChapterStart;
							int iChapterPage = prop.pageNumber - iChapterStart + 1;

							chapterPos = "  (" + iChapterPage + " / " + iChapterLen + ")";
						}

						if (chapterName != null && chapterName.length() > 0)
							buf.append("\n" + chapterName);
						if (chapterPos != null && chapterPos.length() > 0)
							buf.append(chapterPos);
					}
				}
			}

			public void done() {
				if (!isDocumentInteractionCurrent(
						expectedBook, interaction))
					return;
				mActivity.showToast(buf.toString());
			}
		});
	}

	public void toggleTitlebar() {
		boolean newBool = "1".equals(getSetting(PROP_STATUS_LINE));
		String newValue = !newBool ? "1" : "0";
		mActivity.setSetting(PROP_STATUS_LINE, newValue, true);
	}

	private final CloseableTaskGate readerOptionsDialogLifecycle =
			new CloseableTaskGate();

	public void showOptionsDialog() {
		BackgroundThread.ensureGUI();
		final BookInfo expectedBook = mBookInfo;
		final DocumentLoadLifecycle.Interaction interaction =
				documentLoadLifecycle.interaction();
		if (!isDocumentInteractionCurrent(
				expectedBook, interaction))
			return;
		final ReaderOptionsHandler optionsHandler =
				readerOptionsHandler(
						expectedBook, interaction);
		final CloseableTaskGate.Token owner =
				readerOptionsDialogLifecycle.replace();
		if (owner == null)
			return;
		BackgroundThread.instance().postBackground(() -> {
			BackgroundThread.ensureBackground();
			if (!readerOptionsDialogLifecycle.isActive(owner)
					|| !optionsHandler.isActive()) {
				readerOptionsDialogLifecycle.complete(owner);
				return;
			}
			final String[] fontSnapshot;
			try {
				String[] fontFaces = Engine.getFontFaceList();
				fontSnapshot =
						fontFaces != null
								? fontFaces.clone()
								: null;
			} catch (Exception e) {
				if (readerOptionsDialogLifecycle.complete(owner)
						&& optionsHandler.isActive())
					log.e("Cannot load reader font catalog", e);
				return;
			}
			if (!readerOptionsDialogLifecycle.isActive(owner)
					|| !optionsHandler.isActive()) {
				readerOptionsDialogLifecycle.complete(owner);
				return;
			}
			BackgroundThread.instance().executeGUI(() -> {
				if (!readerOptionsDialogLifecycle.complete(owner)
						|| !optionsHandler.isActive())
					return;
				OptionsDialog dlg = new OptionsDialog(
						mActivity,
						mEngine,
						OptionsDialog.Mode.READER,
						optionsHandler,
						fontSnapshot,
						null);
				dlg.show();
			});
		});
	}

	private ReaderOptionsHandler readerOptionsHandler(
			final BookInfo expectedBook,
			final DocumentLoadLifecycle.Interaction interaction) {
		return new ReaderOptionsHandler() {
			@Override
			public boolean isActive() {
				return isDocumentInteractionCurrent(
						expectedBook, interaction);
			}

			@Override
			public ReaderDocumentOptions snapshot() {
				if (!isActive())
					return null;
				return ReaderDocumentOptions.capture(
						expectedBook);
			}

			@Override
			public boolean applyDocumentOptions(
					boolean textAutoformatEnabled,
					boolean documentStylesEnabled,
					boolean documentFontsEnabled,
					int domVersion,
					int blockRenderingFlags) {
				return applyReaderDocumentOptions(
						textAutoformatEnabled,
						documentStylesEnabled,
						documentFontsEnabled,
						domVersion,
						blockRenderingFlags,
						expectedBook,
						interaction);
			}
		};
	}

	private boolean applyReaderDocumentOptions(
			boolean textAutoformatEnabled,
			boolean documentStylesEnabled,
			boolean documentFontsEnabled,
			int domVersion,
			int blockRenderingFlags,
			BookInfo expectedBook,
			DocumentLoadLifecycle.Interaction interaction) {
		BackgroundThread.ensureGUI();
		if (!isDocumentInteractionCurrent(
				expectedBook, interaction)
				|| expectedBook.getFileInfo() == null)
			return false;
		FileInfo fileInfo = expectedBook.getFileInfo();
		boolean textAutoformatChanged =
				textAutoformatEnabled
						== fileInfo.getFlag(
								FileInfo.DONT_REFLOW_TXT_FILES_FLAG);
		boolean documentStylesChanged =
				documentStylesEnabled
						== fileInfo.getFlag(
								FileInfo.DONT_USE_DOCUMENT_STYLES_FLAG);
		boolean documentFontsChanged =
				documentFontsEnabled
						!= fileInfo.getFlag(
								FileInfo.USE_DOCUMENT_FONTS_FLAG);
		boolean domVersionChanged =
				domVersion != fileInfo.domVersion;
		boolean blockRenderingChanged =
				blockRenderingFlags
						!= fileInfo.blockRenderingFlags;
		if (!textAutoformatChanged
				&& !documentStylesChanged
				&& !documentFontsChanged
				&& !domVersionChanged
				&& !blockRenderingChanged)
			return true;
		if (textAutoformatChanged)
			fileInfo.setFlag(
					FileInfo.DONT_REFLOW_TXT_FILES_FLAG,
					!textAutoformatEnabled);
		if (documentStylesChanged)
			fileInfo.setFlag(
					FileInfo.DONT_USE_DOCUMENT_STYLES_FLAG,
					!documentStylesEnabled);
		if (documentFontsChanged)
			fileInfo.setFlag(
					FileInfo.USE_DOCUMENT_FONTS_FLAG,
					documentFontsEnabled);
		if (domVersionChanged)
			fileInfo.domVersion = domVersion;
		if (blockRenderingChanged)
			fileInfo.blockRenderingFlags =
					blockRenderingFlags;
		if (mActivity.getDB() != null)
			mActivity.getDB().saveBookInfo(expectedBook);
		if (textAutoformatChanged
				|| domVersionChanged
				|| blockRenderingChanged) {
			if (isBookLoaded())
				reloadDocument();
		} else if (isBookLoaded()) {
			applyReaderRenderingOptions(
					documentStylesChanged,
					documentStylesEnabled,
					documentFontsChanged,
					documentFontsEnabled,
					expectedBook,
					interaction);
		}
		return true;
	}

	private void applyReaderRenderingOptions(
			final boolean documentStylesChanged,
			final boolean documentStylesEnabled,
			final boolean documentFontsChanged,
			final boolean documentFontsEnabled,
			final BookInfo expectedBook,
			final DocumentLoadLifecycle.Interaction interaction) {
		mEngine.execute(new Task() {
			private boolean rendered;

			@Override
			public void work() {
				if (!isDocumentInteractionCurrent(
						expectedBook, interaction))
					return;
				if (documentStylesChanged)
					doc.doCommand(
							ReaderCommand.DCMD_SET_INTERNAL_STYLES
									.nativeId,
							documentStylesEnabled ? 1 : 0);
				if (!isDocumentInteractionCurrent(
						expectedBook, interaction))
					return;
				if (documentFontsChanged)
					doc.doCommand(
							ReaderCommand.DCMD_SET_DOC_FONTS
									.nativeId,
							documentFontsEnabled ? 1 : 0);
				if (!isDocumentInteractionCurrent(
						expectedBook, interaction))
					return;
				rendered = doc.doCommand(
						ReaderCommand.DCMD_REQUEST_RENDER.nativeId,
						1);
			}

			@Override
			public void done() {
				if (!rendered
						|| !isDocumentInteractionCurrent(
								expectedBook, interaction))
					return;
				pageInvalidationState.invalidate();
				drawPage();
			}
		});
	}

	public void toggleDocumentStyles() {
		BackgroundThread.ensureGUI();
		BookInfo expectedBook = mBookInfo;
		DocumentLoadLifecycle.Interaction interaction =
				documentLoadLifecycle.interaction();
		if (!isBookLoaded()
				|| !isDocumentInteractionCurrent(
						expectedBook, interaction))
			return;
		ReaderDocumentOptions options =
				ReaderDocumentOptions.capture(expectedBook);
		if (options == null)
			return;
		log.d("toggleDocumentStyles()");
		applyReaderDocumentOptions(
				options.isTextAutoformatEnabled(),
				!options.isDocumentStylesEnabled(),
				options.isDocumentFontsEnabled(),
				options.getDomVersion(),
				options.getBlockRenderingFlags(),
				expectedBook, interaction);
	}

	public void toggleEmbeddedFonts() {
		BackgroundThread.ensureGUI();
		BookInfo expectedBook = mBookInfo;
		DocumentLoadLifecycle.Interaction interaction =
				documentLoadLifecycle.interaction();
		if (!isBookLoaded()
				|| !isDocumentInteractionCurrent(
						expectedBook, interaction))
			return;
		ReaderDocumentOptions options =
				ReaderDocumentOptions.capture(expectedBook);
		if (options == null)
			return;
		log.d("toggleEmbeddedFonts()");
		applyReaderDocumentOptions(
				options.isTextAutoformatEnabled(),
				options.isDocumentStylesEnabled(),
				!options.isDocumentFontsEnabled(),
				options.getDomVersion(),
				options.getBlockRenderingFlags(),
				expectedBook, interaction);
	}

	public boolean isTextAutoformatEnabled() {
		if (mOpened && mBookInfo != null) {
			boolean disableTextReflow = mBookInfo.getFileInfo().getFlag(FileInfo.DONT_REFLOW_TXT_FILES_FLAG);
			return !disableTextReflow;
		}
		return true;
	}

	public boolean isTextFormat() {
		if (mOpened && mBookInfo != null) {
			DocumentFormat fmt = mBookInfo.getFileInfo().format;
			return fmt == DocumentFormat.TXT || fmt == DocumentFormat.HTML || fmt == DocumentFormat.PDB;
		}
		return false;
	}

	public boolean isFormatWithEmbeddedFonts() {
		if (mOpened && mBookInfo != null) {
			DocumentFormat fmt = mBookInfo.getFileInfo().format;
			return fmt == DocumentFormat.EPUB;
		}
		return false;
	}

	public boolean isFormatWithEmbeddedStyles() {
		if (mOpened && mBookInfo != null) {
			DocumentFormat fmt = mBookInfo.getFileInfo().format;
			return fmt == DocumentFormat.EPUB || fmt == DocumentFormat.HTML || fmt == DocumentFormat.CHM || fmt == DocumentFormat.FB2 || fmt == DocumentFormat.FB3;
		}
		return false;
	}

	public boolean isHtmlFormat() {
		if (mOpened && mBookInfo != null) {
			DocumentFormat fmt = mBookInfo.getFileInfo().format;
			return fmt == DocumentFormat.EPUB || fmt == DocumentFormat.HTML || fmt == DocumentFormat.PDB || fmt == DocumentFormat.CHM;
		}
		return false;
	}

	public int getDOMVersion() {
		if (mOpened && mBookInfo != null) {
			return mBookInfo.getFileInfo().domVersion;
		}
		return Engine.DOM_VERSION_CURRENT;
	}

	public void setDOMVersion(int version) {
		BackgroundThread.ensureGUI();
		BookInfo expectedBook = mBookInfo;
		DocumentLoadLifecycle.Interaction interaction =
				documentLoadLifecycle.interaction();
		if (!isDocumentInteractionCurrent(
				expectedBook, interaction))
			return;
		ReaderDocumentOptions options =
				ReaderDocumentOptions.capture(expectedBook);
		if (options == null)
			return;
		applyReaderDocumentOptions(
				options.isTextAutoformatEnabled(),
				options.isDocumentStylesEnabled(),
				options.isDocumentFontsEnabled(),
				version,
				options.getBlockRenderingFlags(),
				expectedBook, interaction);
	}

	public int getBlockRenderingFlags() {
		if (mOpened && mBookInfo != null) {
			return mBookInfo.getFileInfo().blockRenderingFlags;
		}
		return 0;
	}

	public void setBlockRenderingFlags(int flags) {
		BackgroundThread.ensureGUI();
		BookInfo expectedBook = mBookInfo;
		DocumentLoadLifecycle.Interaction interaction =
				documentLoadLifecycle.interaction();
		if (!isDocumentInteractionCurrent(
				expectedBook, interaction))
			return;
		ReaderDocumentOptions options =
				ReaderDocumentOptions.capture(expectedBook);
		if (options == null)
			return;
		applyReaderDocumentOptions(
				options.isTextAutoformatEnabled(),
				options.isDocumentStylesEnabled(),
				options.isDocumentFontsEnabled(),
				options.getDomVersion(),
				flags,
				expectedBook, interaction);
	}

	public void toggleTextFormat() {
		BackgroundThread.ensureGUI();
		BookInfo expectedBook = mBookInfo;
		DocumentLoadLifecycle.Interaction interaction =
				documentLoadLifecycle.interaction();
		if (!isBookLoaded()
				|| !isDocumentInteractionCurrent(
						expectedBook, interaction))
			return;
		ReaderDocumentOptions options =
				ReaderDocumentOptions.capture(expectedBook);
		if (options == null || !options.isTextFormat())
			return;
		log.d("toggleTextFormat()");
		applyReaderDocumentOptions(
				!options.isTextAutoformatEnabled(),
				options.isDocumentStylesEnabled(),
				options.isDocumentFontsEnabled(),
				options.getDomVersion(),
				options.getBlockRenderingFlags(),
				expectedBook, interaction);
	}

	public boolean getDocumentStylesEnabled() {
		if (mOpened && mBookInfo != null) {
			boolean flg = !mBookInfo.getFileInfo().getFlag(FileInfo.DONT_USE_DOCUMENT_STYLES_FLAG);
			return flg;
		}
		return true;
	}

	public boolean getDocumentFontsEnabled() {
		if (mOpened && mBookInfo != null) {
			boolean flg = mBookInfo.getFileInfo().getFlag(FileInfo.USE_DOCUMENT_FONTS_FLAG);
			return flg;
		}
		return true;
	}

	private final CloseableTaskGate bookInfoDialogLifecycle =
			new CloseableTaskGate();

	public void showBookInfo() {
		BackgroundThread.ensureGUI();
		final BookInfo expectedBook = mBookInfo;
		final DocumentLoadLifecycle.Interaction interaction =
				documentLoadLifecycle.interaction();
		if (!isDocumentInteractionCurrent(
				expectedBook, interaction))
			return;
		final ReaderBookInfoSnapshot snapshot =
				ReaderBookInfoSnapshot.capture(
						mActivity.getVersion(),
						batteryStatus.getChargeLevel(),
						Utils.formatTime(
								mActivity,
								System.currentTimeMillis()),
						expectedBook);
		if (snapshot == null)
			return;
		final CloseableTaskGate.Token owner =
				bookInfoDialogLifecycle.replace();
		if (owner == null)
			return;
		execute(new Task() {
			List<String> items;

			@Override
			public void work() {
				BackgroundThread.ensureBackground();
				if (!bookInfoDialogLifecycle.isActive(owner)
						|| !isDocumentInteractionCurrent(
								expectedBook, interaction))
					return;
				Bookmark bookmark =
						doc.getCurrentPageBookmark();
				PositionProperties position =
						bookmark != null
								? doc.getPositionProps(
										bookmark.getStartPos(),
										true)
								: null;
				items = snapshot.buildItems(
						bookmark, position);
			}

			public void done() {
				BackgroundThread.ensureGUI();
				if (!bookInfoDialogLifecycle.complete(owner)
						|| !isDocumentInteractionCurrent(
								expectedBook, interaction)
						|| items == null)
					return;
				BookInfoDialog dlg = new BookInfoDialog(
						mActivity, mGenresCollection, items);
				dlg.show();
			}

			@Override
			public void fail(Exception e) {
				if (!bookInfoDialogLifecycle.complete(owner)
						|| !isDocumentInteractionCurrent(
								expectedBook, interaction))
					return;
				super.fail(e);
			}
		});
	}

	private final AutoScrollSpeedState autoScrollSpeedState =
			new AutoScrollSpeedState();
	private final AutoScrollSessionState<AutoScrollAnimation>
			autoScrollSessions = new AutoScrollSessionState<>();
	private final DelayedExecutor autoScrollScheduler =
			DelayedExecutor.createGUI("autoscroll");

	private boolean isAutoScrollActive() {
		return autoScrollSessions.isActive();
	}

	private void stopAutoScroll() {
		AutoScrollAnimation stopped;
		boolean initialized;
		synchronized (autoScrollSessions) {
			stopped = autoScrollSessions.currentSession();
			initialized =
					autoScrollSessions.isInitialized(stopped);
			stopped = autoScrollSessions.stopCurrent();
			if (stopped != null)
				autoScrollScheduler.cancel();
		}
		if (stopped == null)
			return;
		log.d("stopAutoScroll()");
		if (initialized)
			stopped.finishStop();
		else
			stopped.finishAbortedStart();
	}

	public static final int AUTOSCROLL_START_ANIMATION_PERCENT = 5;

	private void startAutoScroll() {
		if (isAutoScrollActive())
			return;
		final BookInfo expectedBook = mBookInfo;
		final DocumentLoadLifecycle.Interaction interaction =
				documentLoadLifecycle.interaction();
		if (!isBookLoaded()
				|| !isDocumentInteractionCurrent(
				expectedBook, interaction))
			return;
		log.d("startAutoScroll()");
		AutoScrollAnimation animation =
				new AutoScrollAnimation(
						AUTOSCROLL_START_ANIMATION_PERCENT * 100,
						expectedBook, interaction);
		if (!autoScrollSessions.requestStart(animation))
			return;
		animation.start();
		invalidateTapHighlight();
	}

	private void toggleAutoScroll() {
		if (isAutoScrollActive())
			stopAutoScroll();
		else
			startAutoScroll();
	}

	private void changeAutoScrollSpeed(int delta) {
		int speed = autoScrollSpeedState.change(delta);
		setSetting(
				PROP_APP_VIEW_AUTOSCROLL_SPEED,
				String.valueOf(speed),
				false, true, false);
	}

	class AutoScrollAnimation {
		private final BookInfo expectedBook;
		private final DocumentLoadLifecycle.Interaction interaction;
		private final ReaderRenderRequest renderRequest;

		boolean isScrollView;
		BitmapInfo image1;
		BitmapInfo image2;
		PositionProperties currPos;
		int progress;
		int pageCount;
		int charCount;
		int timerInterval;
		long pageTurnStart;
		int nextPos;

		Paint[] shadePaints;
		Paint[] hilitePaints;

		final int startAnimationProgress;

		public static final int MAX_PROGRESS = 10000;
		public final static int ANIMATION_INTERVAL_NORMAL = 30;
		public final static int ANIMATION_INTERVAL_EINK = 5000;

		private AutoScrollAnimation(
				final int startProgress,
				BookInfo expectedBook,
				DocumentLoadLifecycle.Interaction interaction) {
			this.expectedBook = expectedBook;
			this.interaction = interaction;
			this.renderRequest =
					ReaderRenderRequest.fromInteraction(
							expectedBook, interaction);
			progress = startProgress;
			startAnimationProgress = AUTOSCROLL_START_ANIMATION_PERCENT * 100;

			final int numPaints = 32;
			shadePaints = new Paint[numPaints];
			hilitePaints = new Paint[numPaints];
			for (int i = 0; i < numPaints; i++) {
				shadePaints[i] = new Paint();
				hilitePaints[i] = new Paint();
				hilitePaints[i].setStyle(Paint.Style.FILL);
				shadePaints[i].setStyle(Paint.Style.FILL);
				if (mActivity.isNightMode()) {
					shadePaints[i].setColor(Color.argb((i + 1) * 128 / numPaints, 0, 0, 0));
					hilitePaints[i].setColor(Color.argb((i + 1) * 128 / numPaints, 128, 128, 128));
				} else {
					shadePaints[i].setColor(Color.argb((i + 1) * 128 / numPaints, 0, 0, 0));
					hilitePaints[i].setColor(Color.argb((i + 1) * 128 / numPaints, 255, 255, 255));
				}
			}
		}

		private boolean ownsDocument() {
			return isBookLoaded()
					&& isDocumentInteractionCurrent(
							expectedBook, interaction);
		}

		private boolean isCurrentSession() {
			return ownsDocument()
					&& autoScrollSessions.isCurrent(this);
		}

		private boolean isReadySession() {
			return ownsDocument()
					&& autoScrollSessions.isReady(this);
		}

		private void start() {
			BackgroundThread.instance().postBackground(() -> {
				if (!isCurrentSession()) {
					abandonFailedStart();
					return;
				}
				if (initPageTurn(progress)) {
					log.d("AutoScrollAnimation: starting autoscroll timer");
					timerInterval = DeviceInfo.EINK_SCREEN ? ANIMATION_INTERVAL_EINK : ANIMATION_INTERVAL_NORMAL;
					startTimer(timerInterval);
				} else {
					abandonFailedStart();
				}
			});
		}

		private void abandonFailedStart() {
			if (autoScrollSessions.stop(this))
				finishAbortedStart();
		}

		private int calcProgressPercent() {
			long duration = Utils.timeInterval(pageTurnStart);
			return AnimationTiming.autoscrollProgress(
					duration, charCount,
					autoScrollSpeedState.speed());
		}

		private boolean onTimer() {
			if (!isReadySession()) {
				if (!ownsDocument())
					abandonFailedStart();
				return false;
			}
			int newProgress = calcProgressPercent();
			alog.v("onTimer(progress = " + newProgress + ")");
			mActivity.onUserActivity();
			progress = newProgress;
			if (progress == 0 || progress >= startAnimationProgress) {
				if (image1 != null && image2 != null) {
					if (image1.isReleased() || image2.isReleased()) {
						log.d("Images lost! Recreating images...");
						if (!initPageTurn(progress)) {
							stop();
							return false;
						}
					}
					if (!isReadySession())
						return false;
					draw();
				}
			}
			if (progress >= 10000) {
				if (!donePageTurn(true)) {
					stop();
					return false;
				}
				if (!initPageTurn(0)) {
					stop();
					return false;
				}
			}
			return true;
		}

		class AutoscrollTimerTask implements Runnable {
			final long interval;

			public AutoscrollTimerTask(long interval) {
				this.interval = interval;
			}

			private boolean schedule() {
				synchronized (autoScrollSessions) {
					if (isCurrentSession()) {
						autoScrollScheduler.postDelayed(
								this, interval);
						return true;
					}
				}
				if (!ownsDocument())
					abandonFailedStart();
				return false;
			}

			@Override
			public void run() {
				if (!isCurrentSession()) {
					log.v("timer is cancelled - GUI");
					abandonFailedStart();
					return;
				}
				BackgroundThread.instance().postBackground(() -> {
					if (!isCurrentSession()) {
						log.v("timer is cancelled - BackgroundThread");
						abandonFailedStart();
						return;
					}
					if (onTimer())
						schedule();
					else
						log.v("timer is cancelled - onTimer returned false");
				});
			}
		}

		private void startTimer(final int interval) {
			if (new AutoscrollTimerTask(interval).schedule())
				mActivity.onUserActivity();
		}

		private boolean initPageTurn(int startProgress) {
			if (!isCurrentSession()
					|| !autoScrollSessions.beginInitialization(this)
					|| !ownsDocument())
				return false;
			cancelGc();
			log.v("initPageTurn(startProgress = " + startProgress + ")");
			pageTurnStart = Utils.timeStamp();
			progress = startProgress;
			PositionProperties nextPosition =
					doc.getPositionProps(null, true);
			if (nextPosition == null || !isCurrentSession())
				return false;
			currPos = nextPosition;
			charCount = currPos.charCount;
			pageCount = currPos.pageMode;
			if (charCount < 150)
				charCount = 150;
			isScrollView = currPos.pageMode == 0;
			log.v("initPageTurn(charCount = " + charCount + ")");
			if (isScrollView) {
				image1 = preparePageImage(
						0, renderRequest);
				if (image1 == null || !isCurrentSession()) {
					log.v("ScrollViewAnimation -- not started: image is null");
					return false;
				}
				int pos0 = image1.position.y;
				int pos1 = pos0 + image1.position.pageHeight * 9 / 10;
				if (pos1 > image1.position.fullHeight - image1.position.pageHeight)
					pos1 = image1.position.fullHeight - image1.position.pageHeight;
				if (pos1 < 0)
					pos1 = 0;
				nextPos = pos1;
				image2 = preparePageImage(
						pos1 - pos0, renderRequest);
				if (image2 == null || !isCurrentSession()) {
					log.v("ScrollViewAnimation -- not started: image is null");
					return false;
				}
			} else {
				int page1 = currPos.pageNumber;
				int page2 = currPos.pageNumber + 1;
				if (page2 < 0 || page2 >= currPos.pageCount) {
					animationState.reset();
					return false;
				}
				image1 = preparePageImage(
						0, renderRequest);
				if (!isCurrentSession())
					return false;
				image2 = preparePageImage(
						1, renderRequest);
				if (page1 == page2) {
					log.v("PageViewAnimation -- cannot start animation: not moved");
					return false;
				}
				if (image1 == null || image2 == null) {
					log.v("PageViewAnimation -- cannot start animation: page image is null");
					return false;
				}

			}
			long duration = android.os.SystemClock.uptimeMillis() - pageTurnStart;
			log.v("AutoScrollAnimation -- page turn initialized in " + duration + " millis");
			if (!ownsDocument()
					|| !autoScrollSessions.markReady(this)
					|| !ownsDocument())
				return false;
			draw();
			return true;
		}


		private boolean donePageTurn(boolean turnPage) {
			log.v("donePageTurn()");
			if (currPos == null || !ownsDocument())
				return false;
			if (turnPage) {
				boolean moved;
				if (isScrollView)
					moved = doc.doCommand(
							ReaderCommand.DCMD_GO_POS.nativeId,
							nextPos);
				else
					moved = doc.doCommand(
							ReaderCommand.DCMD_PAGEDOWN.nativeId,
							1);
				if (!moved) {
					progress = 0;
					return false;
				}
				if (!ownsDocument())
					return false;
				updateCurrentPositionStatus(
						expectedBook, interaction);
			}
			progress = 0;
			//draw();
			return currPos.canMoveToNextPage();
		}

		public void draw() {
			draw(true);
		}

		public void draw(boolean isPartially) {
			if (!isReadySession())
				return;
			//	long startTs = android.os.SystemClock.uptimeMillis();
			drawCallback(this::draw, null, isPartially);
		}

		public void stop() {
			boolean initialized;
			synchronized (autoScrollSessions) {
				initialized =
						autoScrollSessions.isInitialized(this);
				if (!autoScrollSessions.stop(this))
					return;
				autoScrollScheduler.cancel();
			}
			if (initialized)
				finishStop();
			else
				finishAbortedStart();
		}

		private void finishAbortedStart() {
			scheduleGc();
		}

		private void finishStop() {
			BackgroundThread.instance().executeBackground(() -> {
				if (!ownsDocument())
					return;
				donePageTurn(wantPageTurn());
				if (!ownsDocument())
					return;
				BackgroundThread.instance().executeGUI(() -> {
					if (!ownsDocument())
						return;
					//redraw();
					drawPage(null, false);
					scheduleSaveCurrentPositionBookmark(
							DEF_SAVE_POSITION_INTERVAL,
							expectedBook, interaction);
				});
			});
			scheduleGc();
		}

		private boolean wantPageTurn() {
			return (progress > (startAnimationProgress + MAX_PROGRESS) / 2);
		}

		private void drawGradient(Canvas canvas, Rect rc, Paint[] paints, int startIndex, int endIndex) {
			//log.v("drawShadow");
			int n = (startIndex < endIndex) ? endIndex - startIndex + 1 : startIndex - endIndex + 1;
			int dir = (startIndex < endIndex) ? 1 : -1;
			int dx = rc.bottom - rc.top;
			Rect rect = new Rect(rc);
			for (int i = 0; i < n; i++) {
				int index = startIndex + i * dir;
				int x1 = rc.top + dx * i / n;
				int x2 = rc.top + dx * (i + 1) / n;
				if (x1 < 0)
					x1 = 0;
				if (x2 > canvas.getHeight())
					x2 = canvas.getHeight();
				rect.top = x1;
				rect.bottom = x2;
				if (x2 > x1) {
					//log.v("drawShadow : " + x1 + ", " + x2 + ", " + index);
					canvas.drawRect(rect, paints[index]);
				}
			}
		}

		private void drawShadow(Canvas canvas, Rect rc) {
			drawGradient(canvas, rc, shadePaints, shadePaints.length * 3 / 4, 0);
		}

		void drawPageProgress(Canvas canvas, int scrollPercent, Rect dst, Rect src) {
			int shadowHeight = 32;
			int h = dst.height();
			int div = (h + shadowHeight) * scrollPercent / 10000 - shadowHeight;
			//log.v("drawPageProgress() div = " + div + ", percent = " + scrollPercent);
			int d = Math.max(div, 0);
			if (d > 0) {
				Rect src1 = new Rect(src.left, src.top, src.right, src.top + d);
				Rect dst1 = new Rect(dst.left, dst.top, dst.right, dst.top + d);
				drawDimmedBitmap(canvas, image2.bitmap, src1, dst1);
			}
			if (d < h) {
				Rect src2 = new Rect(src.left, src.top + d, src.right, src.bottom);
				Rect dst2 = new Rect(dst.left, dst.top + d, dst.right, dst.bottom);
				drawDimmedBitmap(canvas, image1.bitmap, src2, dst2);
			}
			if (scrollPercent > 0 && scrollPercent < 10000) {
				Rect shadowRect = new Rect(src.left, src.top + div, src.right, src.top + div + shadowHeight);
				drawShadow(canvas, shadowRect);
			}
		}

		public void draw(Canvas canvas) {
			if (!isReadySession())
				return;
			alog.v("AutoScrollAnimation.draw(" + progress + ")");
			if (progress != 0 && progress < startAnimationProgress)
				return; // don't draw page w/o started animation
			if (image1 == null
					|| image2 == null
					|| image1.isReleased()
					|| image2.isReleased())
				return;
			int scrollPercent = 10000 * (progress - startAnimationProgress) / (MAX_PROGRESS - startAnimationProgress);
			if (scrollPercent < 0)
				scrollPercent = 0;
			int w = image1.bitmap.getWidth();
			int h = image1.bitmap.getHeight();
			if (isScrollView) {
				// scroll
				drawPageProgress(canvas, scrollPercent, new Rect(0, 0, w, h), new Rect(0, 0, w, h));
			} else {
				if (pageCount == 2) {
					if (scrollPercent < 5000) {
						// < 50%
						scrollPercent = scrollPercent * 2;
						drawPageProgress(canvas, scrollPercent, new Rect(0, 0, w / 2, h), new Rect(0, 0, w / 2, h));
						drawPageProgress(canvas, 0, new Rect(w / 2, 0, w, h), new Rect(w / 2, 0, w, h));
					} else {
						// >=50%
						scrollPercent = (scrollPercent - 5000) * 2;
						drawPageProgress(canvas, 10000, new Rect(0, 0, w / 2, h), new Rect(0, 0, w / 2, h));
						drawPageProgress(canvas, scrollPercent, new Rect(w / 2, 0, w, h), new Rect(w / 2, 0, w, h));
					}
				} else {
					drawPageProgress(canvas, scrollPercent, new Rect(0, 0, w, h), new Rect(0, 0, w, h));
				}
			}
		}
	}

	public void onCommand(final ReaderCommand cmd, final int param) {
		onCommand(cmd, param, null);
	}

	private void navigateByHistory(final ReaderCommand cmd) {
		final BookInfo expectedBook = mBookInfo;
		final DocumentLoadLifecycle.Interaction interaction =
				documentLoadLifecycle.interaction();
		if (!isDocumentInteractionCurrent(
				expectedBook, interaction))
			return;
		BackgroundThread.instance().postBackground(() -> {
			if (!isDocumentInteractionCurrent(
					expectedBook, interaction))
				return;
			final boolean res = doc.doCommand(cmd.nativeId, 0);
			if (res)
				updateCurrentPositionStatus(
						expectedBook, interaction);
			BackgroundThread.instance().postGUI(() -> {
				if (!isDocumentInteractionCurrent(
						expectedBook, interaction))
					return;
				if (res) {
					// successful
					drawPage();
					scheduleSaveCurrentPositionBookmark(
							DEF_SAVE_POSITION_INTERVAL,
							expectedBook, interaction);
				} else {
					// cannot navigate - no data on stack
					if (cmd == ReaderCommand.DCMD_LINK_BACK) {
						// TODO: exit from activity in some cases?
						if (mActivity.isPreviousFrameHome())
							mActivity.showRootWindow();
						else
							mActivity.showBrowser(!mActivity.isBrowserCreated() ? getOpenedFileInfo() : null);
					}
				}
			});
		});
	}

	public void onCommand(final ReaderCommand cmd, final int param, final Runnable onFinishHandler) {
		BackgroundThread.ensureGUI();
		log.i("On command " + cmd + (param != 0 ? " (" + param + ")" : " "));
		switch (cmd) {
			case DCMD_FILE_BROWSER_ROOT:
				mActivity.showRootWindow();
				break;
			case DCMD_ABOUT:
				mActivity.showAboutDialog();
				break;
			case DCMD_SWITCH_PROFILE:
				showSwitchProfileDialog();
				break;
			case DCMD_TOGGLE_AUTOSCROLL:
				toggleAutoScroll();
				break;
			case DCMD_AUTOSCROLL_SPEED_INCREASE:
				changeAutoScrollSpeed(1);
				break;
			case DCMD_AUTOSCROLL_SPEED_DECREASE:
				changeAutoScrollSpeed(-1);
				break;
			case DCMD_SHOW_DICTIONARY:
				mActivity.showDictionary();
				break;
			case DCMD_OPEN_PREVIOUS_BOOK:
				mActivity.loadPreviousDocument(() -> {
					// do nothing
				});
				break;
			case DCMD_BOOK_INFO:
				if (isBookLoaded())
					showBookInfo();
				break;
			case DCMD_USER_MANUAL:
				showManual();
				break;
			case DCMD_TTS_PLAY:
				startTts();
				break;
			case DCMD_TTS_STOP:
				stopTts();
				break;
			case DCMD_TOGGLE_DOCUMENT_STYLES:
				if (isBookLoaded())
					toggleDocumentStyles();
				break;
			case DCMD_SHOW_HOME_SCREEN:
				mActivity.showHomeScreen();
				break;
			case DCMD_TOGGLE_ORIENTATION:
				toggleScreenOrientation();
				break;
			case DCMD_TOGGLE_FULLSCREEN:
				toggleFullscreen();
				break;
			case DCMD_TOGGLE_TITLEBAR:
				toggleTitlebar();
				break;
			case DCMD_SHOW_POSITION_INFO_POPUP:
				if (isBookLoaded())
					showReadingPositionPopup();
				break;
			case DCMD_TOGGLE_SELECTION_MODE:
				if (isBookLoaded())
					toggleSelectionMode();
				break;
			case DCMD_TOGGLE_TOUCH_SCREEN_LOCK:
				isTouchScreenEnabled = !isTouchScreenEnabled;
				if (isTouchScreenEnabled)
					mActivity.showToast(R.string.action_touch_screen_enabled_toast);
				else
					mActivity.showToast(R.string.action_touch_screen_disabled_toast);
				break;
			case DCMD_LINK_BACK:
			case DCMD_LINK_FORWARD:
				navigateByHistory(cmd);
				break;
			case DCMD_ZOOM_OUT:
				doEngineCommand(ReaderCommand.DCMD_ZOOM_OUT, param);
				syncViewSettings(getSettings(), true, true);
				break;
			case DCMD_ZOOM_IN:
				doEngineCommand(ReaderCommand.DCMD_ZOOM_IN, param);
				syncViewSettings(getSettings(), true, true);
				break;
			case DCMD_FONT_NEXT:
				switchFontFace(1);
				break;
			case DCMD_FONT_PREVIOUS:
				switchFontFace(-1);
				break;
			case DCMD_MOVE_BY_CHAPTER:
				if (isBookLoaded())
					doEngineCommand(cmd, param, onFinishHandler);
				drawPage();
				break;
			case DCMD_PAGEDOWN:
				if (isBookLoaded()) {
					ReaderPageAnimationState.Snapshot
							animationSettings =
									pageAnimationState.snapshot();
					if (animationSettings.isEnabled()
							&& param == 1
							&& !DeviceInfo.EINK_SCREEN) {
						animatePageFlip(
								1, onFinishHandler,
								animationSettings);
					} else {
						if (isPageMode()) {
							doEngineCommand(ReaderCommand.DCMD_PAGEDOWN, param, onFinishHandler);
						} else {
							doScrollPageCommand(
									1, onFinishHandler);
						}
					}
				}
				break;
			case DCMD_PAGEUP:
				if (isBookLoaded()) {
					ReaderPageAnimationState.Snapshot
							animationSettings =
									pageAnimationState.snapshot();
					if (animationSettings.isEnabled()
							&& param == 1
							&& !DeviceInfo.EINK_SCREEN) {
						animatePageFlip(
								-1, onFinishHandler,
								animationSettings);
					} else {
						if (isPageMode()) {
							doEngineCommand(ReaderCommand.DCMD_PAGEUP, param, onFinishHandler);
						} else {
							doScrollPageCommand(
									-1, onFinishHandler);
						}
					}
				}
				break;
			case DCMD_BEGIN:
			case DCMD_END:
				if (isBookLoaded())
					doEngineCommand(cmd, param);
				break;
			case DCMD_RECENT_BOOKS_LIST:
				mActivity.showRecentBooks();
				break;
			case DCMD_SEARCH:
				if (isBookLoaded())
					showSearchDialog(null);
				break;
			case DCMD_EXIT:
				mActivity.finish();
				break;
			case DCMD_BOOKMARKS:
				if (isBookLoaded())
					mActivity.showBookmarksDialog();
				break;
			case DCMD_NEW_BOOKMARK:
				if (0 == param) {	// bookmark to page
					addBookmark(0);
				}
				break;
			case DCMD_GO_PERCENT_DIALOG:
				if (isBookLoaded())
					showGoToPercentDialog();
				break;
			case DCMD_GO_PAGE_DIALOG:
				if (isBookLoaded())
					showGoToPageDialog();
				break;
			case DCMD_TOC_DIALOG:
				if (isBookLoaded())
					showTOC();
				break;
			case DCMD_FILE_BROWSER:
				mActivity.showBrowser(!mActivity.isBrowserCreated() ? getOpenedFileInfo() : null);
				break;
			case DCMD_CURRENT_BOOK_DIRECTORY:
				mActivity.showBrowser(getOpenedFileInfo());
				break;
			case DCMD_OPTIONS_DIALOG:
				showOptionsDialog();
				break;
			case DCMD_READER_MENU:
				mActivity.showReaderMenu();
				break;
			case DCMD_TOGGLE_DAY_NIGHT_MODE:
				toggleDayNightMode();
				break;
			case DCMD_TOGGLE_DICT_ONCE:
				log.i("Next dictionary will be the 2nd for one time");
				mActivity.showToast("Next dictionary will be the 2nd for one time");
				mActivity.mDictionaries.setiDic2IsActive(2);
				break;
			case DCMD_TOGGLE_DICT:
				if (mActivity.mDictionaries.isiDic2IsActive() > 0) {
					mActivity.mDictionaries.setiDic2IsActive(0);
				} else {
					mActivity.mDictionaries.setiDic2IsActive(1);
				}
				log.i("Switched to dictionary: " + Integer.toString(mActivity.mDictionaries.isiDic2IsActive() + 1));
				mActivity.showToast("Switched to dictionary: " + Integer.toString(mActivity.mDictionaries.isiDic2IsActive() + 1));
				break;
			case DCMD_BACKLIGHT_SET_DEFAULT:
				setSetting(PROP_APP_SCREEN_BACKLIGHT, "-1");		// system default backlight level
				break;
			case DCMD_SHOW_SYSTEM_BACKLIGHT_DIALOG:
				if (DeviceInfo.EINK_HAVE_FRONTLIGHT) {
					if (DeviceInfo.EINK_ONYX) {
						mActivity.sendBroadcast(new Intent("action.show.brightness.dialog"));
					} else {
						// TODO: other eink devices with frontlight
					}
				}
				break;
			/*
			  Commented until the appearance of free implementation of the binding to the Google Drive (R)
			case DCMD_GOOGLEDRIVE_SYNC:
				if (0 == param) {							// sync to
					mActivity.forceSyncToGoogleDrive();
				} else if (1 == param) {					// sync from
					mActivity.forceSyncFromGoogleDrive();
				}
				break;
			 */
			case DCMD_SAVE_LOGCAT:
				mActivity.createLogcatFile();
				break;
			default:
				// do nothing
				break;
		}
	}

	boolean firstShowBrowserCall = true;

	private final CloseableTaskGate ttsInitializationLifecycle =
			new CloseableTaskGate();
	private final ReaderTtsToolbarState<TTSToolbarDlg>
			ttsToolbarState = new ReaderTtsToolbarState<>();

	private void startTts() {
		BackgroundThread.ensureGUI();
		if (ttsToolbarState.current() != null) {
			log.i("DCMD_TTS_PLAY: skipping re-init of active TTS");
			return;
		}
		final BookInfo expectedBook = mBookInfo;
		final DocumentLoadLifecycle.Interaction interaction =
				documentLoadLifecycle.interaction();
		final TtsDocumentSnapshot documentSnapshot =
				TtsDocumentSnapshot.capture(expectedBook);
		if (documentSnapshot == null
				|| !isDocumentInteractionCurrent(
						expectedBook, interaction))
			return;
		CloseableTaskGate.Token owner =
				ttsInitializationLifecycle.beginIfIdle();
		if (owner == null) {
			log.i("DCMD_TTS_PLAY: TTS initialization is already pending");
			return;
		}
		log.i("DCMD_TTS_PLAY: initializing TTS");
		mActivity.initTTS(
				ttsacc -> finishTtsInitialization(
						owner,
						ttsacc,
						expectedBook,
						interaction,
						documentSnapshot),
				() -> ttsInitializationLifecycle.complete(owner));
	}

	private void finishTtsInitialization(
			CloseableTaskGate.Token owner,
			TTSControlServiceAccessor ttsAccessor,
			BookInfo expectedBook,
			DocumentLoadLifecycle.Interaction interaction,
			TtsDocumentSnapshot documentSnapshot) {
		BackgroundThread.ensureGUI();
		if (!ttsInitializationLifecycle.complete(owner)
				|| !mServiceLifecycle.isActive()
				|| ttsAccessor == null
				|| !isDocumentInteractionCurrent(
						expectedBook, interaction))
			return;
		log.i("TTS created: opening TTS toolbar");
		TTSToolbarDlg toolbar = TTSToolbarDlg.showDialog(
				mActivity,
				surface,
				mEngine,
				documentSnapshot,
				ttsDocumentHandler(
						expectedBook, interaction),
				ttsAccessor);
		if (!ttsToolbarState.startIfIdle(toolbar)) {
			toolbar.stopAndCloseForDocumentChange();
			return;
		}
		toolbar.setOnCloseListener(
				() -> ttsToolbarState.finish(toolbar));
		toolbar.setAppSettings(
				readerSettingsState.copy(), null);
		toolbar.initAudiobookWordTimings(null);
	}

	private TtsDocumentHandler ttsDocumentHandler(
			final BookInfo expectedBook,
			final DocumentLoadLifecycle.Interaction interaction) {
		return new TtsDocumentHandler() {
			@Override
			public boolean isActive() {
				return isDocumentInteractionCurrent(
						expectedBook, interaction);
			}

			@Override
			public void clearSelection() {
				ReaderView.this.clearSelection(
						expectedBook, interaction);
			}

			@Override
			public void savePosition() {
				if (isActive())
					ReaderView.this.save();
			}

			@Override
			public ReaderViewModeState.Lease enterReaderMode() {
				if (!isActive())
					return null;
				return acquireTemporaryScrollMode();
			}

			@Override
			public void restoreReaderMode(
					ReaderViewModeState.Lease lease) {
				releaseTemporaryScrollMode(lease);
			}

			@Override
			public void moveSelection(
					ReaderCommand command,
					SelectionHandler selectionHandler) {
				ReaderView.this.moveSelection(
						command,
						0,
						new MoveSelectionCallback() {
							@Override
							public void onNewSelection(
									Selection selection) {
								selectionHandler
										.onNewSelection(
												selection);
							}

							@Override
							public void onFail() {
								selectionHandler.onFail();
							}
						},
						expectedBook,
						interaction);
			}

			@Override
			public void drawCover(
					Bitmap bitmap,
					CoverHandler coverHandler) {
				if (!isActive()
						|| bitmap == null
						|| mActivity.getDB() == null)
					return;
				mCoverpageManager.drawCoverpageFor(
						mActivity.getDB(),
						expectedBook.getFileInfo(),
						bitmap,
						true,
						(file, readyBitmap) -> {
							if (isActive())
								coverHandler.onCoverReady(
										readyBitmap);
						});
			}

			@Override
			public List<SentenceInfo> getAllSentences() {
				return BackgroundThread.instance()
						.callBackground(() -> {
							if (!isActive())
								return null;
							List<SentenceInfo> sentences =
									doc.getAllSentences();
							return isActive()
									? sentences
									: null;
						});
			}
		};
	}

	private void stopTts() {
		BackgroundThread.ensureGUI();
		ttsInitializationLifecycle.cancel();
		mActivity.cancelTtsInitialization();
		TTSToolbarDlg toolbar = ttsToolbarState.current();
		if (toolbar != null) {
			log.i("DCMD_TTS_STOP: stopping TTS");
			toolbar.stopAndCloseForDocumentChange();
		}
	}

	public void stopTtsForDocumentChange() {
		stopTts();
	}

	public void pauseTTS() {
		TTSToolbarDlg toolbar = ttsToolbarState.current();
		if (toolbar != null)
			toolbar.pause();
	}

	public boolean isTTSActive() {
		return ttsToolbarState.current() != null;
	}

	public TTSToolbarDlg getTTSToolbar() {
		return ttsToolbarState.current();
	}

	public void doEngineCommand(final ReaderCommand cmd, final int param) {
		doEngineCommand(cmd, param, null);
	}

	public void doEngineCommand(final ReaderCommand cmd, final int param, final Runnable doneHandler) {
		BackgroundThread.ensureGUI();
		if (cmd == null || !mServiceLifecycle.isActive())
			return;
		log.d("doCommand(" + cmd + ", " + param + ")");
		final ReaderEngineCommandPolicy.Scope commandScope =
				ReaderEngineCommandPolicy.scopeOf(cmd);
		final boolean movesDocument =
				ReaderEngineCommandPolicy.movesDocument(cmd);
		final BookInfo expectedBook = mBookInfo;
		final DocumentLoadLifecycle.Interaction interaction =
				documentLoadLifecycle.interaction();
		final ReaderRenderRequest renderRequest =
				commandScope
						== ReaderEngineCommandPolicy
								.Scope.DOCUMENT
						? ReaderRenderRequest.fromInteraction(
								expectedBook, interaction)
						: null;
		if (commandScope
				== ReaderEngineCommandPolicy.Scope.DOCUMENT
				&& !isRenderRequestCurrent(renderRequest))
			return;
		postEngineCommand(
				commandScope,
				movesDocument,
				expectedBook,
				interaction,
				renderRequest,
				doneHandler,
				() -> doc.doCommand(cmd.nativeId, param));
	}

	private void doScrollPageCommand(
			final int direction,
			final Runnable doneHandler) {
		BackgroundThread.ensureGUI();
		if (direction == 0 || !mServiceLifecycle.isActive())
			return;
		final BookInfo expectedBook = mBookInfo;
		final DocumentLoadLifecycle.Interaction interaction =
				documentLoadLifecycle.interaction();
		final ReaderRenderRequest renderRequest =
				ReaderRenderRequest.fromInteraction(
						expectedBook, interaction);
		if (!isRenderRequestCurrent(renderRequest))
			return;
		postEngineCommand(
				ReaderEngineCommandPolicy.Scope.DOCUMENT,
				true,
				expectedBook,
				interaction,
				renderRequest,
				doneHandler,
				() -> {
					PositionProperties position =
							doc.getPositionProps(
									null, false);
					Integer destination =
							ReaderScrollPageCommand
									.destination(
											position,
											direction);
					return destination != null
							&& isEngineCommandRequestCurrent(
									ReaderEngineCommandPolicy
											.Scope.DOCUMENT,
									renderRequest)
							&& doc.doCommand(
									ReaderCommand
											.DCMD_GO_POS
											.nativeId,
									destination);
				});
	}

	private interface NativeCommandOperation {
		boolean execute();
	}

	private void postEngineCommand(
			final ReaderEngineCommandPolicy.Scope commandScope,
			final boolean movesDocument,
			final BookInfo expectedBook,
			final DocumentLoadLifecycle.Interaction interaction,
			final ReaderRenderRequest renderRequest,
			final Runnable doneHandler,
			final NativeCommandOperation operation) {
		post(new Task() {
			boolean res;

			public void work() {
				BackgroundThread.ensureBackground();
				if (!isEngineCommandRequestCurrent(
						commandScope, renderRequest))
					return;
				res = operation.execute();
				if (!isEngineCommandRequestCurrent(
						commandScope, renderRequest))
					return;
				if (movesDocument)
					updateCurrentPositionStatus(
							expectedBook, interaction);
			}

			public void done() {
				if (!isEngineCommandRequestCurrent(
						commandScope, renderRequest))
					return;
				if (res) {
					pageInvalidationState.invalidate();
					if (commandScope
							== ReaderEngineCommandPolicy
									.Scope.DOCUMENT) {
						drawPage(
								doneHandler,
								false,
								renderRequest);
					} else {
						drawPage(doneHandler, false);
					}
				}
				if (movesDocument)
					scheduleSaveCurrentPositionBookmark(
							DEF_SAVE_POSITION_INTERVAL,
							expectedBook, interaction);
			}
		});
	}

	private boolean isEngineCommandRequestCurrent(
			ReaderEngineCommandPolicy.Scope commandScope,
			ReaderRenderRequest renderRequest) {
		return mServiceLifecycle.isActive()
				&& readerNativeLifecycle.isInitialized()
				&& (commandScope
						== ReaderEngineCommandPolicy.Scope.READER
						|| isRenderRequestCurrent(renderRequest));
	}

	// update book and position info in status bar
	private void updateCurrentPositionStatus(
			final BookInfo expectedBook,
			final DocumentLoadLifecycle.Interaction interaction) {
		if (!isDocumentInteractionCurrent(
				expectedBook, interaction))
			return;
		// in background thread
		final FileInfo fileInfo = expectedBook.getFileInfo();
		if (fileInfo == null)
			return;
		final Bookmark bmk = doc != null ? doc.getCurrentPageBookmark() : null;
		final PositionProperties props = bmk != null ? doc.getPositionProps(bmk.getStartPos(), false) : null;
		if (props != null) BackgroundThread.instance().postGUI(() -> {
			if (!isDocumentInteractionCurrent(
					expectedBook, interaction))
				return;
			mActivity.updateCurrentPositionStatus(fileInfo, bmk, props);

			String fname = fileInfo.getBasePath();
			if (fname != null && fname.length() > 0)
				setBookPositionForExternalShell(fname, props.pageNumber, props.pageCount);
		});
	}

	volatile private boolean mOpened = false;

	//private File historyFile;

	private void updateLoadedBookInfo(
			BookInfo bookInfo, boolean updatePath,
			DocumentLoadLifecycle.Interaction interaction) {
		BackgroundThread.ensureBackground();
		// get title, authors, genres, etc.
		doc.updateBookInfo(bookInfo, updatePath);
		updateCurrentPositionStatus(
				bookInfo, interaction);
		// check whether current book properties updated on another devices
		// TODO: fix and reenable
		//syncUpdater.syncExternalChanges(bookInfo);
	}

	private boolean isDocumentInteractionCurrent(
			BookInfo expectedBook,
			DocumentLoadLifecycle.Interaction interaction) {
		return mServiceLifecycle.isActive()
				&& expectedBook != null
				&& mBookInfo == expectedBook
				&& documentLoadLifecycle.isInteractionActive(
						interaction);
	}

	private boolean isRenderRequestCurrent(
			ReaderRenderRequest request) {
		return mServiceLifecycle.isActive()
				&& request != null
				&& request.isCurrent(
						mBookInfo, documentLoadLifecycle);
	}

	private void applySettings(
			Properties props,
			ReaderViewModeState.Snapshot viewModeSnapshot,
			ReaderSettingsApplyRequest applyRequest) {
		props = new Properties(props); // make a copy
		props.remove(PROP_TXT_OPTION_PREFORMATTED);
		props.remove(PROP_EMBEDDED_STYLES);
		props.remove(PROP_EMBEDDED_FONTS);
		props.remove(PROP_REQUESTED_DOM_VERSION);
		props.remove(PROP_RENDER_BLOCK_RENDERING_FLAGS);
		BackgroundThread.ensureBackground();
		log.v("applySettings()");
		boolean isFullScreen = props.getBool(PROP_APP_FULLSCREEN, false);
		props.setBool(PROP_SHOW_BATTERY, isFullScreen);
		props.setBool(PROP_SHOW_TIME, isFullScreen);
		String backgroundImageId = props.getProperty(PROP_PAGE_BACKGROUND_IMAGE);
		int backgroundColor = props.getColor(PROP_BACKGROUND_COLOR, 0xFFFFFF);
		setBackgroundTexture(backgroundImageId, backgroundColor);
		int statusLocation = props.getInt(PROP_STATUS_LOCATION, VIEWER_STATUS_PAGE_HEADER);
		int statusLine = 0;
		switch (statusLocation) {
			case VIEWER_STATUS_PAGE_HEADER:
				statusLine = 1;
				break;
			case VIEWER_STATUS_PAGE_FOOTER:
				statusLine = 2;
				break;
		}
		props.setInt(PROP_STATUS_LINE, statusLine);
		props.setBool(
				PROP_PAGE_VIEW_MODE,
				viewModeSnapshot.isPageMode());
		final boolean hadConfiguredMainLanguage =
				props.containsKey(PROP_TEXTLANG_MAIN_LANG);
		final String configuredMainLanguage =
				props.getProperty(PROP_TEXTLANG_MAIN_LANG);
		boolean appliedBookLanguage = false;

		if (!inDisabledFullRefresh()) {
			// If this function is called when new settings loaded from the cloud are applied,
			// we must prohibit changing the e-ink screen refresh mode, as this will lead to
			// a periodic full screen refresh when drawing the next phase of the progress bar.
			int updModeCode = props.getInt(PROP_APP_SCREEN_UPDATE_MODE, EinkScreen.EinkUpdateMode.Clear.code);
			int updInterval = props.getInt(PROP_APP_SCREEN_UPDATE_INTERVAL, 10);
			mActivity.setScreenUpdateMode(EinkScreen.EinkUpdateMode.byCode(updModeCode), surface);
			mActivity.setScreenUpdateInterval(updInterval, surface);
		}

		final String bookLanguage =
				applyRequest != null
						? applyRequest.bookLanguage(
								documentLoadLifecycle)
						: null;
		if (bookLanguage != null) {
			final String fontFace = props.getProperty(PROP_FONT_FACE);
			if (bookLanguage.length() > 0) {
				if (props.getBool(PROP_TEXTLANG_EMBEDDED_LANGS_ENABLED, false)) {
					props.setProperty(PROP_TEXTLANG_MAIN_LANG, bookLanguage);
					appliedBookLanguage = true;
				}
				final String langDescr = Engine.getHumanReadableLocaleName(bookLanguage);
				if (null != langDescr && langDescr.length() > 0) {
					Engine.font_lang_compat compat = Engine.checkFontLanguageCompatibility(fontFace, bookLanguage);
					log.d("Checking font \"" + fontFace + "\" for compatibility with language \"" + bookLanguage + "\" fcLangCode=" + langDescr + ": compat=" + compat);
					switch (compat) {
						case font_lang_compat_invalid_tag:
							log.w("Can't find compatible language code in embedded FontConfig catalog: language=\"" + bookLanguage + "\"");
							break;
						case font_lang_compat_none:
							BackgroundThread.instance().executeGUI(() -> {
								if (applyRequest.isCurrent(
										documentLoadLifecycle))
									mActivity.showToast(
											R.string.font_not_compat_with_language,
											fontFace, langDescr);
							});
							break;
						case font_lang_compat_partial:
							BackgroundThread.instance().executeGUI(() -> {
								if (applyRequest.isCurrent(
										documentLoadLifecycle))
									mActivity.showToast(
											R.string.font_compat_partial_with_language,
											fontFace, langDescr);
							});
							break;
						case font_lang_compat_full:
							// good, do nothing
							break;
					}
				} else {
						log.d("Invalid language tag: \"" + bookLanguage + "\"");
				}
			}
		}
		if (appliedBookLanguage
				&& !applyRequest.isCurrent(
						documentLoadLifecycle)) {
			if (hadConfiguredMainLanguage)
				props.setProperty(
						PROP_TEXTLANG_MAIN_LANG,
						configuredMainLanguage);
			else
				props.remove(PROP_TEXTLANG_MAIN_LANG);
		}
		doc.applySettings(props);
		//syncViewSettings(props, save, saveDelayed);
		ReaderRenderRequest renderRequest =
				applyRequest != null
						? applyRequest.renderRequest(
								mBookInfo,
								documentLoadLifecycle)
						: null;
		if (renderRequest != null)
			drawPage(null, false, renderRequest);
	}

	public static boolean eq(Object obj1, Object obj2) {
		if (obj1 == null && obj2 == null)
			return true;
		if (obj1 == null || obj2 == null)
			return false;
		return obj1.equals(obj2);
	}

	public void saveSettings(Properties settings) {
		mActivity.setSettings(settings, 0, false);
	}

	/**
	 * Read JNI view settings, update and save if changed
	 */
	private void syncViewSettings(final Properties currSettings, final boolean save, final boolean saveDelayed) {
		BackgroundThread.ensureGUI();
		final BookInfo expectedBook = mBookInfo;
		final DocumentLoadLifecycle.Interaction interaction =
				documentLoadLifecycle.interaction();
		if (!isDocumentInteractionCurrent(
				expectedBook, interaction))
			return;
		final ReaderSettingsSyncSnapshot snapshot =
				ReaderSettingsSyncSnapshot.capture(
						currSettings);
		if (snapshot == null)
			return;
		final CloseableTaskGate.Token owner =
				settingsSyncLifecycle.replace();
		if (owner == null)
			return;
		post(new Task() {
			Properties nativeSettings;

			public void work() {
				BackgroundThread.ensureBackground();
				if (!settingsSyncLifecycle.isActive(owner)
						|| !isDocumentInteractionCurrent(
								expectedBook, interaction))
					return;
				java.util.Properties internalProps = doc.getSettings();
				if (!settingsSyncLifecycle.isActive(owner)
						|| !isDocumentInteractionCurrent(
								expectedBook, interaction))
					return;
				nativeSettings = new Properties(internalProps);
			}

			public void done() {
				BackgroundThread.ensureGUI();
				if (!settingsSyncLifecycle.complete(owner)
						|| !isDocumentInteractionCurrent(
								expectedBook, interaction))
					return;
				Properties merged =
						snapshot.merge(
								readerSettingsState.copy(),
								nativeSettings);
				if (merged == null)
					return;
				merged.setBool(
						PROP_PAGE_VIEW_MODE,
						readerViewModeState
								.isConfiguredPageMode());
				ReaderSettingsState.Snapshot published =
						readerSettingsState.replace(merged);
				if (save) {
					mActivity.setSettings(
							published.copy(),
							saveDelayed ? 5000 : 0,
							false);
				} else {
					mActivity.setSettings(
							published.copy(), -1, false);
				}
			}

			public void fail(Exception e) {
				BackgroundThread.ensureGUI();
				if (settingsSyncLifecycle.complete(owner)
						&& isDocumentInteractionCurrent(
								expectedBook, interaction))
					log.e("Cannot synchronize reader settings", e);
			}
		});
	}

	public Properties getSettings() {
		return readerSettingsState.copy();
	}

	static public int stringToInt(String value, int defValue) {
		if (value == null)
			return defValue;
		try {
			return Integer.valueOf(value);
		} catch (NumberFormatException e) {
			return defValue;
		}
	}

	private String getManualFileName() {
		Scanner s = mScanner;
		if (s != null) {
			FileInfo fi = s.getDownloadDirectory();
			if (fi != null) {
				File bookDir = new File(fi.getPathName());
				return HelpFileGenerator.getHelpFileName(bookDir, mActivity.getCurrentLanguage()).getAbsolutePath();
			}
		}
		log.e("cannot get manual file name!");
		return null;
	}

	private File generateManual() {
		HelpFileGenerator generator = new HelpFileGenerator(mActivity, mEngine, getSettings(), mActivity.getCurrentLanguage());
		FileInfo downloadDir = mScanner.getDownloadDirectory();
		File bookDir;
		if (downloadDir != null)
			bookDir = new File(mScanner.getDownloadDirectory().getPathName());
		else {
			log.e("cannot download directory file name!");
			bookDir = new File("/tmp/");
		}
		int settingsHash = generator.getSettingsHash();
		String helpFileContentId = mActivity.getCurrentLanguage() + settingsHash + "v" + mActivity.getVersion();
		String lastHelpFileContentId = mActivity.getLastGeneratedHelpFileSignature();
		File manual = generator.getHelpFileName(bookDir);
		if (!manual.exists() || lastHelpFileContentId == null || !lastHelpFileContentId.equals(helpFileContentId)) {
			log.d("Generating help file " + manual.getAbsolutePath());
			mActivity.setLastGeneratedHelpFileSignature(helpFileContentId);
			manual = generator.generateHelpFile(bookDir);
		}
		return manual;
	}

	/**
	 * Generate help file (if necessary) and show it.
	 *
	 * @return true if opened successfully
	 */
	private DocumentLoadLifecycle.Request replaceDocumentLoad() {
		BackgroundThread.ensureGUI();
		stopTts();
		stopImageViewer();
		resetTemporaryViewMode();
		timeTickLifecycle.cancel();
		cancelPositionSave();
		bookInfoDialogLifecycle.cancel();
		readerOptionsDialogLifecycle.cancel();
		settingsSyncLifecycle.cancel();
		DocumentLoadLifecycle.Request loadOwner =
				documentLoadLifecycle.replace();
		drawTaskLifecycle.cancel();
		return loadOwner;
	}

	public boolean showManual() {
		return showManual(replaceDocumentLoad());
	}

	public boolean showManual(
			DocumentLoadLifecycle.Request loadOwner) {
		if (!documentLoadLifecycle.isActive(loadOwner))
			return false;
		File manual = generateManual();
		if (!documentLoadLifecycle.isActive(loadOwner))
			return false;
		if (manual == null) {
			documentLoadLifecycle.complete(loadOwner);
			return false;
		}
		return loadDocument(
				loadOwner,
				DocumentSource.file(manual.getAbsolutePath()),
				null, () -> mActivity.showToast("Error while opening manual"));
	}

	static private final int DEF_PAGE_FLIP_MS = 300;

	public void applyAppSetting(String key, String value) {
		boolean flg = "1".equals(value);
		if (key.equals(PROP_PAGE_VIEW_MODE)) {
			readerViewModeState.configure(flg);
		} else if (PROP_APP_HIGHLIGHT_BOOKMARKS.equals(key)) {
			clearSelection();
		} else if (PROP_APP_VIEW_AUTOSCROLL_SPEED.equals(key)) {
			autoScrollSpeedState.configure(
					Utils.parseInt(
							value,
							AutoScrollSpeedState.DEFAULT_SPEED,
							AutoScrollSpeedState.MIN_SPEED,
							AutoScrollSpeedState.MAX_SPEED));
		} else if (PROP_PAGE_ANIMATION.equals(key)) {
			pageAnimationState.configure(value);
		} else if (PROP_APP_VIEW_ANIM_DURATION.equals(key)) {
			animationTiming.resetSamples(Utils.parseInt(value, 50));
		} else {
			//mActivity.applyAppSetting(key, value);
		}
		//
	}

	public void setAppSettings(Properties newSettings, Properties oldSettings) {
		log.v("setAppSettings()"); //|| keyCode == KeyEvent.KEYCODE_DPAD_LEFT
		BackgroundThread.ensureGUI();
		if (oldSettings == null)
			oldSettings = readerSettingsState.copy();
		Properties changedSettings = newSettings.diff(oldSettings);
		for (Map.Entry<Object, Object> entry : changedSettings.entrySet()) {
			String key = (String) entry.getKey();
			String value = (String) entry.getValue();
			applyAppSetting(key, value);
			if (PROP_APP_FULLSCREEN.equals(key)) {
				boolean flg =
						readerSettingsState.getBool(
								PROP_APP_FULLSCREEN,
								false);
				newSettings.setBool(PROP_SHOW_BATTERY, flg);
				newSettings.setBool(PROP_SHOW_TIME, flg);
			} else if (PROP_APP_SCREEN_ORIENTATION.equals(key)
					|| PROP_PAGE_ANIMATION.equals(key)
					|| PROP_PAGE_VIEW_MODE.equals(key)
					|| PROP_CONTROLS_ENABLE_VOLUME_KEYS.equals(key)
					|| PROP_APP_SHOW_COVERPAGES.equals(key)
					|| PROP_APP_COVERPAGE_SIZE.equals(key)
					|| PROP_APP_SCREEN_BACKLIGHT.equals(key)
					|| PROP_APP_SCREEN_WARM_BACKLIGHT.equals(key)
					|| PROP_APP_BOOK_PROPERTY_SCAN_ENABLED.equals(key)
					|| PROP_APP_SCREEN_BACKLIGHT_LOCK.equals(key)
					|| PROP_APP_TAP_ZONE_HILIGHT.equals(key)
					|| PROP_APP_DICTIONARY.equals(key)
					|| PROP_APP_DOUBLE_TAP_SELECTION.equals(key)
					|| PROP_APP_BOUNCE_TAP_INTERVAL.equals(key)
					|| PROP_APP_FLICK_BACKLIGHT_CONTROL.equals(key)
					|| PROP_APP_FLICK_WARMLIGHT_CONTROL.equals(key)
					|| PROP_APP_FLICK_BACKLIGHT_CONTROL_TOGETHER.equals(key)
					|| PROP_APP_FILE_BROWSER_HIDE_EMPTY_FOLDERS.equals(key)
					|| PROP_APP_FILE_BROWSER_HIDE_EMPTY_GENRES.equals(key)
					|| PROP_APP_SELECTION_ACTION.equals(key)
					|| PROP_APP_FILE_BROWSER_SIMPLE_MODE.equals(key)
					|| PROP_APP_GESTURE_PAGE_FLIPPING.equals(key)
					|| PROP_APP_HIGHLIGHT_BOOKMARKS.equals(key)
					|| PROP_HIGHLIGHT_SELECTION_COLOR.equals(key)
					|| PROP_HIGHLIGHT_BOOKMARK_COLOR_COMMENT.equals(key)
					|| PROP_HIGHLIGHT_BOOKMARK_COLOR_CORRECTION.equals(key)
				// TODO: redesign all this mess!
			) {
				newSettings.setProperty(key, value);
			}
		}
	}

	public ViewMode getViewMode() {
		return isPageMode() ? ViewMode.PAGES : ViewMode.SCROLL;
	}

	/**
	 * Change settings.
	 *
	 * @param newSettings are new settings
	 */
	public void updateSettings(Properties newSettings) {
		log.v("updateSettings() " + newSettings.toString());
		log.v("oldNightMode="
				+ readerSettingsState.getProperty(
						PROP_NIGHT_MODE)
				+ " newNightMode="
				+ newSettings.getProperty(PROP_NIGHT_MODE));
		BackgroundThread.ensureGUI();
		settingsSyncLifecycle.cancel();
		final Properties currSettings =
				readerSettingsState.copy();
		TTSToolbarDlg toolbar = ttsToolbarState.current();
		if (toolbar != null) {
			// ignore all non TTS options if TTS is active...
			toolbar.setAppSettings(newSettings, currSettings);
			Properties changedSettings = newSettings.diff(currSettings);
			currSettings.setAll(changedSettings);
			readerSettingsState.replace(currSettings);
		} else {
			setAppSettings(newSettings, currSettings);
			Properties changedSettings = newSettings.diff(currSettings);
			currSettings.setAll(changedSettings);
			readerSettingsState.replace(currSettings);
			final ReaderViewModeState.Snapshot viewModeSnapshot =
					readerViewModeState.snapshot();
			final ReaderSettingsApplyRequest applyRequest =
					ReaderSettingsApplyRequest.capture(
							mBookInfo,
							documentLoadLifecycle);
			BackgroundThread.instance().postBackground(
					() -> applySettings(
							currSettings,
							viewModeSnapshot,
							applyRequest));
		}
	}

	private void setBackgroundTexture(String textureId, int color) {
		for (BackgroundTextureInfo item :
				mEngine.getAvailableTextures()) {
			if (item.getId().equals(textureId)) {
				setBackgroundTexture(item, color);
				return;
			}
		}
		setBackgroundTexture(Engine.NO_TEXTURE, color);
	}

	private void setBackgroundTexture(BackgroundTextureInfo texture, int color) {
		log.v("setBackgroundTexture(" + texture + ", " + color + ")");
		boolean tiled = texture.isTiled();
		if (!backgroundState.needsReplacement(
				texture, tiled, color))
			return;
		log.d("setBackgroundTexture( " + texture + " )");
		byte[] data = mEngine.getImageData(texture);
		if (!backgroundState.needsReplacement(
				texture, tiled, color))
			return;
		doc.setPageBackgroundTexture(
				data, tiled ? 1 : 0);
		Bitmap candidate = null;
		if (data != null && data.length > 0) {
			try {
				candidate =
						android.graphics.BitmapFactory
								.decodeByteArray(
										data, 0,
										data.length);
			} catch (Exception e) {
				log.e(
						"Exception while decoding image data",
						e);
			}
		}
		ReaderBackgroundState.Publication<Bitmap>
				publication =
						backgroundState.replace(
								texture, candidate,
								tiled, color);
		recycleBackgroundBitmap(
				publication.releasable());
	}

	private final ReaderBackgroundState<
			BackgroundTextureInfo, Bitmap> backgroundState =
					new ReaderBackgroundState<>(
							Engine.NO_TEXTURE,
							null, false, 0);

	private void recycleBackgroundBitmap(Bitmap bitmap) {
		if (bitmap != null && !bitmap.isRecycled())
			bitmap.recycle();
	}

	class CreateViewTask extends Task {
		Properties props = new Properties();
		private final ReaderViewModeState.Snapshot
				viewModeSnapshot;
		private final ReaderSettingsApplyRequest
				settingsApplyRequest;

		public CreateViewTask(Properties props) {
			this.props = new Properties(props);
			Properties oldSettings = new Properties(); // may be changed by setAppSettings
			setAppSettings(this.props, oldSettings);
			this.props.setAll(oldSettings);
			readerSettingsState.replace(this.props);
			viewModeSnapshot = readerViewModeState.snapshot();
			settingsApplyRequest =
					ReaderSettingsApplyRequest.capture(
							mBookInfo,
							documentLoadLifecycle);
		}

		public void work() throws Exception {
			BackgroundThread.ensureBackground();
			if (!readerNativeLifecycle.isActive())
				return;
			log.d("CreateViewTask - in background thread");

			//File historyDir = activity.getDir("settings", Context.MODE_PRIVATE);
			//historyDir.mkdirs();
			//File historyFile = new File(historyDir, "cr3hist.ini");

			//File historyFile = new File(activity.getDir("settings", Context.MODE_PRIVATE), "cr3hist.ini");
			//if ( historyFile.exists() ) {
			//log.d("Reading history from file " + historyFile.getAbsolutePath());
			//readHistoryInternal(historyFile.getAbsolutePath());
			//}
			String css = mEngine.loadResourceUtf8(R.raw.fb2);
			if (css != null && css.length() > 0)
				doc.setStylesheet(css);
			applySettings(
					props,
					viewModeSnapshot,
					settingsApplyRequest);
			if (!readerNativeLifecycle.markInitialized())
				return;
			log.i("CreateViewTask - finished");
		}

		public void done() {
			if (readerNativeLifecycle.isClosed())
				return;
			log.d("InitializationFinishedEvent");
			//BackgroundThread.ensureGUI();
			//setSettings(props, new Properties());
		}

		public void fail(Exception e) {
			if (readerNativeLifecycle.isClosed())
				return;
			log.e("CoolReader engine initialization failed. Exiting.", e);
			mEngine.fatalError("Failed to init CoolReader engine");
		}
	}

	public void closeIfOpened(final FileInfo fileInfo) {
		if (this.mBookInfo != null
				&& this.mBookInfo.getFileInfo().sameBook(fileInfo)
				&& mOpened) {
			close();
		}
	}

	public boolean reloadDocument() {
		if (this.mBookInfo != null && this.mBookInfo.getFileInfo() != null) {
			stopTts();
			save(); // save current position
			DocumentSource source =
					DocumentSource.fromFileInfo(this.mBookInfo.getFileInfo());
			if (source.getKind() == DocumentSource.Kind.CONTENT_URI) {
				mActivity.loadDocument(
						this.mBookInfo.getFileInfo(), null, null, true);
				return true;
			}
			DocumentLoadLifecycle.Request loadOwner =
					replaceDocumentLoad();
			return enqueueDocumentLoad(
					loadOwner, this.mBookInfo, source, null,
					null, null, null, null);
		}
		return false;
	}

	public boolean loadDocument(final FileInfo fileInfo, final Runnable doneHandler, final Runnable errorHandler) {
		return loadDocument(
				replaceDocumentLoad(),
				fileInfo, DocumentSource.fromFileInfo(fileInfo),
				doneHandler, errorHandler);
	}

	private boolean loadDocument(
			final DocumentLoadLifecycle.Request loadOwner,
			final FileInfo fileInfo, final DocumentSource source,
			final Runnable doneHandler, final Runnable errorHandler) {
		log.v("loadDocument(" + fileInfo.getPathName() + ")");
		if (!documentLoadLifecycle.isActive(loadOwner))
			return false;
		applySourceBookKeyIfMissing(fileInfo, source);
		if (this.mBookInfo != null
				&& this.mBookInfo.getFileInfo().sameBook(fileInfo)
				&& mOpened) {
			if (!documentLoadLifecycle.markPublished(loadOwner))
				return false;
			log.d("trying to load already opened document");
			mActivity.showReader();
			if (null != doneHandler)
				doneHandler.run();
			drawPage();
			return false;
		}
		mHistory.getOrCreateBookInfo(mActivity.getDB(), fileInfo, bookInfo -> {
			if (!documentLoadLifecycle.isActive(loadOwner))
				return;
			log.v("posting LoadDocument task to background thread");
			BackgroundThread.instance().postBackground(() -> {
				if (!documentLoadLifecycle.isActive(loadOwner))
					return;
				log.v("posting LoadDocument task to GUI thread");
				BackgroundThread.instance().postGUI(() -> {
					if (!documentLoadLifecycle.isActive(loadOwner))
						return;
					log.v("synced posting LoadDocument task to GUI thread");
					enqueueDocumentLoad(
							loadOwner, bookInfo, source, null,
							null, null, doneHandler, errorHandler);
				});
			});
		});
		return true;
	}

	public boolean loadDocumentFromStream(
			final InputStream inputStream, final DocumentSource source,
			final Runnable doneHandler, final Runnable errorHandler) {
		BackgroundThread.ensureGUI();
		save();
		if (inputStream == null || source == null) {
			if (errorHandler != null)
				errorHandler.run();
			return false;
		}
		final DocumentLoadLifecycle.Request loadOwner =
				replaceDocumentLoad();
		if (loadOwner == null) {
			try {
				inputStream.close();
			} catch (IOException ignored) {
			}
			return false;
		}
		String identity = source.getIdentity();
		FileInfo fileInfo;
		try {
			fileInfo = new FileInfo(source.getLocalPath());
		} catch (IllegalStateException e) {
			fileInfo = new FileInfo();
			fileInfo.pathname = identity;
		}
		if (source.getDisplayName() != null)
			fileInfo.filename = source.getDisplayName();
		if (fileInfo.filename == null || fileInfo.filename.length() == 0)
			fileInfo.filename = "document";
		if (source.getFormat() != null)
			fileInfo.format = source.getFormat();
		if (source.getSize() >= 0)
			fileInfo.size = source.getSize();
		applySourceBookKeyIfMissing(fileInfo, source);
		log.v("loadDocumentFromStream("
				+ safeDocumentPathForLog(identity) + ")");
		// When the document is opened from the stream at this moment,
		// we do not know the real path to the file, since it will be
		// changed after the successful opening of the document,
		// so here we cannot compare the path to the document currently
		// open with the fileinfo argument.

		// Copy data from input stream to byte array
		final int maxMemoryStreamBytes = 16 * 1024 * 1024;
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		boolean copyOk = false;
		try (InputStream documentStream = inputStream) {
			byte [] buf = new byte [4096];
			int totalBytes = 0;
			int readBytes;
			while (true) {
				readBytes = documentStream.read(buf);
				if (readBytes > 0) {
					if (totalBytes > maxMemoryStreamBytes - readBytes)
						throw new IOException("Memory stream exceeds 16 MiB limit");
					outputStream.write(buf, 0, readBytes);
					totalBytes += readBytes;
				} else if (readBytes < 0) {
					break;
				}
			}
			copyOk = true;
		} catch (IOException e1) {
			log.e("I/O error while copying content from input stream to buffer. Interrupted.");
		} catch (OutOfMemoryError e2) {
			log.e("Out of memory while copying content from input stream to buffer. Interrupted.");
		}
		if (copyOk) {
			byte[] docBuffer = outputStream.toByteArray();
			// Don't search in DB this memory stream before opening it
			BookInfo bookInfo = new BookInfo(fileInfo);
			log.v("posting LoadDocument task to background thread");
			BackgroundThread.instance().postBackground(() -> {
				if (!documentLoadLifecycle.isActive(loadOwner))
					return;
				log.v("posting LoadDocument task to GUI thread");
				BackgroundThread.instance().postGUI(() -> {
					if (!documentLoadLifecycle.isActive(loadOwner))
						return;
					log.v("synced posting LoadDocument task to GUI thread");
					enqueueDocumentLoad(
							loadOwner, bookInfo, source, docBuffer,
							null, null, doneHandler, errorHandler);
				});
			});
			return true;
		}
		if (documentLoadLifecycle.complete(loadOwner)
				&& errorHandler != null)
			errorHandler.run();
		return false;
	}

	public boolean loadDocumentFromFileDescriptor(final ParcelFileDescriptor pfd,
												 DocumentSource source,
												 final Runnable doneHandler,
												 final Runnable errorHandler) {
		return loadDocumentFromFileDescriptor(
				replaceDocumentLoad(), pfd, source,
				doneHandler, errorHandler);
	}

	public boolean loadDocumentFromFileDescriptor(
			final DocumentLoadLifecycle.Request loadOwner,
			final ParcelFileDescriptor pfd,
			DocumentSource source,
			final Runnable doneHandler,
			final Runnable errorHandler) {
		BackgroundThread.ensureGUI();
		save();
		String contentPath = source != null ? source.getIdentity() : null;
		log.i("loadDocumentFromFileDescriptor(" + safeDocumentPathForLog(contentPath) + ")");
		if (!documentLoadLifecycle.isActive(loadOwner)) {
			closeDescriptorQuietly(pfd);
			return false;
		}
		if (pfd == null || source == null
				|| contentPath == null || contentPath.length() == 0
				|| source.getFormat() == null) {
			if (pfd != null) {
				try {
					pfd.close();
				} catch (IOException ignored) {
				}
			}
			if (documentLoadLifecycle.complete(loadOwner)
					&& errorHandler != null)
				errorHandler.run();
			return false;
		}

		FileInfo sourceFileInfo = new FileInfo();
		sourceFileInfo.pathname = contentPath;
		sourceFileInfo.filename = source.getDisplayName();
		if (sourceFileInfo.filename == null || sourceFileInfo.filename.length() == 0)
			sourceFileInfo.filename = "document";
		sourceFileInfo.format = source.getFormat();
		sourceFileInfo.size = source.getSize() >= 0
				? source.getSize() : pfd.getStatSize();
		applySourceBookKeyIfMissing(sourceFileInfo, source);

		mHistory.getOrCreateBookInfo(
				mActivity.getDB(), sourceFileInfo, bookInfo -> {
					if (!documentLoadLifecycle.isActive(loadOwner)) {
						closeDescriptorQuietly(pfd);
						return;
					}
					if (bookInfo == null || bookInfo.getFileInfo() == null) {
						closeDescriptorQuietly(pfd);
						if (documentLoadLifecycle.complete(loadOwner)
								&& errorHandler != null)
							errorHandler.run();
						return;
					}
					enqueueFileDescriptorLoad(
							loadOwner, pfd, source, bookInfo,
							doneHandler, errorHandler);
				});
		return true;
	}

	private void enqueueFileDescriptorLoad(
			DocumentLoadLifecycle.Request loadOwner,
			ParcelFileDescriptor pfd, DocumentSource source, BookInfo bookInfo,
			Runnable doneHandler, Runnable errorHandler) {
		final String streamName = streamNameFor(source);
		BackgroundThread.instance().postBackground(() -> {
			if (!documentLoadLifecycle.isActive(loadOwner)) {
				closeDescriptorQuietly(pfd);
				return;
			}
			BackgroundThread.instance().postGUI(() -> {
				if (!documentLoadLifecycle.isActive(loadOwner)) {
					closeDescriptorQuietly(pfd);
					return;
				}
				enqueueDocumentLoad(
						loadOwner, bookInfo, source, null, pfd,
						streamName, doneHandler, errorHandler);
			});
		});
	}

	private boolean enqueueDocumentLoad(
			DocumentLoadLifecycle.Request loadOwner,
			BookInfo bookInfo, DocumentSource source, byte[] docBuffer,
			ParcelFileDescriptor parcelFileDescriptor, String streamName,
			Runnable doneHandler, Runnable errorHandler) {
		BackgroundThread.ensureGUI();
		if (!documentLoadLifecycle.isActive(loadOwner)
				|| bookInfo == null || bookInfo.getFileInfo() == null) {
			closeDescriptorQuietly(parcelFileDescriptor);
			if (documentLoadLifecycle.complete(loadOwner)
					&& errorHandler != null)
				errorHandler.run();
			return false;
		}
		// Queue the serialized native/cache close before the new load.
		closeCurrentDocument(false);
		post(new LoadDocumentTask(
				loadOwner, bookInfo, source, docBuffer,
				parcelFileDescriptor, streamName,
				doneHandler, errorHandler));
		return true;
	}

	private static void closeDescriptorQuietly(ParcelFileDescriptor descriptor) {
		if (descriptor == null)
			return;
		try {
			descriptor.close();
		} catch (IOException ignored) {
		}
	}

	private static String streamNameFor(DocumentSource source) {
		String displayName = source.getDisplayName();
		if (displayName == null || displayName.length() == 0)
			displayName = "document";
		if (DocumentFormat.byExtension(displayName) == source.getFormat())
			return displayName;
		String extension =
				source.getFormat().getPrimaryExtension();
		return extension != null ? displayName + extension : displayName;
	}

	private static String safeDocumentPathForLog(String path) {
		if (path == null)
			return "<null>";
		Uri uri = Uri.parse(path);
		if (ContentResolver.SCHEME_CONTENT.equalsIgnoreCase(uri.getScheme()))
			return uri.buildUpon().clearQuery().fragment(null).build().toString();
		return path;
	}

	public boolean loadDocument(
			DocumentSource initialSource, final Runnable doneHandler,
			final Runnable errorHandler) {
		return loadDocument(
				replaceDocumentLoad(), initialSource,
				doneHandler, errorHandler);
	}

	public boolean loadDocument(
			DocumentLoadLifecycle.Request loadOwner,
			DocumentSource initialSource,
			final Runnable doneHandler,
			final Runnable errorHandler) {
		BackgroundThread.ensureGUI();
		save();
		if (!documentLoadLifecycle.isActive(loadOwner))
			return false;
		if (initialSource == null) {
			log.v("loadDocument() : no document source specified");
			if (documentLoadLifecycle.complete(loadOwner)
					&& errorHandler != null)
				errorHandler.run();
			return false;
		}
		DocumentSource source = initialSource;
		String fileName;
		try {
			fileName = source.getLocalPath();
		} catch (IllegalStateException e) {
			log.e("ReaderView cannot directly open a non-local source", e);
			if (documentLoadLifecycle.complete(loadOwner)
					&& errorHandler != null)
				errorHandler.run();
			return false;
		}
		log.i("loadDocument(" + safeDocumentPathForLog(source.getIdentity()) + ")");
		String normalized = mEngine.getPathCorrector().normalizeIfPossible(fileName);
		if (normalized == null) {
			log.e("Trying to load book from non-standard path " + fileName);
			mActivity.showToast("Trying to load book from non-standard path " + fileName);
			hideProgress();
			if (documentLoadLifecycle.complete(loadOwner)
					&& errorHandler != null)
				errorHandler.run();
			return false;
		} else if (!normalized.equals(fileName)) {
			log.w("Filename normalized to " + normalized);
			fileName = normalized;
			source = source.withLocalPath(normalized);
		}
		String identity = source.getIdentity();
		BookInfo book = mHistory.getBookInfo(identity);
		if (book != null)
			log.v("loadDocument() : found book in history : " + book);
		FileInfo fi = null;
		if (book == null) {
			if (source.getKind() == DocumentSource.Kind.TEMPORARY_IMPORT) {
				fi = new FileInfo(fileName);
				fi.filename = source.getDisplayName();
				fi.format = source.getFormat();
				fi.size = source.getSize();
			}
			log.v("loadDocument() : book not found in history, looking for location directory");
			FileInfo dir = null;
			if (fi == null) {
				dir = mScanner.findParent(
						new FileInfo(fileName),
						mScanner.getRoot());
				if (dir != null) {
					log.v("loadDocument() : document location found : " + dir);
					fi = dir.findItemByPathName(fileName);
					log.v("loadDocument() : item inside location : " + fi);
				}
			}
			if (fi == null) {
				log.v("loadDocument() : no file item " + fileName + " found inside " + dir);
				if (documentLoadLifecycle.complete(loadOwner)
						&& errorHandler != null)
					errorHandler.run();
				return false;
			}
			if (fi.isDirectory) {
				log.v("loadDocument() : is a directory, opening browser");
				mActivity.showBrowser(fi);
				return true;
			}
		} else {
			fi = book.getFileInfo();
			log.v("loadDocument() : item from history : " + fi);
		}
		return loadDocument(
				loadOwner, fi, source, doneHandler, errorHandler);
	}

	private static void applySourceBookKeyIfMissing(
			FileInfo fileInfo, DocumentSource source) {
		if (fileInfo == null || source == null
				|| (fileInfo.bookKey != null
						&& fileInfo.sourceType != null
						&& fileInfo.sourceLocator != null))
			return;
		DocumentSource sizedSource = source.withMetadata(
				source.getDisplayName(), source.getMimeType(),
				fileInfo.size, source.getFormat());
		BookKey.fromDocumentSource(sizedSource).applyTo(fileInfo);
	}

	public BookInfo getBookInfo() {
		BackgroundThread.ensureGUI();
		return mBookInfo;
	}

	public List<SentenceInfo> getAllSentences() {
		return doc.getAllSentences();
	}

	private volatile BatteryStatus batteryStatus =
			BatteryStatus.fromRawLevel(
					BatteryStatus.STATE_DISCHARGING,
					BatteryStatus.CHARGER_NO,
					0,
					100);
	private final CloseableTaskGate timeTickLifecycle =
			new CloseableTaskGate();

	public void setBatteryStatus(BatteryStatus status) {
		if (status == null)
			throw new IllegalArgumentException("status must not be null");
		BatteryStatus previous = batteryStatus;
		if (status.equals(previous))
			return;
		if (status.getState() != previous.getState())
			log.i("Battery state changed: " + status.getState());
		if (status.getChargingConnection()
				!= previous.getChargingConnection()) {
			log.i("Battery charging connection changed: "
					+ status.getChargingConnection());
		}
		if (status.getChargeLevel() != previous.getChargeLevel())
			log.i("Battery charging level changed: "
					+ status.getChargeLevel());
		batteryStatus = status;
		if (!DeviceInfo.EINK_SCREEN && !isAutoScrollActive())
			redraw();
	}

	public int getBatteryState() {
		return batteryStatus.getState();
	}

	public int getBatteryChargingConnection() {
		return batteryStatus.getChargingConnection();
	}

	public int getBatteryChargeLevel() {
		return batteryStatus.getChargeLevel();
	}

	private void applyBatteryStatusToDocument() {
		BatteryStatus status = batteryStatus;
		doc.setBatteryState(
				status.getState(),
				status.getChargingConnection(),
				status.getChargeLevel());
	}

	public void onTimeTickReceived() {
		BackgroundThread.ensureGUI();
		if (DeviceInfo.EINK_SCREEN || isAutoScrollActive()) {
			timeTickLifecycle.cancel();
			return;
		}
		final CloseableTaskGate.Token owner =
				timeTickLifecycle.replace();
		final ReaderRenderRequest renderRequest =
				ReaderRenderRequest.capture(
						mBookInfo,
						documentLoadLifecycle);
		if (owner == null
				|| !isRenderRequestCurrent(renderRequest)) {
			timeTickLifecycle.complete(owner);
			return;
		}
		post(new Task() {
			boolean changed;

			@Override
			public void work() {
				BackgroundThread.ensureBackground();
				if (!timeTickLifecycle.isActive(owner)
						|| !readerNativeLifecycle.isInitialized()
						|| !isRenderRequestCurrent(
								renderRequest))
					return;
				changed = doc.isTimeChanged();
			}

			@Override
			public void done() {
				BackgroundThread.ensureGUI();
				if (!timeTickLifecycle.complete(owner)
						|| !isRenderRequestCurrent(
								renderRequest)
						|| !changed
						|| isAutoScrollActive()
						|| readerSurfaceState.isClosed())
					return;
				log.i("The current time has been changed "
						+ "(minutes), redrawing is scheduled.");
				surface.invalidate();
				pageInvalidationState.invalidate();
				drawPage(null, false, renderRequest);
			}

			@Override
			public void fail(Exception e) {
				BackgroundThread.ensureGUI();
				if (timeTickLifecycle.complete(owner)
						&& isRenderRequestCurrent(
								renderRequest))
					super.fail(e);
			}
		});
	}

	private final VMRuntimeHack runtime = new VMRuntimeHack();
	private final ReaderSurfaceMemoryState surfaceMemoryState =
			new ReaderSurfaceMemoryState();

	private static final class BitmapFactory {
		public static final int MAX_FREE_LIST_SIZE = 2;
		private final VMRuntimeHack runtime;
		private final ArrayList<Bitmap> freeList = new ArrayList<Bitmap>();
		private final ArrayList<Bitmap> usedList = new ArrayList<Bitmap>();

		BitmapFactory(VMRuntimeHack runtime) {
			this.runtime = runtime;
		}

		public synchronized Bitmap get(int dx, int dy) {
			for (int i = 0; i < freeList.size(); i++) {
				Bitmap bmp = freeList.get(i);
				if (bmp.getWidth() == dx && bmp.getHeight() == dy) {
					// found bitmap of proper size
					freeList.remove(i);
					usedList.add(bmp);
					//log.d("BitmapFactory: reused free bitmap, used list = " + usedList.size() + ", free list=" + freeList.size());
					return bmp;
				}
			}
			for (int i = freeList.size() - 1; i >= 0; i--) {
				Bitmap bmp = freeList.remove(i);
				runtime.trackAlloc(BitmapMemoryAccounting.bitmapBytes(
						bmp.getRowBytes(), bmp.getHeight()));
				//log.d("Recycling free bitmap "+bmp.getWidth()+"x"+bmp.getHeight());
				//bmp.recycle(); //20110109
			}
			Bitmap bmp = Bitmap.createBitmap(dx, dy, DeviceInfo.BUFFER_COLOR_FORMAT);
			runtime.trackFree(BitmapMemoryAccounting.bitmapBytes(
					bmp.getRowBytes(), bmp.getHeight()));
			//bmp.setDensity(0);
			usedList.add(bmp);
			//log.d("Created new bitmap "+dx+"x"+dy+". New bitmap list size = " + usedList.size());
			return bmp;
		}

		public synchronized void compact() {
			while (freeList.size() > 0) {
				//freeList.get(0).recycle();//20110109
				Bitmap bmp = freeList.remove(0);
				runtime.trackAlloc(BitmapMemoryAccounting.bitmapBytes(
						bmp.getRowBytes(), bmp.getHeight()));
			}
		}

		public synchronized void release(Bitmap bmp) {
			for (int i = 0; i < usedList.size(); i++) {
				if (usedList.get(i) == bmp) {
					freeList.add(bmp);
					usedList.remove(i);
					while (freeList.size() > MAX_FREE_LIST_SIZE) {
						//freeList.get(0).recycle(); //20110109
						Bitmap b = freeList.remove(0);
						runtime.trackAlloc(BitmapMemoryAccounting.bitmapBytes(
								b.getRowBytes(), b.getHeight()));
						//b.recycle();
					}
					log.d("BitmapFactory: bitmap released, used size = " + usedList.size() + ", free size=" + freeList.size());
					return;
				}
			}
			// unknown bitmap, just recycle
			//bmp.recycle();//20110109
		}
	}

	;
	private final BitmapFactory factory = new BitmapFactory(runtime);

	class BitmapInfo {
		Bitmap bitmap;
		PositionProperties position;
		ImageInfo imageInfo;

		void recycle() {
			pageBitmapLifetime.retire(this);
		}

		private synchronized void releaseNow() {
			if (bitmap == null)
				return;
			factory.release(bitmap);
			bitmap = null;
			position = null;
			imageInfo = null;
		}

		synchronized boolean isReleased() {
			return bitmap == null;
		}

		@Override
		public String toString() {
			return "BitmapInfo [position=" + position + "]";
		}

	}

	private final ReaderPageBitmapLifetime<BitmapInfo>
			pageBitmapLifetime =
					new ReaderPageBitmapLifetime<>(
							BitmapInfo::releaseNow);
	private BitmapInfo mCurrentPageInfo;
	private BitmapInfo mNextPageInfo;
	private final ReaderPageInvalidationState
			pageInvalidationState =
					new ReaderPageInvalidationState();

	private ViewportResizeState.Size ensureAppliedViewportSize() {
		BackgroundThread.ensureBackground();
		ViewportResizeState.Size applied =
				viewportResizeState.appliedSize();
		if (applied != null)
			return applied;
		ViewportResizeState.Size requested =
				viewportResizeState.requestedSize();
		log.d("Applying initial viewport size "
				+ requested.width() + ","
				+ requested.height());
		doc.resize(requested.width(), requested.height());
		viewportResizeState.publishApplied(requested);
		return requested;
	}

	/**
	 * Prepare and cache page image.
	 * Cache is represented by two slots: mCurrentPageInfo and mNextPageInfo.
	 * If page already exists in cache, returns it (if current page requested,
	 * ensures that it became stored as mCurrentPageInfo; if another page requested,
	 * no mCurrentPageInfo/mNextPageInfo reordering made).
	 *
	 * @param offset is kind of page: 0==current, -1=previous, 1=next page
	 * @return page image and properties, null if requested page is unavailable (e.g. requested next/prev page is out of document range)
	 */
	private BitmapInfo preparePageImage(
			int offset, ReaderRenderRequest renderRequest) {
		BackgroundThread.ensureBackground();
		if (!isRenderRequestCurrent(renderRequest))
			return null;
		log.v("preparePageImage( " + offset + ")");
		synchronized (documentLoadLifecycle) {
			if (!isRenderRequestCurrent(renderRequest))
				return null;
			if (pageInvalidationState.claim()) {
				synchronized (pageBitmapLifetime) {
					BitmapInfo current = mCurrentPageInfo;
					BitmapInfo next = mNextPageInfo;
					mCurrentPageInfo = null;
					mNextPageInfo = null;
					if (current != null)
						current.recycle();
					if (next != null && next != current)
						next.recycle();
				}
			}
		}

		ViewportResizeState.Size viewport =
				ensureAppliedViewportSize();
		if (!isRenderRequestCurrent(renderRequest))
			return null;

		ImageViewer imageViewer = imageViewerState.current();
		if (imageViewer != null && imageViewer.isActive()) {
			BitmapInfo image =
					imageViewer.prepareImage(
							renderRequest, viewport);
			return isRenderRequestCurrent(renderRequest)
					? image : null;
		}

		PositionProperties currpos = doc.getPositionProps(null, false);
		if (currpos == null
				|| !isRenderRequestCurrent(renderRequest))
			return null;

		boolean isPageView = currpos.pageMode != 0;

		BitmapInfo currposBitmap = null;
		if (mCurrentPageInfo != null && mCurrentPageInfo.position != null && mCurrentPageInfo.position.equals(currpos) && mCurrentPageInfo.imageInfo == null)
			currposBitmap = mCurrentPageInfo;
		else if (mNextPageInfo != null && mNextPageInfo.position != null && mNextPageInfo.position.equals(currpos) && mNextPageInfo.imageInfo == null)
			currposBitmap = mNextPageInfo;
		if (offset == 0) {
			// Current page requested
			if (currposBitmap != null) {
				synchronized (documentLoadLifecycle) {
					if (!isRenderRequestCurrent(
							renderRequest))
						return null;
					synchronized (pageBitmapLifetime) {
						if (mNextPageInfo == currposBitmap) {
							// reorder pages
							BitmapInfo tmp = mNextPageInfo;
							mNextPageInfo = mCurrentPageInfo;
							mCurrentPageInfo = tmp;
						}
						// found ready page image
						return mCurrentPageInfo;
					}
				}
			}
			BitmapInfo bi = new BitmapInfo();
			bi.position = currpos;
			bi.bitmap = factory.get(
					viewport.width(), viewport.height());
			applyBatteryStatusToDocument();
			doc.getPageImage(bi.bitmap);
			return publishCurrentPageCandidate(
					bi, renderRequest);
		}
		if (isPageView) {
			// PAGES: one of next or prev pages requested, offset is specified as param
			int cmd1 = offset > 0 ? ReaderCommand.DCMD_PAGEDOWN.nativeId : ReaderCommand.DCMD_PAGEUP.nativeId;
			int cmd2 = offset > 0 ? ReaderCommand.DCMD_PAGEUP.nativeId : ReaderCommand.DCMD_PAGEDOWN.nativeId;
			if (offset < 0)
				offset = -offset;
			if (!doc.doCommand(cmd1, offset))
				// cannot move to page: out of document range
				return null;
			BitmapInfo nextposBitmap = null;
			BitmapInfo candidate = null;
			try {
				// can move to next page
				if (isRenderRequestCurrent(renderRequest)) {
					PositionProperties nextpos =
							doc.getPositionProps(null, false);
					if (nextpos != null
							&& isRenderRequestCurrent(
									renderRequest)) {
						if (mCurrentPageInfo != null
								&& mCurrentPageInfo.position != null
								&& mCurrentPageInfo.position.equals(nextpos))
							nextposBitmap = mCurrentPageInfo;
						else if (mNextPageInfo != null
								&& mNextPageInfo.position != null
								&& mNextPageInfo.position.equals(nextpos))
							nextposBitmap = mNextPageInfo;
						if (nextposBitmap == null) {
							candidate = new BitmapInfo();
							candidate.position = nextpos;
							candidate.bitmap =
									factory.get(
											viewport.width(),
											viewport.height());
							applyBatteryStatusToDocument();
							doc.getPageImage(candidate.bitmap);
						}
					}
				}
			} finally {
				// return back to previous page even after replacement
				doc.doCommand(cmd2, offset);
			}
			if (candidate != null) {
				return publishNextPageCandidate(
						candidate, renderRequest);
			}
			return isRenderRequestCurrent(renderRequest)
					? nextposBitmap : null;
		} else {
			// SCROLL next or prev page requested, with pixel offset specified
			int y = currpos.y + offset;
			if (!doc.doCommand(
					ReaderCommand.DCMD_GO_POS.nativeId, y))
				return null;
			BitmapInfo nextposBitmap = null;
			BitmapInfo candidate = null;
			try {
				if (isRenderRequestCurrent(renderRequest)) {
					PositionProperties nextpos =
							doc.getPositionProps(null, false);
					if (nextpos != null
							&& isRenderRequestCurrent(
									renderRequest)) {
						if (mCurrentPageInfo != null
								&& mCurrentPageInfo.position != null
								&& mCurrentPageInfo.position.equals(nextpos))
							nextposBitmap = mCurrentPageInfo;
						else if (mNextPageInfo != null
								&& mNextPageInfo.position != null
								&& mNextPageInfo.position.equals(nextpos))
							nextposBitmap = mNextPageInfo;
						if (nextposBitmap == null) {
							candidate = new BitmapInfo();
							candidate.position = nextpos;
							candidate.bitmap =
									factory.get(
											viewport.width(),
											viewport.height());
							applyBatteryStatusToDocument();
							doc.getPageImage(candidate.bitmap);
						}
					}
				}
			} finally {
				// return back to previous position even after replacement
				doc.doCommand(
						ReaderCommand.DCMD_GO_POS.nativeId,
						currpos.y);
			}
			if (candidate != null) {
				return publishNextPageCandidate(
						candidate, renderRequest);
			}
			return isRenderRequestCurrent(renderRequest)
					? nextposBitmap : null;
		}

	}

	private BitmapInfo publishCurrentPageCandidate(
			BitmapInfo candidate,
			ReaderRenderRequest renderRequest) {
		synchronized (documentLoadLifecycle) {
			if (!isRenderRequestCurrent(renderRequest)) {
				candidate.recycle();
				return null;
			}
			synchronized (pageBitmapLifetime) {
				BitmapInfo previous = mCurrentPageInfo;
				mCurrentPageInfo = candidate;
				if (previous != null
						&& previous != candidate
						&& previous != mNextPageInfo)
					previous.recycle();
				return candidate;
			}
		}
	}

	private BitmapInfo publishNextPageCandidate(
			BitmapInfo candidate,
			ReaderRenderRequest renderRequest) {
		synchronized (documentLoadLifecycle) {
			if (!isRenderRequestCurrent(renderRequest)) {
				candidate.recycle();
				return null;
			}
			synchronized (pageBitmapLifetime) {
				BitmapInfo previous = mNextPageInfo;
				mNextPageInfo = candidate;
				if (previous != null
						&& previous != candidate
						&& previous != mCurrentPageInfo)
					previous.recycle();
				return candidate;
			}
		}
	}

	private final CloseableTaskGate drawTaskLifecycle =
			new CloseableTaskGate();

	private class DrawPageTask extends Task {
		private final CloseableTaskGate.Token owner;
		private final ReaderRenderRequest renderRequest;
		private BitmapInfo bi;
		private final Runnable doneHandler;
		private final boolean isPartially;

		DrawPageTask(
				Runnable doneHandler,
				boolean isPartially,
				ReaderRenderRequest renderRequest) {
//			// DEBUG stack trace
//			try {
//				throw new Exception("DrawPageTask() stack trace");
//			} catch (Exception e) {
//				Log.d("cr3", "stack trace", e);
//			}
			this.owner = drawTaskLifecycle.replace();
			this.renderRequest = renderRequest;
			this.doneHandler = doneHandler;
			this.isPartially = isPartially;
			if (owner != null)
				cancelGc();
		}

		public void work() {
			BackgroundThread.ensureBackground();
			if (!drawTaskLifecycle.isActive(owner)
					|| !isRenderRequestCurrent(renderRequest)) {
				log.d("skipping duplicate drawPage request");
				return;
			}
			invalidateTapHighlight();
			if (animationState.current() != null) {
				log.d("skipping drawPage request while scroll animation is in progress");
				return;
			}
			ViewportResizeState.Size viewport =
					viewportResizeState
							.appliedOrRequestedSize();
			log.e("DrawPageTask.work("
					+ viewport.width() + ","
					+ viewport.height() + ")");
			bi = preparePageImage(0, renderRequest);
			if (bi != null
					&& drawTaskLifecycle.isActive(owner)
					&& isRenderRequestCurrent(renderRequest)) {
				bookView.draw(isPartially);
			}
		}

		@Override
		public void done() {
			BackgroundThread.ensureGUI();
			boolean ownsRenderCompletion =
					drawTaskLifecycle.complete(owner);
			boolean ownsDocument =
					isRenderRequestCurrent(renderRequest);
//			log.d("drawPage : bitmap is ready, invalidating view to draw new bitmap");
//			if ( bi!=null ) {
//				setBitmap( bi.bitmap );
//				invalidate();
//			}
//    		if (mOpened)
			//hideProgress();
			if (ownsRenderCompletion && ownsDocument)
				scheduleGc();
			if (doneHandler != null
					&& ownsDocument
					&& !drawTaskLifecycle.isClosed()
					&& mServiceLifecycle.isActive())
				doneHandler.run();
		}

		@Override
		public void fail(Exception e) {
			if (drawTaskLifecycle.complete(owner)
					&& isRenderRequestCurrent(renderRequest)) {
				hideProgress();
				scheduleGc();
			}
		}
	}

	;

	static class ReaderSurfaceView extends SurfaceView {
		public ReaderSurfaceView(Context context) {
			super(context);
		}
	}

	//	private boolean mIsOnFront = false;
	private final ViewportResizeState viewportResizeState =
			new ViewportResizeState(100, 100);
	private final DelayedExecutor resizeScheduler =
			DelayedExecutor.createGUI("viewport-resize");
//	public void setOnFront(boolean front) {
//		if (mIsOnFront == front)
//			return;
//		mIsOnFront = front;
//		log.d("setOnFront(" + front + ")");
//		if (mIsOnFront) {
//			checkSize();
//		} else {
//			// save position immediately
//			scheduleSaveCurrentPositionBookmark(0);
//		}
//	}

	private void requestResize(int width, int height) {
		scheduleResize(
				viewportResizeState.request(width, height));
	}

	private void checkSize() {
		if (viewportResizeState.requestedIsApplied())
			return;
		scheduleResize(viewportResizeState.requestCurrent());
	}

	private void scheduleResize(
			ViewportResizeState.Request request) {
		if (request == null)
			return;
		if (viewportResizeState.completeIfApplied(request))
			return;
		if (getActivity().isDialogActive()) {
			log.d("checkSize() : dialog is active, skipping resize");
			return;
		}
		log.d("checkSize() : calling resize");
		resize(request);
	}

	private void resize(
			ViewportResizeState.Request request) {
//	    if ( w<h && mActivity.isLandscape() ) {
//	    	log.i("ignoring size change to portrait since landscape is set");
//	    	return;
//	    }
//		if ( mActivity.isPaused() ) {
//			log.i("ignoring size change since activity is paused");
//			return;
//		}
		// update size with delay: chance to avoid extra unnecessary resizing

		Runnable task = () -> {
			if (!viewportResizeState.isCurrent(request)) {
				log.d("skipping duplicate resize request in GUI thread");
				return;
			}
			post(new Task() {
				private boolean applied;

				public void work() {
					BackgroundThread.ensureBackground();
					if (!viewportResizeState.beginApply(request)) {
						log.d("skipping duplicate resize request");
						return;
					}
					ViewportResizeState.Size requested =
							request.size();
					log.d("ResizeTask: resizeInternal("
							+ requested.width() + ","
							+ requested.height() + ")");
					doc.resize(
							requested.width(),
							requested.height());
					applied =
							viewportResizeState.finishApply(
									request);
//	    		        if ( mOpened ) {
//	    					log.d("ResizeTask: done, drawing page");
//	    			        drawPage();
//	    		        }
				}

				public void done() {
					if (!applied
							|| !viewportResizeState
									.completeCurrentApplied())
						return;
					clearImageCache();
					drawPage(null, false);
					//redraw();
				}

				@Override
				public void fail(Exception e) {
					viewportResizeState.cancelApply(request);
					viewportResizeState.complete(request);
					super.fail(e);
				}
			});
		};

		int delay = viewportResizeState.resizeDelayMillis(
				android.os.SystemClock.uptimeMillis());

		if (mOpened) {
			log.d("scheduling delayed resize task for "
					+ delay + " ms");
			synchronized (viewportResizeState) {
				if (viewportResizeState.isCurrent(request))
					resizeScheduler.postDelayed(task, delay);
			}
		} else {
			log.d("executing resize without delay");
			task.run();
		}
	}

	private void applyEinkFocusRefresh(
			ReaderSurfaceState.FocusRefresh refresh) {
		BackgroundThread.ensureGUI();
		if (readerSurfaceState.claimFocusRefresh(refresh)
				&& mServiceLifecycle.isActive())
			mEinkScreen.refreshScreen(surface);
	}

	private void refreshEinkScreenIfReady() {
		BackgroundThread.ensureGUI();
		if (DeviceInfo.EINK_SCREEN
				&& readerSurfaceState.isDrawable()
				&& mServiceLifecycle.isActive())
			mEinkScreen.refreshScreen(surface);
	}

	// SurfaceView callbacks
	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, final int width,
							   final int height) {
		if (readerSurfaceState.isClosed())
			return;
		log.i("surfaceChanged(" + width + ", " + height + ")");

		applySurfaceMemoryChange(
				surfaceMemoryState.resize(width, height));

		surface.invalidate();
		//if (!isProgressActive())
		bookView.draw();
		//requestResize(width, height);
		//draw();
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		log.i("surfaceCreated()");
		if (readerSurfaceState.markSurfaceCreated()) {
			einkRefreshScheduler.cancel();
			refreshEinkScreenIfReady();
		}
		//draw();
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		log.i("surfaceDestroyed()");
		readerSurfaceState.markSurfaceDestroyed();
		einkRefreshScheduler.cancel();
		applySurfaceMemoryChange(
				surfaceMemoryState.clear());
	}

	private void applySurfaceMemoryChange(
			ReaderSurfaceMemoryState.Change change) {
		if (change == null)
			return;
		if (change.releasedBytes() > 0)
			runtime.trackAlloc(change.releasedBytes());
		if (change.acquiredBytes() > 0)
			runtime.trackFree(change.acquiredBytes());
	}

	enum AnimationType {
		SCROLL, // for scroll mode
		PAGE_SHIFT, // for simple page shift
	}


	private final ReaderAnimationState<
			ViewAnimationControl, AnimationUpdate> animationState =
				new ReaderAnimationState<>();

	private void cancelDocumentAnimation() {
		animationScheduler.cancel();
		animationState.reset();
	}

	private final ReaderPageAnimationState pageAnimationState =
			new ReaderPageAnimationState(
					PAGE_ANIMATION_NONE,
					PAGE_ANIMATION_SLIDE2,
					PAGE_ANIMATION_NONE,
					PAGE_ANIMATION_MAX,
					DEF_PAGE_FLIP_MS);

	//	private void animatePageFlip( final int dir ) {
//		animatePageFlip(dir, null);
//	}
	private void animatePageFlip(
			final int dir,
			final Runnable onFinishHandler,
			final ReaderPageAnimationState.Snapshot
					animationSettings) {
		final BookInfo expectedBook = mBookInfo;
		final DocumentLoadLifecycle.Interaction interaction =
				documentLoadLifecycle.interaction();
		if (!isDocumentInteractionCurrent(
				expectedBook, interaction))
			return;
		BackgroundThread.instance().executeBackground(() -> {
			BackgroundThread.ensureBackground();
			if (isDocumentInteractionCurrent(
					expectedBook, interaction)
					&& animationState.current() == null) {
				PositionProperties currPos = doc.getPositionProps(null, false);
				if (currPos == null)
					return;
				if (mCurrentPageInfo == null)
					return;
				int w = currPos.pageWidth;
				int h = currPos.pageHeight;
				int dir2 = dir;
//					if ( currPos.pageMode==2 )
//						if ( dir2==1 )
//							dir2 = 2;
//						else if ( dir2==-1 )
//							dir2 = -2;
				int speed = animationSettings.durationMs();
				if (onFinishHandler != null)
					speed = animationSettings.durationMs() / 2;
				if (currPos.pageMode != 0) {
					int fromX = dir2 > 0 ? w : 0;
					int toX = dir2 > 0 ? 0 : w;
					ViewAnimationControl animation =
							new PageViewAnimation(
									fromX, w, dir2,
									expectedBook, interaction,
									animationSettings);
					if (animationState.isCurrent(animation)
							&& isDocumentInteractionCurrent(
									expectedBook, interaction)) {
						invalidateTapHighlight();
						animation.update(toX, h / 2);
						animation.move(speed, true);
						animation.stop(-1, -1);
						if (onFinishHandler != null)
							BackgroundThread.instance().executeGUI(() -> {
								if (isDocumentInteractionCurrent(
										expectedBook, interaction))
									onFinishHandler.run();
							});
					} else {
						((ViewAnimationBase) animation).close();
					}
				} else {
					ViewAnimationControl animation =
							new ScrollViewAnimation(
									dir > 0 ? h * 7 / 8 : -(h * 7 / 8),
									expectedBook, interaction,
									animationSettings);
					if (animationState.isCurrent(animation)
							&& isDocumentInteractionCurrent(
									expectedBook, interaction)) {
						invalidateTapHighlight();
						animation.move(speed, true);
						animation.stop(-1, -1);
						if (onFinishHandler != null)
							BackgroundThread.instance().executeGUI(() -> {
								if (isDocumentInteractionCurrent(
										expectedBook, interaction))
									onFinishHandler.run();
							});
					} else {
						((ViewAnimationBase) animation).close();
					}
				}
			}
		});
	}

	private final static int HILITE_RECT_ALPHA = 64;
	private final TapHighlightState tapHighlightState =
			new TapHighlightState();
	private final DelayedExecutor tapHighlightScheduler =
			DelayedExecutor.createGUI("tap-highlight");

	private void unhiliteTapZone() {
		TapHighlightState.Hide hide;
		synchronized (tapHighlightState) {
			hide = tapHighlightState.requestHideAll();
			tapHighlightScheduler.cancel();
		}
		applyTapHighlightHide(hide);
	}

	private TapHighlightState.Show showTapHighlight(
			final int startX,
			final int startY,
			final int maxX,
			final int maxY) {
		final ReaderRenderRequest renderRequest =
				ReaderRenderRequest.capture(
						mBookInfo, documentLoadLifecycle);
		if (!isRenderRequestCurrent(renderRequest))
			return null;
		alog.d("highliteTapZone(" + startX + ", " + startY + ")");
		int txcolor = readerSettingsState.getColor(
				PROP_FONT_COLOR, Color.BLACK);
		final int color = (txcolor & 0xFFFFFF) | (HILITE_RECT_ALPHA << 24);
		TapHighlightState.Show show;
		synchronized (tapHighlightState) {
			show = tapHighlightState.requestShow(
					TapZoneGeometry.boundsAt(
							startX, startY, maxX, maxY),
					color);
			tapHighlightScheduler.cancel();
		}
		if (show == null)
			return null;
		BackgroundThread.instance().executeBackground(() -> {
			if (!tapHighlightState.isCurrent(show)
					|| !isRenderRequestCurrent(
							renderRequest))
				return;

			if (isAutoScrollActive()) {
				invalidateTapHighlight();
				return;
			}

			BackgroundThread.ensureBackground();
			final BitmapInfo pageImage =
					preparePageImage(0, renderRequest);
			if (pageImage != null && pageImage.bitmap != null && pageImage.position != null) {
				TapHighlightState.Transition transition =
						tapHighlightState.applyShow(show);
				drawTapHighlightTransition(
						transition, renderRequest);
			} else {
				invalidateTapHighlight();
			}
		});
		return show;
	}

	private void scheduleUnhilite(
			TapHighlightState.Show owner,
			int delay) {
		if (owner == null)
			return;
		synchronized (tapHighlightState) {
			if (tapHighlightState.isCurrent(owner)) {
				tapHighlightScheduler.postDelayed(
						() -> unhiliteTapZone(owner),
						delay);
			}
		}
	}

	private void unhiliteTapZone(
			TapHighlightState.Show owner) {
		TapHighlightState.Hide hide;
		synchronized (tapHighlightState) {
			hide = tapHighlightState.requestOwnedHide(owner);
			if (hide != null)
				tapHighlightScheduler.cancel();
		}
		applyTapHighlightHide(hide);
	}

	private void applyTapHighlightHide(
			TapHighlightState.Hide hide) {
		if (hide == null)
			return;
		final ReaderRenderRequest renderRequest =
				ReaderRenderRequest.capture(
						mBookInfo, documentLoadLifecycle);
		BackgroundThread.instance().executeBackground(() -> {
			TapHighlightState.Transition transition =
					tapHighlightState.applyHide(hide);
			if (transition == null
					|| !transition.hasVisualChange()
					|| !isRenderRequestCurrent(
							renderRequest))
				return;
			BackgroundThread.ensureBackground();
			BitmapInfo pageImage =
					preparePageImage(0, renderRequest);
			if (pageImage != null
					&& pageImage.bitmap != null
					&& pageImage.position != null) {
				drawTapHighlightTransition(
						transition, renderRequest);
			}
		});
	}

	private void drawTapHighlightTransition(
			TapHighlightState.Transition transition,
			ReaderRenderRequest renderRequest) {
		if (transition == null
				|| !transition.hasVisualChange()
				|| !isRenderRequestCurrent(
						renderRequest))
			return;
		Rect dirty = tapHighlightDirtyRect(transition);
		if (dirty == null || dirty.isEmpty())
			return;
		drawCallback(canvas -> {
			BitmapInfo currentPage;
			synchronized (pageBitmapLifetime) {
				currentPage = mCurrentPageInfo;
			}
			if (!readerNativeLifecycle.isInitialized()
					|| !isRenderRequestCurrent(
							renderRequest)
					|| currentPage == null
					|| currentPage.bitmap == null)
				return;
			log.d("onDraw() -- drawing page image");
			Rect dst =
					new Rect(
							0, 0,
							canvas.getWidth(),
							canvas.getHeight());
			Rect src =
					new Rect(
							0, 0,
							currentPage.bitmap.getWidth(),
							currentPage.bitmap.getHeight());
			drawDimmedBitmap(
					canvas,
					currentPage.bitmap,
					src,
					dst);
			TapHighlightState.Show current =
					transition.current();
			if (current != null
					&& tapHighlightState.isVisible(current)) {
				drawTapHighlightBorder(canvas, current);
			}
		}, dirty, false);
	}

	private void drawTapHighlightBorder(
			Canvas canvas,
			TapHighlightState.Show show) {
		Rect bounds = tapHighlightRect(show);
		Paint paint = new Paint();
		paint.setStyle(Paint.Style.FILL);
		paint.setColor(show.color());
		int width =
				Math.max(
						1,
						(int) (2.0f
								* mActivity.getDensityFactor()));
		canvas.drawRect(
				new Rect(
						bounds.left,
						bounds.top,
						bounds.right - width,
						bounds.top + width),
				paint);
		canvas.drawRect(
				new Rect(
						bounds.left,
						bounds.top + width,
						bounds.left + width,
						bounds.bottom - width),
				paint);
		canvas.drawRect(
				new Rect(
						bounds.right - width - width,
						bounds.top + width,
						bounds.right - width,
						bounds.bottom - width),
				paint);
		canvas.drawRect(
				new Rect(
						bounds.left + width,
						bounds.bottom - width - width,
						bounds.right - width - width,
						bounds.bottom - width),
				paint);
	}

	private static Rect tapHighlightDirtyRect(
			TapHighlightState.Transition transition) {
		Rect dirty = null;
		if (transition.previous() != null)
			dirty = tapHighlightRect(transition.previous());
		if (transition.current() != null) {
			Rect current =
					tapHighlightRect(transition.current());
			if (dirty == null)
				dirty = current;
			else
				dirty.union(current);
		}
		return dirty;
	}

	private static Rect tapHighlightRect(
			TapHighlightState.Show show) {
		TapZoneGeometry.Bounds bounds = show.bounds();
		return new Rect(
				bounds.left(),
				bounds.top(),
				bounds.right(),
				bounds.bottom());
	}

	private void invalidateTapHighlight() {
		synchronized (tapHighlightState) {
			tapHighlightState.invalidate();
			tapHighlightScheduler.cancel();
		}
	}

	private void closeTapHighlight() {
		synchronized (tapHighlightState) {
			tapHighlightState.close();
			tapHighlightScheduler.cancel();
		}
	}

	int currentBrightnessValueIndex = -1;
	int currentBrightnessValue = -1;
	int currentBrightnessWarmValueIndex = -1;
	int currentBrightnessWarmValue = -1;
	int currentBrightnessPrevYPos = -1;

	private void startBrightnessControl(final int startX, final int startY, int type) {
		switch (type) {
			case BRIGHTNESS_TYPE_COMMON:
				currentBrightnessValue = mActivity.getScreenBacklightLevel();
				if (!DeviceInfo.EINK_SCREEN) {
					currentBrightnessValueIndex =
							BacklightOptions.nearestIndex(
									currentBrightnessValue);
					if (0 == currentBrightnessValueIndex) {		// system backlight level
						// A trick that allows you to reduce the brightness of the backlight
						// if the brightness is set to the same as in the system.
						currentBrightnessValue = 50;
						currentBrightnessValueIndex =
								BacklightOptions.nearestIndex(
										currentBrightnessValue);
					}
				} else if (DeviceInfo.EINK_HAVE_FRONTLIGHT)
					currentBrightnessValueIndex = Utils.findNearestIndex(mEinkScreen.getFrontLightLevels(mActivity), currentBrightnessValue);
				break;
			case BRIGHTNESS_TYPE_BOTH:
				// only for e-ink
				currentBrightnessValue = mActivity.getScreenBacklightLevel();
				currentBrightnessWarmValue = mActivity.getWarmBacklightLevel();
				if (DeviceInfo.EINK_HAVE_FRONTLIGHT)
					currentBrightnessValueIndex = Utils.findNearestIndex(mEinkScreen.getFrontLightLevels(mActivity), currentBrightnessValue);
				if (DeviceInfo.EINK_HAVE_NATURAL_BACKLIGHT)
					currentBrightnessWarmValueIndex = Utils.findNearestIndex(mEinkScreen.getWarmLightLevels(mActivity), currentBrightnessWarmValue);
				break;
			case BRIGHTNESS_TYPE_WARM:
				currentBrightnessWarmValue = mActivity.getWarmBacklightLevel();
				if (DeviceInfo.EINK_HAVE_NATURAL_BACKLIGHT)
					currentBrightnessWarmValueIndex = Utils.findNearestIndex(mEinkScreen.getWarmLightLevels(mActivity), currentBrightnessWarmValue);
				break;
			default:
				return;
		}
		currentBrightnessPrevYPos = startY;
		updateBrightnessControl(startX, startY, type);
	}

	private void updateBrightnessControl(final int x, final int y, int type) {
		List<Integer> levelList = null;
		List<Integer> levelWarmList = null;
		int count = 0;
		int countWarm = 0;
		switch (type) {
			case BRIGHTNESS_TYPE_COMMON:
				if (!DeviceInfo.EINK_SCREEN)
					count = BacklightOptions.size();
				else if (null != mEinkScreen) {
					levelList = mEinkScreen.getFrontLightLevels(mActivity);
					if (null != levelList)
						count = levelList.size();
					else
						return;
				}
				break;
			case BRIGHTNESS_TYPE_BOTH:
				// only for e-ink
				if (null != mEinkScreen) {
					levelList = mEinkScreen.getFrontLightLevels(mActivity);
					if (null != levelList)
						count = levelList.size();
					else
						return;
					levelWarmList = mEinkScreen.getWarmLightLevels(mActivity);
					if (null != levelWarmList)
						countWarm = levelWarmList.size();
					else
						return;
				}
				break;
			case BRIGHTNESS_TYPE_WARM:
				if (null != mEinkScreen) {
					levelWarmList = mEinkScreen.getWarmLightLevels(mActivity);
					if (null != levelWarmList)
						countWarm = levelWarmList.size();
					else
						return;
				}
				break;
			default:
				return;
		}
		int index = currentBrightnessValueIndex;
		if (count > 0) {
			int diff = count * (currentBrightnessPrevYPos - y) / surface.getHeight();
			index += diff;
			if (index < 0)
				index = 0;
			else if (index >= count)
				index = count - 1;
			if (!DeviceInfo.EINK_SCREEN) {
				if (index == 0) {
					// ignore system brightness level on non eink devices
					currentBrightnessPrevYPos = y;
					return;
				}
			}
		}
		int indexWarm = currentBrightnessWarmValueIndex;
		if (countWarm > 0) {
			int diffWarm = countWarm * (currentBrightnessPrevYPos - y) / surface.getHeight();
			indexWarm += diffWarm;
			if (indexWarm < 0)
				indexWarm = 0;
			else if (indexWarm >= countWarm)
				indexWarm = countWarm - 1;
		}
		if (index != currentBrightnessValueIndex || indexWarm != currentBrightnessWarmValueIndex) {
			currentBrightnessValueIndex = index;
			currentBrightnessWarmValueIndex = indexWarm;
			switch (type) {
				case BRIGHTNESS_TYPE_COMMON:
					if (!DeviceInfo.EINK_SCREEN)
						currentBrightnessValue = BacklightOptions.valueAt(
								currentBrightnessValueIndex);
					else {
						// Here levelList already != null
						currentBrightnessValue = levelList.get(currentBrightnessValueIndex);
					}
					log.e("C: setScreenBacklightLevel()");
					mActivity.setScreenBacklightLevel(currentBrightnessValue);
					break;
				case BRIGHTNESS_TYPE_BOTH:
					// only for e-ink
					if (DeviceInfo.EINK_SCREEN) {
						// Here levelList already != null
						currentBrightnessValue = levelList.get(currentBrightnessValueIndex);
						currentBrightnessWarmValue = levelWarmList.get(currentBrightnessWarmValueIndex);
						log.e("B: setScreenBacklightLevel()");
						mActivity.setScreenBacklightLevel(currentBrightnessValue);
						mActivity.setScreenWarmBacklightLevel(currentBrightnessWarmValue);
					}
					break;
				case BRIGHTNESS_TYPE_WARM:
					if (DeviceInfo.EINK_SCREEN) {
						// Here levelList already != null
						currentBrightnessWarmValue = levelWarmList.get(currentBrightnessWarmValueIndex);
						log.e("W: setScreenWarmBacklightLevel()");
						mActivity.setScreenWarmBacklightLevel(currentBrightnessWarmValue);
					}
					break;
			}
			currentBrightnessPrevYPos = y;
		}
	}

	private void stopBrightnessControl(final int x, final int y, int type) {
		if (currentBrightnessValueIndex >= 0 || currentBrightnessWarmValueIndex >= 0) {
			if (x >= 0 && y >= 0) {
				updateBrightnessControl(x, y, type);
			}
			Properties updatedSettings =
					readerSettingsState.copy();
			switch (type) {
				case BRIGHTNESS_TYPE_COMMON:
					updatedSettings.setInt(
							PROP_APP_SCREEN_BACKLIGHT,
							currentBrightnessValue);
					break;
				case BRIGHTNESS_TYPE_BOTH:
					updatedSettings.setInt(
							PROP_APP_SCREEN_BACKLIGHT,
							currentBrightnessValue);
					updatedSettings.setInt(
							PROP_APP_SCREEN_WARM_BACKLIGHT,
							currentBrightnessWarmValue);
					break;
				case BRIGHTNESS_TYPE_WARM:
					updatedSettings.setInt(
							PROP_APP_SCREEN_WARM_BACKLIGHT,
							currentBrightnessWarmValue);
					break;
				default:
					return;
			}
			ReaderSettingsState.Snapshot publishedSettings =
					readerSettingsState.replace(
							updatedSettings);
			if (showBrightnessFlickToast && currentBrightnessValueIndex >= 0) {
				String s = BacklightOptions.titleAt(
						currentBrightnessValueIndex,
						mActivity.getString(
								R.string.options_app_backlight_screen_default));
				mActivity.showToast(s);
			}
			if (!DeviceInfo.EINK_SCREEN)
				saveSettings(publishedSettings.copy());
			currentBrightnessValue = -1;
			currentBrightnessWarmValue = -1;
			currentBrightnessValueIndex = -1;
			currentBrightnessWarmValueIndex = -1;
			currentBrightnessPrevYPos = -1;
		}
	}

	private static final boolean showBrightnessFlickToast = false;


	private void startAnimation(
			final int startX,
			final int startY,
			final int maxX,
			final int maxY,
			final int newX,
			final int newY,
			final ReaderPageAnimationState.Snapshot
					animationSettings) {
		final BookInfo expectedBook = mBookInfo;
		final DocumentLoadLifecycle.Interaction interaction =
				documentLoadLifecycle.interaction();
		if (!isDocumentInteractionCurrent(
				expectedBook, interaction))
			return;
		alog.d("startAnimation(" + startX + ", " + startY + ")");
		BackgroundThread.instance().executeBackground(() -> {
			BackgroundThread.ensureBackground();
			if (!isDocumentInteractionCurrent(
					expectedBook, interaction))
				return;
			if (animationState.current() != null)
				return;
			PositionProperties currPos = doc.getPositionProps(null, false);
			ViewAnimationControl animation;
			if (currPos == null) {
				return;
			} else if (currPos.pageMode != 0) {
				//int dir = startX > maxX/2 ? currPos.pageMode : -currPos.pageMode;
				//int dir = startX > maxX/2 ? 1 : -1;
				int dir = newX - startX < 0 ? 1 : -1;
				int sx = startX;
//					if ( dir<0 )
//						sx = 0;
				animation = new PageViewAnimation(
						sx, maxX, dir,
						expectedBook, interaction,
						animationSettings);
			} else {
				int dir = newX < startX || newY < startY ? -1 : 1;
				animation = new ScrollViewAnimation(
						dir * currPos.pageHeight * 7 / 8,
						expectedBook, interaction,
						animationSettings);
			}
			if (animationState.isCurrent(animation)
					&& isDocumentInteractionCurrent(
							expectedBook, interaction)) {
				invalidateTapHighlight();
			} else {
				((ViewAnimationBase) animation).close();
			}
		});
	}

	private class AnimationUpdate {
		private int x;
		private int y;
		private final BookInfo expectedBook;
		private final DocumentLoadLifecycle.Interaction interaction;

		public void set(int x, int y) {
			this.x = x;
			this.y = y;
		}

		public AnimationUpdate(int x, int y) {
			this.x = x;
			this.y = y;
			this.expectedBook = mBookInfo;
			this.interaction = documentLoadLifecycle.interaction();
		}

		private void scheduleUpdate() {
			BackgroundThread.instance().postBackground(() -> {
				ViewAnimationControl animation = null;
				synchronized (animationState) {
					alog.d("updating(" + x + ", " + y + ")");
					ViewAnimationControl candidate =
							animationState.current();
					if (candidate != null
							&& animationState.isPendingUpdate(
									AnimationUpdate.this)
							&& isDocumentInteractionCurrent(
									expectedBook, interaction)) {
						animation = candidate;
						animationState.clearPendingUpdate(
								AnimationUpdate.this);
						animation.update(x, y);
					} else {
						animationState.clearPendingUpdate(
								AnimationUpdate.this);
					}
				}
				if (animation != null)
					animation.animate();
			});
		}

	}

	private void updateAnimation(final int x, final int y) {
		if (!mOpened)
			return;
		alog.d("updateAnimation(" + x + ", " + y + ")");
		AnimationUpdate update;
		boolean schedule = false;
		synchronized (animationState) {
			update = animationState.pendingUpdate();
			if (update != null) {
				update.set(x, y);
			} else {
				update = new AnimationUpdate(x, y);
				schedule =
						animationState.installPendingUpdate(update);
			}
		}
		if (schedule)
			update.scheduleUpdate();
		try {
			// give a chance to background thread to process event faster
			Thread.sleep(0);
		} catch (InterruptedException e) {
			// ignore
		}
	}

	private void stopAnimation(final int x, final int y) {
		final BookInfo expectedBook = mBookInfo;
		final DocumentLoadLifecycle.Interaction interaction =
				documentLoadLifecycle.interaction();
		if (!isDocumentInteractionCurrent(
				expectedBook, interaction))
			return;
		alog.d("stopAnimation(" + x + ", " + y + ")");
		BackgroundThread.instance().executeBackground(() -> {
			ViewAnimationControl animation =
					animationState.current();
			if (animation != null
					&& isDocumentInteractionCurrent(
							expectedBook, interaction))
				animation.stop(x, y);
		});
	}

	private final DelayedExecutor animationScheduler =
			DelayedExecutor.createBackground("animation");

	private void scheduleAnimation(
			final ViewAnimationControl animation) {
		animationScheduler.post(() -> {
			if (animationState.isCurrent(animation))
				animation.animate();
		});
	}

	interface ViewAnimationControl {
		public void update(int x, int y);

		public void stop(int x, int y);

		public void animate();

		public void move(int duration, boolean accelerated);

		public boolean isStarted();

		abstract void draw(Canvas canvas);
	}

//	private Object surfaceLock = new Object();

	private final GestureAcceleration gestureAcceleration =
			GestureAcceleration.legacy();

	private int accelerate(int start, int end, int value) {
		return gestureAcceleration.apply(start, end, value);
	}

	private interface DrawCanvasCallback {
		void drawTo(Canvas c);
	}

	private void drawCallback(DrawCanvasCallback callback, Rect rc, boolean isPartially) {
		if (!readerSurfaceState.isDrawable())
			return;
		ReaderPageBitmapLifetime.Read pageRead =
				pageBitmapLifetime.beginRead();
		if (pageRead == null)
			return;
		try {
			drawCallbackWithPageRead(
					callback, rc, isPartially);
		} finally {
			pageBitmapLifetime.finishRead(pageRead);
		}
	}

	private void drawCallbackWithPageRead(
			DrawCanvasCallback callback,
			Rect rc,
			boolean isPartially) {
		//synchronized(surfaceLock) { }
		//log.v("draw() - in thread " + Thread.currentThread().getName());
		final SurfaceHolder holder = surface.getHolder();
		//log.v("before synchronized(surfaceLock)");
		if (holder != null)
		//synchronized(surfaceLock)
		{
			Canvas canvas = null;
			long startTs = android.os.SystemClock.uptimeMillis();
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
				try {
					canvas = holder.lockHardwareCanvas();
				} catch (Exception e) {
					log.e("drawCallback() -> lockHardwareCanvas(): " + e.toString());
				}
			}
			try {
				if (canvas == null)
					canvas = holder.lockCanvas(rc);
				//log.v("before draw(canvas)");
				if (canvas != null) {
					if (DeviceInfo.EINK_SCREEN) {
						// pre draw update
						//BackgroundThread.instance().executeGUI(() -> EinkScreen.PrepareController(surface, isPartially));
						mEinkScreen.prepareController(surface, isPartially);
					}
					callback.drawTo(canvas);
				}
			} finally {
				//log.v("exiting finally");
				if (canvas != null && surface.getHolder() != null) {
					//log.v("before unlockCanvasAndPost");
					holder.unlockCanvasAndPost(canvas);
					if (rc == null
							&& animationState.current() != null) {
						long endTs = android.os.SystemClock.uptimeMillis();
						updateAnimationDurationStats(endTs - startTs);
					}
					if (DeviceInfo.EINK_SCREEN) {
						// post draw update
						mEinkScreen.updateController(surface, isPartially);
					}
					//log.v("after unlockCanvasAndPost");
				}
			}
		}
		//log.v("exiting draw()");
	}

	abstract class ViewAnimationBase implements ViewAnimationControl {
		//long startTimeStamp;
		boolean started;
		private final BookInfo expectedBook;
		private final DocumentLoadLifecycle.Interaction interaction;
		private final ReaderRenderRequest renderRequest;
		private final ReaderPageAnimationState.Snapshot
				animationSettings;

		public boolean isStarted() {
			return started;
		}

		ViewAnimationBase(
				BookInfo expectedBook,
				DocumentLoadLifecycle.Interaction interaction,
				ReaderPageAnimationState.Snapshot
						animationSettings) {
			//startTimeStamp = android.os.SystemClock.uptimeMillis();
			this.expectedBook = expectedBook;
			this.interaction = interaction;
			this.animationSettings = animationSettings;
			this.renderRequest =
					ReaderRenderRequest.fromInteraction(
							expectedBook, interaction);
			cancelGc();
		}

		final boolean ownsDocument() {
			return isDocumentInteractionCurrent(
					expectedBook, interaction);
		}

		final boolean isCurrentAnimation() {
			return animationState.isCurrent(this)
					&& ownsDocument();
		}

		final ReaderRenderRequest renderRequest() {
			return renderRequest;
		}

		final ReaderPageAnimationState.Snapshot
				animationSettings() {
			return animationSettings;
		}

		public void close() {
			boolean wasCurrent =
					animationState.finish(this);
			if (wasCurrent) {
				animationScheduler.cancel();
			}
			if (ownsDocument()) {
				scheduleSaveCurrentPositionBookmark(
						DEF_SAVE_POSITION_INTERVAL,
						expectedBook, interaction);
				positionPersistenceState.invalidate(
						expectedBook);
				updateCurrentPositionStatus(
						expectedBook, interaction);
			}

			if (wasCurrent || animationState.current() == null)
				scheduleGc();
		}

		public void draw() {
			draw(false);
		}

		public void draw(boolean isPartially) {
			if (!isCurrentAnimation())
				return;
			//	long startTs = android.os.SystemClock.uptimeMillis();
			drawCallback(this::draw, null, isPartially);
		}
	}

	//private static final int PAGE_ANIMATION_DURATION = 3000;
	class ScrollViewAnimation extends ViewAnimationBase {
		int offset;
		int dir;
		int posStart;
		int posEnd;
		double progress;
		int pageHeight;
		int pageWidth;
		BitmapInfo imageStart;
		BitmapInfo imageEnd;

		ScrollViewAnimation(
				int offset, BookInfo expectedBook,
				DocumentLoadLifecycle.Interaction interaction,
				ReaderPageAnimationState.Snapshot
						animationSettings) {
			super(
					expectedBook, interaction,
					animationSettings);
			if (!ownsDocument())
				return;
			log.v("ScrollViewAnimation -- creating: drawing two pages to buffer");
			this.offset = offset;
			this.dir = offset < 0 ? -1 : 1;

			PositionProperties currPos = doc.getPositionProps(null, false);
			if (currPos == null)
				return;
			this.posStart = currPos.y;
			this.posEnd = posStart + offset;
			this.pageHeight = currPos.pageHeight;
			this.pageWidth = currPos.pageWidth;
			this.progress = 0.0;
			BitmapInfo startImage =
					preparePageImage(0, renderRequest());
			BitmapInfo endImage =
					preparePageImage(
							offset, renderRequest());
			if (startImage == null || endImage == null
					|| startImage.bitmap == null
					|| endImage.bitmap == null) {
				log.v("ScrollViewAnimation -- not started: image is null");
				return;
			}
			this.imageStart = startImage;
			this.imageEnd = endImage;
			if (!ownsDocument())
				return;
			animationState.installIfIdle(this);
		}

		@Override
		public void stop(int x, int y) {
			if (!isCurrentAnimation()) {
				close();
				return;
			}
			this.progress = 1.0;
			draw();
			if (!isCurrentAnimation()) {
				close();
				return;
			}
			doc.doCommand(ReaderCommand.DCMD_GO_POS.nativeId, this.posEnd);
			close();
		}

		@Override
		public void move(int duration, boolean accelerated) {
			if (!isCurrentAnimation()) {
				close();
				return;
			}
			if (duration > 0
					&& animationSettings().isEnabled()) {
				int steps = (int) (duration / getAvgAnimationDrawDuration()) + 2;
				for (int i = 1; i < steps; i++) {
					this.progress = AnimationTiming.scrollStep(
							i, steps, accelerated);
					draw();
				}
			}else{
				draw();
			}
		}

		@Override
		public void update(int x, int y) {
		}

		public void animate() {
			if (!isCurrentAnimation()) {
				close();
				return;
			}
			if (!started) {
				started = true;
			}
			if (!animationSettings().isEnabled()) {
				progress = 1.0;
			}else {
				//long frameDur = getAvgAnimationDrawDuration();
				/* just move 12px per frame */
				int absOffset = offset < 0 ? 0-offset : offset;
				long remainingPx = Math.round(absOffset * (1 - progress)); //delta
				long targetStepCount = Math.round(remainingPx / 12);
				targetStepCount = targetStepCount < 1 ? 1 : targetStepCount;
				progress += (1-progress)/targetStepCount;
			}
			draw();
			if (progress < 1.0) {
				scheduleAnimation(this);
			}
		}

		public void draw(Canvas canvas) {
			if (!isCurrentAnimation()
					|| imageStart == null
					|| imageEnd == null
					|| imageStart.isReleased()
					|| imageEnd.isReleased()) {
				return;
			}
			Bitmap imgStart = imageStart.bitmap;
			Bitmap imgEnd = imageEnd.bitmap;
			int h = this.pageHeight;
			int w = this.pageWidth;
			int yBoundary = (int) (this.pageHeight * progress);
			Rect rectImgStartBitmapSrc, rectImgStartCanvasDest, rectImgEndBitmapSrc, rectImgEndCanvasDest;
			if(dir > 0){
				rectImgStartBitmapSrc  = new Rect(0, yBoundary,     w, h);
				rectImgStartCanvasDest = new Rect(0, 0,             w, h - yBoundary);
				rectImgEndBitmapSrc    = new Rect(0, 0,             w, yBoundary);
				rectImgEndCanvasDest   = new Rect(0, h - yBoundary, w, h);
			} else {
				rectImgStartBitmapSrc  = new Rect(0, 0,             w, h - yBoundary);
				rectImgStartCanvasDest = new Rect(0, yBoundary,     w, h);
				rectImgEndBitmapSrc    = new Rect(0, h - yBoundary, w, h);
				rectImgEndCanvasDest   = new Rect(0, 0,             w, yBoundary);
			}
			drawDimmedBitmap(canvas, imgStart, rectImgStartBitmapSrc, rectImgStartCanvasDest);
			drawDimmedBitmap(canvas, imgEnd, rectImgEndBitmapSrc, rectImgEndCanvasDest);
		}
	}

	private final static int SIN_TABLE_SIZE = 1024;
	private final static int SIN_TABLE_SCALE = 0x10000;
	private final static int PI_DIV_2 = (int) (Math.PI / 2 * SIN_TABLE_SCALE);
	private static final PageCurveTables PAGE_CURVE_TABLES =
			new PageCurveTables(SIN_TABLE_SIZE, SIN_TABLE_SCALE);

	class PageViewAnimation extends ViewAnimationBase {
		int startX;
		int maxX;
		int page1;
		int page2;
		int direction;
		int currShift;
		int destShift;
		int pageCount;
		Paint divPaint;
		Paint[] shadePaints;
		Paint[] hilitePaints;
		private final boolean naturalPageFlip;
		private final boolean flipTwoPages;

		BitmapInfo image1;
		BitmapInfo image2;

		PageViewAnimation(
				int startX, int maxX, int direction,
				BookInfo expectedBook,
				DocumentLoadLifecycle.Interaction interaction,
				ReaderPageAnimationState.Snapshot
						animationSettings) {
			super(
					expectedBook, interaction,
					animationSettings);
			this.startX = startX;
			this.maxX = maxX;
			this.direction = direction;
			this.currShift = 0;
			this.destShift = 0;
			this.naturalPageFlip =
					animationSettings.mode()
							== PAGE_ANIMATION_PAPER;
			this.flipTwoPages =
					animationSettings.mode()
							== PAGE_ANIMATION_SLIDE2;
			if (!ownsDocument())
				return;

			long start = android.os.SystemClock.uptimeMillis();
			log.v("PageViewAnimation -- creating: drawing two pages to buffer");

			PositionProperties currPos = mCurrentPageInfo == null ? null : mCurrentPageInfo.position;
			if (currPos == null)
				currPos = doc.getPositionProps(null, false);
			if (currPos == null)
				return;
			page1 = currPos.pageNumber;
			page2 = currPos.pageNumber + direction;
			if (page2 < 0 || page2 >= currPos.pageCount) {
				return;
			}
			this.pageCount = currPos.pageMode;
			image1 = preparePageImage(
					0, renderRequest());
			image2 = preparePageImage(
					direction, renderRequest());
			if (image1 == null || image2 == null) {
				log.v("PageViewAnimation -- cannot start animation: page image is null");
				return;
			}
			if (page1 == page2) {
				log.v("PageViewAnimation -- cannot start animation: not moved");
				return;
			}
			page2 = image2.position.pageNumber;
			if (!ownsDocument())
				return;
			divPaint = new Paint();
			divPaint.setStyle(Paint.Style.FILL);
			divPaint.setColor(mActivity.isNightMode() ? Color.argb(96, 64, 64, 64) : Color.argb(128, 128, 128, 128));
			final int numPaints = 16;
			shadePaints = new Paint[numPaints];
			hilitePaints = new Paint[numPaints];
			for (int i = 0; i < numPaints; i++) {
				shadePaints[i] = new Paint();
				hilitePaints[i] = new Paint();
				hilitePaints[i].setStyle(Paint.Style.FILL);
				shadePaints[i].setStyle(Paint.Style.FILL);
				if (mActivity.isNightMode()) {
					shadePaints[i].setColor(Color.argb((i + 1) * 96 / numPaints, 0, 0, 0));
					hilitePaints[i].setColor(Color.argb((i + 1) * 96 / numPaints, 64, 64, 64));
				} else {
					shadePaints[i].setColor(Color.argb((i + 1) * 96 / numPaints, 0, 0, 0));
					hilitePaints[i].setColor(Color.argb((i + 1) * 96 / numPaints, 255, 255, 255));
				}
			}


			long duration = android.os.SystemClock.uptimeMillis() - start;
			log.d("PageViewAnimation -- created in " + duration + " millis");
			animationState.installIfIdle(this);
		}

		private void drawGradient(Canvas canvas, Rect rc, Paint[] paints, int startIndex, int endIndex) {
			int n = (startIndex < endIndex) ? endIndex - startIndex + 1 : startIndex - endIndex + 1;
			int dir = (startIndex < endIndex) ? 1 : -1;
			int dx = rc.right - rc.left;
			Rect rect = new Rect(rc);
			for (int i = 0; i < n; i++) {
				int index = startIndex + i * dir;
				int x1 = rc.left + dx * i / n;
				int x2 = rc.left + dx * (i + 1) / n;
				if (x2 > rc.right)
					x2 = rc.right;
				rect.left = x1;
				rect.right = x2;
				if (x2 > x1) {
					canvas.drawRect(rect, paints[index]);
				}
			}
		}

		private void drawShadow(Canvas canvas, Rect rc) {
			drawGradient(canvas, rc, shadePaints, shadePaints.length / 2, shadePaints.length / 10);
		}

		private final static int DISTORT_PART_PERCENT = 30;

		private void drawDistorted(Canvas canvas, Bitmap bmp, Rect src, Rect dst, int dir) {
			int srcdx = src.width();
			int dstdx = dst.width();
			int dx = srcdx - dstdx;
			int maxdistortdx = srcdx * DISTORT_PART_PERCENT / 100;
			int maxdx = maxdistortdx * (PI_DIV_2 - SIN_TABLE_SCALE) / SIN_TABLE_SCALE;
			int maxdistortsrc = maxdistortdx * PI_DIV_2 / SIN_TABLE_SCALE;

			int distortdx = dx < maxdistortdx ? dx : maxdistortdx;
			int distortsrcstart = -1;
			int distortsrcend = -1;
			int distortdststart = -1;
			int distortdstend = -1;
			int distortanglestart = -1;
			int distortangleend = -1;
			int normalsrcstart = -1;
			int normalsrcend = -1;
			int normaldststart = -1;
			int normaldstend = -1;

			if (dx < maxdx) {
				// start
				int index = PageFlipGeometry.tableIndex(
						dx, maxdx, SIN_TABLE_SIZE);
				int dstv = PAGE_CURVE_TABLES.destinationShift(index)
						* maxdistortdx / SIN_TABLE_SCALE;
				distortdststart = distortsrcstart = dstdx - dstv;
				distortsrcend = srcdx;
				distortdstend = dstdx;
				normalsrcstart = normaldststart = 0;
				normalsrcend = distortsrcstart;
				normaldstend = distortdststart;
				distortanglestart = 0;
				distortangleend =
						PAGE_CURVE_TABLES.sourceAngle(index);
				distortdx = maxdistortdx;
			} else if (dstdx > maxdistortdx) {
				// middle
				distortdststart = distortsrcstart = dstdx - maxdistortdx;
				distortsrcend = distortsrcstart + maxdistortsrc;
				distortdstend = dstdx;
				normalsrcstart = normaldststart = 0;
				normalsrcend = distortsrcstart;
				normaldstend = distortdststart;
				distortanglestart = 0;
				distortangleend = PI_DIV_2;
			} else {
				// end
				normalsrcstart = normaldststart = normalsrcend = normaldstend = -1;
				distortdx = dstdx;
				distortsrcstart = 0;
				int n = maxdistortdx >= dstdx ? maxdistortdx - dstdx : 0;
				distortsrcend = PAGE_CURVE_TABLES.arcsine(
						PageFlipGeometry.tableIndex(
								n, maxdistortdx, SIN_TABLE_SIZE))
						* maxdistortsrc / SIN_TABLE_SCALE;
				distortdststart = 0;
				distortdstend = dstdx;
				distortangleend = PI_DIV_2;
				n = maxdistortdx >= distortdx ? maxdistortdx - distortdx : 0;
				distortanglestart = PAGE_CURVE_TABLES.arcsine(
						PageFlipGeometry.tableIndex(
								n, maxdistortdx, SIN_TABLE_SIZE));
			}

			Rect srcrc = new Rect(src);
			Rect dstrc = new Rect(dst);
			if (normalsrcstart < normalsrcend) {
				if (dir > 0) {
					srcrc.left = src.left + normalsrcstart;
					srcrc.right = src.left + normalsrcend;
					dstrc.left = dst.left + normaldststart;
					dstrc.right = dst.left + normaldstend;
				} else {
					srcrc.right = src.right - normalsrcstart;
					srcrc.left = src.right - normalsrcend;
					dstrc.right = dst.right - normaldststart;
					dstrc.left = dst.right - normaldstend;
				}
				drawDimmedBitmap(canvas, bmp, srcrc, dstrc);
			}
			if (distortdststart < distortdstend) {
				int n = distortdx / 5 + 1;
				int dst0 = PAGE_CURVE_TABLES.sine(
						PageFlipGeometry.tableIndex(
								distortanglestart,
								PI_DIV_2,
								SIN_TABLE_SIZE))
						* maxdistortdx / SIN_TABLE_SCALE;
				int src0 = distortanglestart * maxdistortdx / SIN_TABLE_SCALE;
				for (int i = 0; i < n; i++) {
					int angledelta = distortangleend - distortanglestart;
					int startangle = distortanglestart + i * angledelta / n;
					int endangle = distortanglestart + (i + 1) * angledelta / n;
					int src1 = startangle * maxdistortdx / SIN_TABLE_SCALE - src0;
					int src2 = endangle * maxdistortdx / SIN_TABLE_SCALE - src0;
					int dst1 = PAGE_CURVE_TABLES.sine(
							PageFlipGeometry.tableIndex(
									startangle,
									PI_DIV_2,
									SIN_TABLE_SIZE))
							* maxdistortdx / SIN_TABLE_SCALE - dst0;
					int dst2 = PAGE_CURVE_TABLES.sine(
							PageFlipGeometry.tableIndex(
									endangle,
									PI_DIV_2,
									SIN_TABLE_SIZE))
							* maxdistortdx / SIN_TABLE_SCALE - dst0;
					int hiliteIndex = startangle * hilitePaints.length / PI_DIV_2;
					Paint[] paints;
					if (dir > 0) {
						dstrc.left = dst.left + distortdststart + dst1;
						dstrc.right = dst.left + distortdststart + dst2;
						srcrc.left = src.left + distortsrcstart + src1;
						srcrc.right = src.left + distortsrcstart + src2;
						paints = hilitePaints;
					} else {
						dstrc.right = dst.right - distortdststart - dst1;
						dstrc.left = dst.right - distortdststart - dst2;
						srcrc.right = src.right - distortsrcstart - src1;
						srcrc.left = src.right - distortsrcstart - src2;
						paints = shadePaints;
					}
					drawDimmedBitmap(canvas, bmp, srcrc, dstrc);
					canvas.drawRect(dstrc, paints[hiliteIndex]);
				}
			}
		}

		@Override
		public void move(int duration, boolean accelerated) {
			if (!isCurrentAnimation()) {
				close();
				return;
			}
			if (duration > 0
					&& animationSettings().isEnabled()) {
				int steps = (int) (duration / getAvgAnimationDrawDuration()) + 2;
				int x0 = currShift;
				int x1 = destShift;
				if ((x0 - x1) < 10 && (x0 - x1) > -10)
					steps = 2;
				for (int i = 1; i < steps; i++) {
					int x = x0 + (x1 - x0) * i / steps;
					currShift = accelerated ? accelerate(x0, x1, x) : x;
					draw();
				}
			}
			currShift = destShift;
			draw();
		}

		@Override
		public void stop(int x, int y) {
			if (!isCurrentAnimation()) {
				close();
				return;
			}
			alog.v("PageViewAnimation.stop(" + x + ", " + y + ")");
			//if ( started ) {
			boolean moved = false;
			if (x != -1) {
				int threshold = mActivity.getPalmTipPixels() * 7 / 8;
				if (direction > 0) {
					// |  <=====  |
					int dx = startX - x;
					if (dx > threshold)
						moved = true;
				} else {
					// |  =====>  |
					int dx = x - startX;
					if (dx > threshold)
						moved = true;
				}
				int duration;
				if (moved) {
					destShift = maxX;
					duration = 300; // 500 ms forward
				} else {
					destShift = 0;
					duration = 200; // 200 ms cancel
				}
				move(duration, false);
			} else {
				moved = true;
			}
			if (!isCurrentAnimation()) {
				close();
				return;
			}
			doc.doCommand(ReaderCommand.DCMD_GO_PAGE_DONT_SAVE_HISTORY.nativeId, moved ? page2 : page1);
			//}
			close();
			// preparing images for next page flip
			if (ownsDocument()) {
				preparePageImage(0, renderRequest());
				preparePageImage(
						direction, renderRequest());
			}
			//if ( started )
			//	drawPage();
		}

		@Override
		public void update(int x, int y) {
			if (!isCurrentAnimation()) {
				close();
				return;
			}
			alog.v("PageViewAnimation.update(" + x + ", " + y + ")");
			int delta = direction > 0 ? startX - x : x - startX;
			if (delta <= 0)
				destShift = 0;
			else if (delta < maxX)
				destShift = delta;
			else
				destShift = maxX;
		}

		public void animate() {
			if (!isCurrentAnimation()) {
				close();
				return;
			}
			int animationDurationMs =
					animationSettings().durationMs();
			alog.v("PageViewAnimation.animate("
					+ currShift + " => " + destShift
					+ ") speed=" + animationDurationMs);
			//log.d("animate() is called");
			if (currShift != destShift) {
				started = true;
				if (animationDurationMs == 0)
					currShift = destShift;
				else {
					int delta = currShift - destShift;
					if (delta < 0)
						delta = -delta;
					long avgDraw = getAvgAnimationDrawDuration();
					int maxStep =
							animationDurationMs > 0
									? (int) (maxX * 1000
											/ avgDraw
											/ animationDurationMs)
									: maxX;
					int step;
					if (delta > maxStep * 2)
						step = maxStep;
					else
						step = (delta + 3) / 4;
					//int step = delta<3 ? 1 : (delta<5 ? 2 : (delta<10 ? 3 : (delta<15 ? 6 : (delta<25 ? 10 : (delta<50 ? 15 : 30)))));
					if (currShift < destShift)
						currShift += step;
					else if (currShift > destShift)
						currShift -= step;
					alog.v("PageViewAnimation.animate(" + currShift + " => " + destShift + "  step=" + step + ")");
				}
				//pointerCurrPos = pointerDestPos;
				draw();
				if (currShift != destShift)
					scheduleAnimation(this);
			}
		}

		public void draw(Canvas canvas) {
			if (!isCurrentAnimation())
				return;
			alog.v("PageViewAnimation.draw(" + currShift + ")");
//			BitmapInfo image1 = mCurrentPageInfo;
//			BitmapInfo image2 = mNextPageInfo;
			if (image1.isReleased() || image2.isReleased())
				return;
			int w = image1.bitmap.getWidth();
			int h = image1.bitmap.getHeight();
			int div;
			if (direction > 0) {
				// FORWARD
				div = w - currShift;
				Rect shadowRect = new Rect(div, 0, div + w / 10, h);
				if (naturalPageFlip) {
					if (this.pageCount == 2) {
						int w2 = w / 2;
						if (div < w2) {
							// left - part of old page
							Rect src1 = new Rect(0, 0, div, h);
							Rect dst1 = new Rect(0, 0, div, h);
							drawDimmedBitmap(canvas, image1.bitmap, src1, dst1);
							// left, resized part of new page
							Rect src2 = new Rect(0, 0, w2, h);
							Rect dst2 = new Rect(div, 0, w2, h);
							//canvas.drawBitmap(image2.bitmap, src2, dst2, null);
							drawDistorted(canvas, image2.bitmap, src2, dst2, -1);
							// right, new page
							Rect src3 = new Rect(w2, 0, w, h);
							Rect dst3 = new Rect(w2, 0, w, h);
							drawDimmedBitmap(canvas, image2.bitmap, src3, dst3);

						} else {
							// left - old page
							Rect src1 = new Rect(0, 0, w2, h);
							Rect dst1 = new Rect(0, 0, w2, h);
							drawDimmedBitmap(canvas, image1.bitmap, src1, dst1);
							// right, resized old page
							Rect src2 = new Rect(w2, 0, w, h);
							Rect dst2 = new Rect(w2, 0, div, h);
							//canvas.drawBitmap(image1.bitmap, src2, dst2, null);
							drawDistorted(canvas, image1.bitmap, src2, dst2, 1);
							// right, new page
							Rect src3 = new Rect(div, 0, w, h);
							Rect dst3 = new Rect(div, 0, w, h);
							drawDimmedBitmap(canvas, image2.bitmap, src3, dst3);

							if (div > 0 && div < w)
								drawShadow(canvas, shadowRect);
						}
					} else {
						Rect src1 = new Rect(0, 0, w, h);
						Rect dst1 = new Rect(0, 0, w - currShift, h);
						//log.v("drawing " + image1);
						//canvas.drawBitmap(image1.bitmap, src1, dst1, null);
						drawDistorted(canvas, image1.bitmap, src1, dst1, 1);
						Rect src2 = new Rect(w - currShift, 0, w, h);
						Rect dst2 = new Rect(w - currShift, 0, w, h);
						//log.v("drawing " + image1);
						drawDimmedBitmap(canvas, image2.bitmap, src2, dst2);

						if (div > 0 && div < w)
							drawShadow(canvas, shadowRect);
					}
				} else {
					if (flipTwoPages) {
						Rect src1 = new Rect(currShift, 0, w, h);
						Rect dst1 = new Rect(0, 0, w - currShift, h);
						//log.v("drawing " + image1);
						drawDimmedBitmap(canvas, image1.bitmap, src1, dst1);
						Rect src2 = new Rect(0, 0, currShift, h);
						Rect dst2 = new Rect(w - currShift, 0, w, h);
						//log.v("drawing " + image1);
						drawDimmedBitmap(canvas, image2.bitmap, src2, dst2);
					} else {
						Rect src1 = new Rect(currShift, 0, w, h);
						Rect dst1 = new Rect(0, 0, w - currShift, h);
						//log.v("drawing " + image1);
						drawDimmedBitmap(canvas, image1.bitmap, src1, dst1);
						Rect src2 = new Rect(w - currShift, 0, w, h);
						Rect dst2 = new Rect(w - currShift, 0, w, h);
						//log.v("drawing " + image1);
						drawDimmedBitmap(canvas, image2.bitmap, src2, dst2);
					}
				}
			} else {
				// BACK
				div = currShift;
				Rect shadowRect = new Rect(div, 0, div + 10, h);
				if (naturalPageFlip) {
					if (this.pageCount == 2) {
						int w2 = w / 2;
						if (div < w2) {
							// left - part of old page
							Rect src1 = new Rect(0, 0, div, h);
							Rect dst1 = new Rect(0, 0, div, h);
							drawDimmedBitmap(canvas, image2.bitmap, src1, dst1);
							// left, resized part of new page
							Rect src2 = new Rect(0, 0, w2, h);
							Rect dst2 = new Rect(div, 0, w2, h);
							//canvas.drawBitmap(image1.bitmap, src2, dst2, null);
							drawDistorted(canvas, image1.bitmap, src2, dst2, -1);
							// right, new page
							Rect src3 = new Rect(w2, 0, w, h);
							Rect dst3 = new Rect(w2, 0, w, h);
							drawDimmedBitmap(canvas, image1.bitmap, src3, dst3);
						} else {
							// left - old page
							Rect src1 = new Rect(0, 0, w2, h);
							Rect dst1 = new Rect(0, 0, w2, h);
							drawDimmedBitmap(canvas, image2.bitmap, src1, dst1);
							// right, resized old page
							Rect src2 = new Rect(w2, 0, w, h);
							Rect dst2 = new Rect(w2, 0, div, h);
							//canvas.drawBitmap(image2.bitmap, src2, dst2, null);
							drawDistorted(canvas, image2.bitmap, src2, dst2, 1);
							// right, new page
							Rect src3 = new Rect(div, 0, w, h);
							Rect dst3 = new Rect(div, 0, w, h);
							drawDimmedBitmap(canvas, image1.bitmap, src3, dst3);

							if (div > 0 && div < w)
								drawShadow(canvas, shadowRect);
						}
					} else {
						Rect src1 = new Rect(currShift, 0, w, h);
						Rect dst1 = new Rect(currShift, 0, w, h);
						drawDimmedBitmap(canvas, image1.bitmap, src1, dst1);
						Rect src2 = new Rect(0, 0, w, h);
						Rect dst2 = new Rect(0, 0, currShift, h);
						//canvas.drawBitmap(image2.bitmap, src2, dst2, null);
						drawDistorted(canvas, image2.bitmap, src2, dst2, 1);

						if (div > 0 && div < w)
							drawShadow(canvas, shadowRect);
					}
				} else {
					if (flipTwoPages) {
						Rect src1 = new Rect(0, 0, w - currShift, h);
						Rect dst1 = new Rect(currShift, 0, w, h);
						drawDimmedBitmap(canvas, image1.bitmap, src1, dst1);
						Rect src2 = new Rect(w - currShift, 0, w, h);
						Rect dst2 = new Rect(0, 0, currShift, h);
						drawDimmedBitmap(canvas, image2.bitmap, src2, dst2);
					} else {
						Rect src1 = new Rect(currShift, 0, w, h);
						Rect dst1 = new Rect(currShift, 0, w, h);
						drawDimmedBitmap(canvas, image1.bitmap, src1, dst1);
						Rect src2 = new Rect(w - currShift, 0, w, h);
						Rect dst2 = new Rect(0, 0, currShift, h);
						drawDimmedBitmap(canvas, image2.bitmap, src2, dst2);
					}
				}
			}
			if (div > 0 && div < w) {
				canvas.drawLine(div, 0, div, h, divPaint);
			}
		}
	}

	private final AnimationTiming animationTiming =
			new AnimationTiming(32, 50);

	private long getAvgAnimationDrawDuration() {
		return animationTiming.averageDrawDuration();
	}

	private void updateAnimationDurationStats(long duration) {
		animationTiming.recordDrawDuration(duration);
	}

	private void drawPage() {
		drawPage(null, false);
	}

	private void drawPage(boolean isPartially) {
		drawPage(null, isPartially);
	}

	private void drawPage(Runnable doneHandler, boolean isPartially) {
		drawPage(
				doneHandler,
				isPartially,
				ReaderRenderRequest.capture(
						mBookInfo,
						documentLoadLifecycle));
	}

	private void drawPage(
			Runnable doneHandler,
			boolean isPartially,
			ReaderRenderRequest renderRequest) {
		if (!readerNativeLifecycle.isInitialized()
				|| !isRenderRequestCurrent(renderRequest))
			return;
		log.v("drawPage() : submitting DrawPageTask");
		if (mOpened)
			scheduleSaveCurrentPositionBookmark(DEF_SAVE_POSITION_INTERVAL);
		post(new DrawPageTask(
				doneHandler, isPartially, renderRequest));
	}

	private byte[] findCoverPage() {
		log.d("document is loaded succesfull, checking coverpage data");
		byte[] coverpageBytes = doc.getCoverPageData();
		if (coverpageBytes != null) {
			log.d("Found cover page data: " + coverpageBytes.length + " bytes");
		}
		return coverpageBytes;
	}

	private final ReaderProgressState progressState =
			new ReaderProgressState();

	private final EinkRefreshLeaseTracker einkRefreshLeases =
			new EinkRefreshLeaseTracker();

	private void requestDisableFullRefresh(int id) {
		if (einkRefreshLeases.acquire(
				id, mEinkScreen.getUpdateInterval())) {
			// current e-ink screen update mode without full refresh
			mEinkScreen.setupController(mEinkScreen.getUpdateMode(), 0, surface);
		}
	}

	private void releaseDisableFullRefresh(int id) {
		Integer intervalToRestore = einkRefreshLeases.release(id);
		if (intervalToRestore != null) {
			// restore e-ink full screen refresh period
			mEinkScreen.setupController(
					mEinkScreen.getUpdateMode(),
					intervalToRestore,
					surface);
		}
	}

	private boolean inDisabledFullRefresh() {
		return einkRefreshLeases.isActive();
	}

	private void showProgress(int position, int titleResource) {
		log.v("showProgress(" + position + ")");
		ReaderProgressState.Change change = progressState.show(
				position,
				titleResource,
				mActivity.getString(titleResource));
		if (change != ReaderProgressState.Change.NONE) {
			if (DeviceInfo.EINK_SCREEN)
				requestDisableFullRefresh(1);
			bookView.draw(
					change == ReaderProgressState.Change.UPDATE);
		}
	}

	private void hideProgress() {
		log.v("hideProgress()");
		if (progressState.hide()) {
			if (DeviceInfo.EINK_SCREEN)
				releaseDisableFullRefresh(1);
			bookView.draw(false);
		}
	}

	public void showCloudSyncProgress(int progress) {
		log.v("showCloudSyncProgress(" + progress + ")");
		ReaderProgressState.Change change =
				progressState.showCloud(progress);
		if (change != ReaderProgressState.Change.NONE) {
			if (DeviceInfo.EINK_SCREEN
					&& change
							== ReaderProgressState.Change.FIRST)
				requestDisableFullRefresh(2);
			bookView.draw(true);
		}
	}

	public void hideCloudSyncProgress() {
		log.v("hideCloudSyncProgress()");
		if (progressState.hideCloud()) {
			if (DeviceInfo.EINK_SCREEN)
				releaseDisableFullRefresh(2);
			bookView.draw(false);
		}
	}

	private class LoadDocumentTask extends Task {
		private final DocumentLoadLifecycle.Request loadOwner;
		private final DocumentLoadLifecycle.Interaction
				loadInteraction;
		private final ReaderRenderRequest renderRequest;
		private final ReaderSettingsApplyRequest
				settingsApplyRequest;
		private BookInfo bookInfo;
		private final DocumentSource documentSource;
		private String filename;
		private String path;
		private final byte[] docBuffer;
		private ParcelFileDescriptor parcelFileDescriptor;
		private final String streamName;
		private final Runnable doneHandler;
		private final Runnable errorHandler;
		private String pos;
		private final int profileNumber;
		private final boolean disableInternalStyles;
		private final boolean disableTextAutoformat;
		private final boolean enableInternalFonts;
		private final Properties settings;
		private final ReaderViewModeState.Snapshot
				viewModeSnapshot;
		private byte[] coverPageBytes;
		private boolean loadSucceeded;

		LoadDocumentTask(
				DocumentLoadLifecycle.Request loadOwner,
				BookInfo bookInfo, DocumentSource documentSource,
				byte[] docBuffer, Runnable doneHandler,
				Runnable errorHandler) {
			this(loadOwner, bookInfo, documentSource, docBuffer,
					null, null, doneHandler, errorHandler);
		}

		LoadDocumentTask(
				DocumentLoadLifecycle.Request loadOwner,
				BookInfo bookInfo, DocumentSource documentSource,
				byte[] docBuffer,
				ParcelFileDescriptor parcelFileDescriptor,
				String streamName, Runnable doneHandler,
				Runnable errorHandler) {
			BackgroundThread.ensureGUI();
			this.loadOwner = loadOwner;
			this.loadInteraction =
					documentLoadLifecycle.interaction();
			this.renderRequest =
					ReaderRenderRequest.fromInteraction(
							bookInfo, loadInteraction);
			this.bookInfo = bookInfo;
			this.documentSource = documentSource;
			this.parcelFileDescriptor = parcelFileDescriptor;
			this.streamName = streamName;
			FileInfo fileInfo = bookInfo.getFileInfo();
			log.v("LoadDocumentTask for " + safeDocumentPathForLog(fileInfo.getPathName()));
			if (fileInfo.getTitle() == null && docBuffer == null && parcelFileDescriptor == null) {
				// As a book 'should' have a title, no title means we should
				// retrieve the book metadata from the engine to get the
				// book language.
				// Is it OK to do this here???  Should we use isScanned?
				// Should we use another fileInfo flag or a new flag?
				mEngine.scanBookProperties(fileInfo);
			}
			this.settingsApplyRequest =
					ReaderSettingsApplyRequest.fromInteraction(
							bookInfo, loadInteraction);
			String language = fileInfo.getLanguage();
			log.v("update hyphenation language: " + language + " for " + fileInfo.getTitle());
			this.filename = documentSource != null
					? documentSource.getIdentity()
					: fileInfo.getPathName();
			this.path = fileInfo.arcname != null ? fileInfo.arcname : fileInfo.pathname;
			this.docBuffer = docBuffer;
			this.doneHandler = doneHandler;
			this.errorHandler = errorHandler;
			//FileInfo fileInfo = new FileInfo(filename);
			disableInternalStyles =
					fileInfo.getFlag(
							FileInfo.DONT_USE_DOCUMENT_STYLES_FLAG);
			disableTextAutoformat =
					fileInfo.getFlag(
							FileInfo.DONT_REFLOW_TXT_FILES_FLAG);
			enableInternalFonts =
					fileInfo.getFlag(
							FileInfo.USE_DOCUMENT_FONTS_FLAG);
			profileNumber = bookInfo.getFileInfo().getProfileId();
			mBookInfo = bookInfo;
			positionPersistenceState.replace(bookInfo);
			// TODO: enable storing of profile per book
			mActivity.setCurrentProfile(profileNumber);
			Bookmark lastPos = null;
			if (bookInfo != null)
				lastPos = bookInfo.getLastPosition();
			if (lastPos != null)
				pos = lastPos.getStartPos();
			log.v("LoadDocumentTask : book " + safeDocumentPathForLog(filename));
			log.v("LoadDocumentTask : last position = " + pos);
			if (lastPos != null)
				setTimeElapsed(lastPos.getTimeElapsed());
			//mBitmap = null;
			//showProgress(1000, R.string.progress_loading);
			//draw();
			BackgroundThread.instance().postGUI(() -> {
				if (documentLoadLifecycle.isActive(loadOwner))
					bookView.draw(false);
			});
			//init();
			settings = readerSettingsState.copy();
			viewModeSnapshot = readerViewModeState.snapshot();
		}

		@Override
		public void work() throws IOException {
			BackgroundThread.ensureBackground();
			if (!documentLoadLifecycle.isActive(loadOwner)) {
				closeParcelFileDescriptor();
				return;
			}
			coverPageBytes = null;
			log.v("LoadDocumentTask : switching current profile");
			applySettings(
					settings,
					viewModeSnapshot,
					settingsApplyRequest); // enforce settings reload
			log.i("Switching done");
			if (!documentLoadLifecycle.isActive(loadOwner)) {
				closeParcelFileDescriptor();
				return;
			}
			log.i("Loading document " + safeDocumentPathForLog(filename));
			doc.doCommand(ReaderCommand.DCMD_SET_INTERNAL_STYLES.nativeId, disableInternalStyles ? 0 : 1);
			doc.doCommand(ReaderCommand.DCMD_SET_TEXT_FORMAT.nativeId, disableTextAutoformat ? 0 : 1);
			doc.doCommand(
					ReaderCommand.DCMD_SET_DOC_FONTS.nativeId,
					enableInternalFonts ? 1 : 0);
			doc.doCommand(ReaderCommand.DCMD_SET_REQUESTED_DOM_VERSION.nativeId, bookInfo.getFileInfo().domVersion);
			if (0 == bookInfo.getFileInfo().domVersion) {
				doc.doCommand(ReaderCommand.DCMD_SET_RENDER_BLOCK_RENDERING_FLAGS.nativeId, 0);
			} else {
				doc.doCommand(ReaderCommand.DCMD_SET_RENDER_BLOCK_RENDERING_FLAGS.nativeId, bookInfo.getFileInfo().blockRenderingFlags);
			}
			boolean success;
			try {
				if (parcelFileDescriptor != null)
					success = doc.loadDocumentFromFD(
							parcelFileDescriptor,
							streamName != null && streamName.length() > 0 ? streamName : filename);
				else if (docBuffer != null)
					success = doc.loadDocumentFromBuffer(docBuffer, filename);
				else
					success = doc.loadDocument(
							documentSource != null
									? documentSource.getLocalPath()
									: filename);
			} finally {
				closeParcelFileDescriptor();
			}
			if (success) {
				log.v("loadDocumentInternal completed successfully");
				if (!documentLoadLifecycle.isActive(loadOwner))
					return;
				updateStrongBookKey();
				if (!documentLoadLifecycle.isActive(loadOwner))
					return;

				doc.requestRender();

				coverPageBytes = findCoverPage();
				log.v("requesting page image, to render");
				preparePageImage(0, renderRequest);
				log.v("updating loaded book info");
				updateLoadedBookInfo(
						bookInfo, null != docBuffer,
						loadInteraction);
				if (!documentLoadLifecycle.isActive(loadOwner))
					return;
				if (null == docBuffer) {
					// Opened existing file
					log.i("Document " + safeDocumentPathForLog(filename) + " is loaded successfully");
					if (pos != null) {
						log.i("Restoring position : " + pos);
						restorePositionBackground(
								pos, bookInfo, loadOwner,
								loadInteraction);
					}
				} else {
					// Opened from memory buffer
					log.i("Stream " + safeDocumentPathForLog(filename) + " loaded successfully");
					// restore the last read position and other tasks are
					// performed in the done () function, since we must
					// receive data from the database through callbacks
					// and cannot control the completion of the operation.
				}
				loadSucceeded =
						documentLoadLifecycle.isActive(loadOwner);
				CoolReader.dumpHeapAllocation();
			} else {
				log.e("Error occurred while trying to load document " + safeDocumentPathForLog(filename));
				throw new IOException("Cannot read document");
			}
		}

		private void updateStrongBookKey() {
			FileInfo fileInfo = bookInfo != null
					? bookInfo.getFileInfo() : null;
			if (fileInfo == null)
				return;
			try {
				String hash;
				if (docBuffer != null) {
					hash = StrongDocumentHasher.sha256(
							new ByteArrayInputStream(docBuffer),
							ParseBudget.MAX_DOCUMENT_INPUT_BYTES);
				} else if (documentSource == null) {
					return;
				} else {
					File hashInput;
					switch (documentSource.getKind()) {
						case FILE:
						case TEMPORARY_IMPORT:
							hashInput = new File(documentSource.getLocalPath());
							break;
						case ARCHIVE_ENTRY:
							hashInput = new File(
									documentSource.getContainer().getLocalPath());
							break;
						case CONTENT_URI:
						default:
							return;
					}
					hash = StrongDocumentHasher.sha256(hashInput);
				}
				BookKey.fromFileInfo(fileInfo)
						.withContentHash(hash)
						.applyTo(fileInfo);
			} catch (IOException | IllegalArgumentException e) {
				log.w("Cannot calculate stable document identity", e);
			}
		}

		@Override
		public void done() {
			BackgroundThread.ensureGUI();
			closeParcelFileDescriptor();
			log.d("LoadDocumentTask, GUI thread is finished successfully");
			if (!loadSucceeded
					|| !mServiceLifecycle.isActive()
					|| !documentLoadLifecycle.isActive(loadOwner))
				return;
			if (!documentLoadLifecycle.markPublished(loadOwner))
				return;

			mBookInfo = bookInfo;
			mOpened = true;
			highlightBookmarks();
			hideProgress();
			drawPage(null, false, renderRequest);
			mActivity.showReader();
			if (doneHandler != null)
				doneHandler.run();

			if (docBuffer == null) {
				finishFileLoad();
			} else {
				resolveStreamBook();
			}
		}

		private void finishFileLoad() {
			mHistory.updateBookAccess(bookInfo, getTimeElapsed());
			final BookInfo finalBookInfo = new BookInfo(bookInfo);
			mActivity.waitForCRDBService(() -> {
				if (mServiceLifecycle.isActive())
					mActivity.getDB().saveBookInfo(finalBookInfo);
			});
			if (coverPageBytes != null
					&& bookInfo.getFileInfo() != null
					&& DeviceInfo.EINK_NOOK) {
				updateNookTouchCoverpage(
						bookInfo.getFileInfo().getPathName(),
						coverPageBytes);
			}
			if (DeviceInfo.EINK_SONY) {
				SonyBookSelector selector =
						new SonyBookSelector(mActivity);
				long contentId = selector.getContentId(path);
				if (contentId != 0) {
					selector.setReadingTime(contentId);
					selector.requestBookSelection(contentId);
				}
			}
			mActivity.setLastBook(filename);
		}

		private void resolveStreamBook() {
			FileInfo fileInfo = bookInfo.getFileInfo();
			if (fileInfo == null || fileInfo.crc32 == 0) {
				log.e("Invalid CRC32 (0)");
				return;
			}
			ArrayList<String> fingerprints =
					new ArrayList<String>(1);
			String fingerprint = Long.toString(fileInfo.crc32);
			fingerprints.add(fingerprint);
			mActivity.waitForCRDBService(() -> {
				if (!documentLoadLifecycle.isActive(loadOwner))
					return;
				mActivity.getDB().findByFingerprints(
						10, fingerprints,
						fileList -> onStreamFingerprints(
								fingerprint, fileList));
			});
		}

		private void onStreamFingerprints(
				String fingerprint, ArrayList<FileInfo> fileList) {
			if (!documentLoadLifecycle.isActive(loadOwner))
				return;
			FileInfo result = null;
			if (fileList != null) {
				for (FileInfo file : fileList) {
					if (file.exists()) {
						result = file;
						break;
					}
				}
			}
			if (result == null) {
				saveStreamToCache();
				return;
			}
			mActivity.getDB().loadBookInfo(
					result,
					resolvedBook -> onStreamBookLoaded(
							fingerprint, resolvedBook));
		}

		private void saveStreamToCache() {
			if (!documentLoadLifecycle.isActive(loadOwner))
				return;
			ByteArrayInputStream inputStream =
					new ByteArrayInputStream(docBuffer);
			BookInfo cachedBook = mDocumentCache.saveStream(
					bookInfo.getFileInfo(), inputStream);
			if (!documentLoadLifecycle.isActive(loadOwner))
				return;
			if (cachedBook != null) {
				bookInfo = new BookInfo(cachedBook);
				mBookInfo = bookInfo;
				mHistory.updateBookAccess(bookInfo, getTimeElapsed());
				final BookInfo finalBookInfo =
						new BookInfo(bookInfo);
				mActivity.waitForCRDBService(() -> {
					if (mServiceLifecycle.isActive())
						mActivity.getDB().saveBookInfo(finalBookInfo);
				});
				mActivity.setLastBook(
						finalBookInfo.getFileInfo().getPathName());
			} else {
				log.e("Failed to save document memory buffer to file!");
				mActivity.showToast(
						R.string.failed_to_save_memory_stream);
			}
		}

		private void onStreamBookLoaded(
				String fingerprint, BookInfo resolvedBook) {
			if (!documentLoadLifecycle.isActive(loadOwner))
				return;
			if (resolvedBook == null) {
				log.e("Failed to load bookmarks for book with fingerprint: "
						+ fingerprint);
				if (errorHandler != null)
					errorHandler.run();
				return;
			}
			bookInfo = new BookInfo(resolvedBook);
			mBookInfo = bookInfo;
			positionPersistenceState.replace(bookInfo);
			FileInfo fileInfo = bookInfo.getFileInfo();
			filename = fileInfo.getPathName();
			path = fileInfo.arcname != null
					? fileInfo.arcname : fileInfo.pathname;
			Bookmark lastPosition = bookInfo.getLastPosition();
			pos = lastPosition != null
					? lastPosition.getStartPos() : null;
			if (pos != null) {
				final String finalPos = pos;
				BackgroundThread.instance().executeBackground(() -> {
					if (!documentLoadLifecycle.isActive(loadOwner))
						return;
					log.i("Restoring position : " + finalPos);
					restorePositionBackground(
							finalPos, bookInfo, loadOwner,
							loadInteraction);
				});
			}
			mHistory.updateBookAccess(bookInfo, getTimeElapsed());
			final BookInfo finalBookInfo = new BookInfo(bookInfo);
			mActivity.waitForCRDBService(() -> {
				if (mServiceLifecycle.isActive())
					mActivity.getDB().saveBookInfo(finalBookInfo);
			});
			mActivity.setLastBook(filename);
		}

		public void fail(Exception e) {
			BackgroundThread.ensureGUI();
			closeParcelFileDescriptor();
			if (!documentLoadLifecycle.isActive(loadOwner))
				return;
			log.v("LoadDocumentTask failed for " + bookInfo, e);
			final FileInfo finalFileInfo =
					new FileInfo(bookInfo.getFileInfo());
			mActivity.waitForCRDBService(() -> {
				if (mServiceLifecycle.isActive()
						&& documentLoadLifecycle.isActive(loadOwner))
					mHistory.removeBookInfo(
							mActivity.getDB(), finalFileInfo, true, false);
			});
			log.d("LoadDocumentTask is finished with exception " + e.getMessage());
			mOpened = false;
			BackgroundThread.instance().executeBackground(() -> {
				if (!documentLoadLifecycle.isActive(loadOwner))
					return;
				doc.doCommand(
						ReaderCommand.DCMD_CLOSE_BOOK.nativeId, 0);
				doc.createDefaultDocument(
						mActivity.getString(R.string.error),
						mActivity.getString(
								R.string.error_while_opening,
								filename));
				doc.requestRender();
				preparePageImage(0, renderRequest);
				drawPage(null, false, renderRequest);
				BackgroundThread.instance().postGUI(
						this::finishFailure);
			});
		}

		private void finishFailure() {
			BackgroundThread.ensureGUI();
			if (!documentLoadLifecycle.complete(loadOwner))
				return;
			if (mBookInfo == bookInfo)
				mBookInfo = null;
			mOpened = false;
			hideProgress();
			mActivity.showToast("Error while loading document");
			if (errorHandler == null)
				return;
			log.e("LoadDocumentTask: Calling error handler");
			errorHandler.run();
		}

		private void closeParcelFileDescriptor() {
			if (parcelFileDescriptor == null)
				return;
			try {
				parcelFileDescriptor.close();
			} catch (IOException e) {
				log.w("Failed to close document file descriptor", e);
			}
			parcelFileDescriptor = null;
		}
	}

	private final static boolean dontStretchWhileDrawing = true;
	private final static boolean centerPageInsteadOfResizing = true;

	private void dimRect(Canvas canvas, Rect dst) {
		if (DeviceInfo.EINK_SCREEN)
			return; // no backlight
		int alpha = dimmingState.alpha();
		if (alpha != 255) {
			Paint p = new Paint();
			p.setColor((255 - alpha) << 24);
			canvas.drawRect(dst, p);
		}
	}

	private void drawDimmedBitmap(Canvas canvas, Bitmap bmp, Rect src, Rect dst) {
		canvas.drawBitmap(bmp, src, dst, null);
		dimRect(canvas, dst);
	}

	protected void drawPageBackground(Canvas canvas, Rect dst, int side) {
		backgroundState.render(background ->
				drawPageBackground(
						canvas, dst, side, background));
	}

	private void drawPageBackground(
			Canvas canvas,
			Rect dst,
			int side,
			ReaderBackgroundState.Snapshot<
					BackgroundTextureInfo, Bitmap> background) {
		Bitmap bmp = background.bitmap();
		if (bmp != null) {
			int h = bmp.getHeight();
			int w = bmp.getWidth();
			Rect src = new Rect(0, 0, w, h);
			if (background.isTiled()) {
				// TILED
				for (int x = 0; x < dst.width(); x += w) {
					int ww = w;
					if (x + ww > dst.width())
						ww = dst.width() - x;
					for (int y = 0; y < dst.height(); y += h) {
						int hh = h;
						if (y + hh > dst.height())
							hh = dst.height() - y;
						Rect d = new Rect(x, y, x + ww, y + hh);
						Rect s = new Rect(0, 0, ww, hh);
						drawDimmedBitmap(canvas, bmp, s, d);
					}
				}
			} else {
				// STRETCHED
				if (side == VIEWER_TOOLBAR_LONG_SIDE)
					side = canvas.getWidth() > canvas.getHeight() ? VIEWER_TOOLBAR_TOP : VIEWER_TOOLBAR_LEFT;
				else if (side == VIEWER_TOOLBAR_SHORT_SIDE)
					side = canvas.getWidth() < canvas.getHeight() ? VIEWER_TOOLBAR_TOP : VIEWER_TOOLBAR_LEFT;
				switch (side) {
					case VIEWER_TOOLBAR_LEFT: {
						int d = dst.width() * dst.height() / h;
						if (d > w)
							d = w;
						src.left = src.right - d;
					}
					break;
					case VIEWER_TOOLBAR_RIGHT: {
						int d = dst.width() * dst.height() / h;
						if (d > w)
							d = w;
						src.right = src.left + d;
					}
					break;
					case VIEWER_TOOLBAR_TOP: {
						int d = dst.height() * dst.width() / w;
						if (d > h)
							d = h;
						src.top = src.bottom - d;
					}
					break;
					case VIEWER_TOOLBAR_BOTTOM: {
						int d = dst.height() * dst.width() / w;
						if (d > h)
							d = h;
						src.bottom = src.top + d;
					}
					break;
				}
				drawDimmedBitmap(canvas, bmp, src, dst);
			}
		} else {
			canvas.drawColor(
					background.color() | 0xFF000000);
		}
	}

	protected void drawPageBackground(Canvas canvas) {
		Rect dst = new Rect(0, 0, canvas.getWidth(), canvas.getHeight());
		drawPageBackground(canvas, dst, VIEWER_TOOLBAR_NONE);
	}

	public class ToolbarBackgroundDrawable extends Drawable {
		private int location = VIEWER_TOOLBAR_NONE;
		private int alpha;

		public void setLocation(int location) {
			this.location = location;
		}

		@Override
		public void draw(Canvas canvas) {
			Rect dst = new Rect(0, 0, canvas.getWidth(), canvas.getHeight());
			try {
				drawPageBackground(canvas, dst, location);
			} catch (Exception e) {
				L.e("Exception in ToolbarBackgroundDrawable.draw", e);
			}
		}

		@Override
		public int getOpacity() {
			return 255 - alpha;
		}

		@Override
		public void setAlpha(int alpha) {
			this.alpha = alpha;

		}

		@Override
		public void setColorFilter(ColorFilter cf) {
			// not supported
		}
	}

	public ToolbarBackgroundDrawable createToolbarBackgroundDrawable() {
		return new ToolbarBackgroundDrawable();
	}

	protected void doDrawProgress(Canvas canvas, int position, String title) {
		log.v("doDrawProgress(" + position + ")");
		if (null == title)
			return;
		int w = canvas.getWidth();
		int h = canvas.getHeight();
		int mins = Math.min(w, h) * 7 / 10;
		int ph = mins / 20;
		int textColor = readerSettingsState.getColor(
				PROP_FONT_COLOR, 0x000000);
		int fontSize = 15;			// 15pt
		float factor = mActivity.getDensityFactor();
		Rect rc = new Rect(w / 2 - mins / 2, h / 2 - ph / 2, w / 2 + mins / 2, h / 2 + ph / 2);
		Utils.drawFrame(canvas, rc, Utils.createSolidPaint(0xC0000000 | textColor));
		//canvas.drawRect(rc, createSolidPaint(0xFFC0C0A0));
		rc.left += 2;
		rc.right -= 2;
		rc.top += 2;
		rc.bottom -= 2;
		int x = rc.left + (rc.right - rc.left) * position / 10000;
		Rect rc1 = new Rect(rc);
		rc1.right = x;
		canvas.drawRect(rc1, Utils.createSolidPaint(0x80000000 | textColor));
		Paint textPaint = Utils.createSolidPaint(0xFF000000 | textColor);
		textPaint.setTextAlign(Paint.Align.CENTER);
		textPaint.setTextSize(fontSize*factor);
		textPaint.setSubpixelText(true);
		canvas.drawText(title, (rc.left + rc.right) / 2, rc1.top - fontSize * factor, textPaint);
		//canvas.drawText(String.valueOf(position * 100 / 10000) + "%", rc.left + 4, rc1.bottom - 4, textPaint);
//		Rect rc2 = new Rect(rc);
//		rc.left = x;
//		canvas.drawRect(rc2, createSolidPaint(0xFFC0C0A0));
	}

	protected void doDrawCloudSyncProgress(Canvas canvas, int position) {
		log.v("doDrawCloudSyncProgress(" + position + ")");
		int w = canvas.getWidth();
		int h = canvas.getHeight();
		int ph = Math.min(w, h)/100;
		if (ph < 5)
			ph = 5;
		ReaderSettingsState.Snapshot settings =
				readerSettingsState.snapshot();
		int textColor = settings.getColor(
				PROP_FONT_COLOR, 0x000000);
		int pageHeaderPos = settings.getInt(
				PROP_STATUS_LOCATION,
				VIEWER_STATUS_PAGE_HEADER);
		Rect rc;
		if (VIEWER_STATUS_PAGE_FOOTER == pageHeaderPos)
			rc = new Rect(0, h - ph, w - 1, h - 2);
		else
			rc = new Rect(0, 1, w - 1, ph);
		int x = rc.left + (rc.right - rc.left) * position / 10000;
		Rect rc1 = new Rect(rc);
		rc1.right = x;
		canvas.drawRect(rc1, Utils.createSolidPaint(0x40000000 | textColor));
	}

	public void setDimmingAlpha(int alpha) {
		if (dimmingState.update(alpha)) {
			mEngine.execute(new Task() {
				@Override
				public void work() throws Exception {
					bookView.draw();
				}

			});
		}
	}

	private void restorePositionBackground(
			String pos, BookInfo expectedBook,
			DocumentLoadLifecycle.Request loadOwner,
			DocumentLoadLifecycle.Interaction interaction) {
		BackgroundThread.ensureBackground();
		if (pos != null) {
			if (!isOwnedDocumentLoadCurrent(
					expectedBook, loadOwner))
				return;
			doc.goToPosition(pos, false);
			if (!isOwnedDocumentLoadCurrent(
					expectedBook, loadOwner))
				return;
			ReaderRenderRequest renderRequest =
					ReaderRenderRequest.fromInteraction(
							expectedBook, interaction);
			preparePageImage(0, renderRequest);
			if (!isOwnedDocumentLoadCurrent(
					expectedBook, loadOwner))
				return;
			drawPage(null, false, renderRequest);
			updateCurrentPositionStatus(
					expectedBook, interaction);
		}
	}

	private boolean isOwnedDocumentLoadCurrent(
			BookInfo expectedBook,
			DocumentLoadLifecycle.Request loadOwner) {
		return mServiceLifecycle.isActive()
				&& expectedBook != null
				&& mBookInfo == expectedBook
				&& documentLoadLifecycle.isActive(loadOwner);
	}

	private final CloseableTaskGate positionSaveLifecycle =
			new CloseableTaskGate();
	private final DelayedExecutor positionSaveScheduler =
			DelayedExecutor.createGUI("position-save");
	private final ReaderPositionPersistenceState<BookInfo>
			positionPersistenceState =
					new ReaderPositionPersistenceState<>();

	private final static int DEF_SAVE_POSITION_INTERVAL = 180000; // 3 minutes

	private void scheduleSaveCurrentPositionBookmark(final int delayMillis) {
		scheduleSaveCurrentPositionBookmark(
				delayMillis, mBookInfo,
				documentLoadLifecycle.interaction());
	}

	private void scheduleSaveCurrentPositionBookmark(
			final int delayMillis,
			final BookInfo expectedBook,
			final DocumentLoadLifecycle.Interaction interaction) {
		BackgroundThread.instance().executeGUI(() -> {
			if (!isDocumentInteractionCurrent(
					expectedBook, interaction))
				return;
			CloseableTaskGate.Token owner =
					replacePositionSave();
			if (owner == null)
				return;
			if (!isBookLoaded() || mBookInfo != expectedBook) {
				positionSaveLifecycle.complete(owner);
				return;
			}
			post(new Task() {
				private ReaderPositionSnapshot snapshot;

				@Override
				public void work() {
					BackgroundThread.ensureBackground();
					if (!positionSaveLifecycle.isActive(owner))
						return;
					snapshot =
							capturePositionSnapshotBackground(
									expectedBook,
									interaction);
					if (!positionSaveLifecycle.isActive(owner))
						snapshot = null;
				}

				@Override
				public void done() {
					BackgroundThread.ensureGUI();
					if (!positionSaveLifecycle.isActive(owner))
						return;
					Bookmark bookmark =
							publishPositionSnapshot(
									snapshot,
									expectedBook,
									interaction);
					if (bookmark == null) {
						positionSaveLifecycle.complete(owner);
						return;
					}
					if (delayMillis <= 1) {
						if (mActivity.getDB() != null
								&& positionSaveLifecycle.complete(owner)
								&& isDocumentInteractionCurrent(
										expectedBook,
										interaction)) {
							log.v(
									"saving last position immediately");
							savePositionBookmark(
									expectedBook,
									bookmark);
							mHistory.updateBookAccess(
									expectedBook,
									getTimeElapsed());
						}
						return;
					}
					synchronized (positionSaveLifecycle) {
						if (positionSaveLifecycle.isActive(owner)) {
							positionSaveScheduler.postDelayed(
									() -> applyPositionSave(
											owner,
											expectedBook,
											bookmark,
											interaction),
									delayMillis);
						}
					}
				}

				@Override
				public void fail(Exception e) {
					BackgroundThread.ensureGUI();
					if (positionSaveLifecycle.complete(owner))
						log.e(
								"Cannot capture current reader position",
								e);
				}
			});
		});

//    	if (DeviceInfo.EINK_SONY && isBookLoaded()) {
//    		getCurrentPositionProperties(new PositionPropertiesCallback() {
//				@Override
//				public void onPositionProperties(PositionProperties props,
//						String positionText) {
//					// update position for Sony T2
//					if (props != null && mBookInfo != null) {
//						String fname = mBookInfo.getFileInfo().getBasePath();
//						if (fname != null && fname.length() > 0)
//							setBookPositionForExternalShell(fname, props.pageNumber, props.pageCount);
//					}
//				}
//    		});
//    	}
	}

	private CloseableTaskGate.Token replacePositionSave() {
		synchronized (positionSaveLifecycle) {
			CloseableTaskGate.Token owner =
					positionSaveLifecycle.replace();
			positionSaveScheduler.cancel();
			return owner;
		}
	}

	private void applyPositionSave(
			CloseableTaskGate.Token owner,
			BookInfo bookInfo,
			Bookmark bookmark,
			DocumentLoadLifecycle.Interaction interaction) {
		if (!positionSaveLifecycle.complete(owner))
			return;
		if (!isDocumentInteractionCurrent(
				bookInfo, interaction)
				|| !isBookLoaded()
				|| mActivity.getDB() == null)
			return;
		log.v("saving last position");
		savePositionBookmark(bookInfo, bookmark);
		mHistory.updateBookAccess(
				bookInfo, getTimeElapsed());
	}

	private void cancelPositionSave() {
		synchronized (positionSaveLifecycle) {
			positionSaveLifecycle.cancel();
			positionSaveScheduler.cancel();
		}
	}

	private void closePositionSave() {
		synchronized (positionSaveLifecycle) {
			positionSaveLifecycle.close();
			positionSaveScheduler.cancel();
		}
	}

	private ReaderPositionSnapshot capturePositionSnapshotBackground(
			BookInfo expectedBook,
			DocumentLoadLifecycle.Interaction interaction) {
		BackgroundThread.ensureBackground();
		if (!readerNativeLifecycle.isInitialized()
				|| !mOpened
				|| !isDocumentInteractionCurrent(
						expectedBook, interaction))
			return null;
		Bookmark bookmark =
				doc.getCurrentPageBookmarkNoRender();
		if (!readerNativeLifecycle.isInitialized()
				|| !mOpened
				|| !isDocumentInteractionCurrent(
						expectedBook, interaction))
			return null;
		return ReaderPositionSnapshot.capture(
				bookmark, System.currentTimeMillis());
	}

	private Bookmark captureCurrentPositionBookmarkSync(
			BookInfo expectedBook,
			DocumentLoadLifecycle.Interaction interaction) {
		BackgroundThread.ensureGUI();
		if (!readerNativeLifecycle.isInitialized()
				|| !mOpened
				|| !isDocumentInteractionCurrent(
						expectedBook, interaction))
			return null;
		ReaderPositionSnapshot snapshot =
				BackgroundThread.instance().callBackground(
						() -> capturePositionSnapshotBackground(
								expectedBook,
								interaction));
		return publishPositionSnapshot(
				snapshot, expectedBook, interaction);
	}

	private Bookmark publishPositionSnapshot(
			ReaderPositionSnapshot snapshot,
			BookInfo expectedBook,
			DocumentLoadLifecycle.Interaction interaction) {
		BackgroundThread.ensureGUI();
		if (snapshot == null
				|| !readerNativeLifecycle.isInitialized()
				|| !mOpened
				|| !isDocumentInteractionCurrent(
						expectedBook, interaction))
			return null;
		Bookmark bookmark = snapshot.copyBookmark();
		expectedBook.setLastPosition(bookmark);
		return bookmark;
	}

	// Sony T2 update position method - by Jotas
	public void setBookPositionForExternalShell(String filename, long current_page, long total_pages) {
		if (DeviceInfo.EINK_SONY) {
			log.d("Trying to update last book and position in Sony T2 shell: file=" + filename + " currentPage=" + current_page + " totalPages=" + total_pages);
			File f = new File(filename);
			if (f.exists()) {
				String file_path = f.getAbsolutePath();
				try {
					file_path = f.getCanonicalPath();
				} catch (Exception e) {
					Log.d("cr3Sony", "setBookPosition getting filename/path", e);
				}

				try {
					Uri uri = Uri.parse("content://com.sony.drbd.ebook.internal.provider/continuerea ding");
					ContentValues contentvalues = new ContentValues();
					contentvalues.put("file_path", file_path);
					contentvalues.put("current_page", current_page);
					contentvalues.put("total_pages", total_pages);
					if (mActivity.getContentResolver().insert(uri, contentvalues) != null)
						Log.d("cr3Sony", "setBookPosition: filename = " + filename + "start=" + current_page + "end=" + total_pages);
					else
						Log.d("crsony", "setBookPosition : error inserting in database!");

				} catch (Exception e) {
					Log.d("cr3Sony", "setBookPositon parse/values!", e);
				}
			}
		}
	}


	public interface PositionPropertiesCallback {
		void onPositionProperties(PositionProperties props, String positionText);
	}

	public void getCurrentPositionProperties(final PositionPropertiesCallback callback) {
		BackgroundThread.ensureGUI();
		final BookInfo expectedBook = mBookInfo;
		final DocumentLoadLifecycle.Interaction interaction =
				documentLoadLifecycle.interaction();
		getCurrentPositionProperties(
				expectedBook, interaction, callback);
	}

	private void getCurrentPositionProperties(
			final BookInfo expectedBook,
			final DocumentLoadLifecycle.Interaction interaction,
			final PositionPropertiesCallback callback) {
		BackgroundThread.ensureGUI();
		if (!isDocumentInteractionCurrent(
				expectedBook, interaction)) {
			callback.onPositionProperties(null, null);
			return;
		}
		BackgroundThread.instance().postBackground(() -> {
			if (!isDocumentInteractionCurrent(
					expectedBook, interaction))
				return;
			final Bookmark bmk = (doc != null) ? doc.getCurrentPageBookmarkNoRender() : null;
			final PositionProperties props = (bmk != null) ? doc.getPositionProps(bmk.getStartPos(), true) : null;
			String computedPositionText = null;
			if (props != null) {
				String percentText =
						DocumentPositionPolicy.formatPercent(
								props.getPercent());
				computedPositionText = ""
						+ DocumentPositionPolicy.displayPageNumber(
								props.pageNumber,
								props.pageCount)
						+ " / "
						+ props.pageCount
						+ " ("
						+ percentText
						+ ")";
			}
			final String positionText = computedPositionText;
			BackgroundThread.instance().postGUI(() -> {
				if (!isDocumentInteractionCurrent(
						expectedBook, interaction))
					return;
				callback.onPositionProperties(
						props, positionText);
			});
		});
	}


	private void savePositionBookmark(
			BookInfo bookInfo,
			Bookmark bookmark) {
		if (bookmark == null
				|| bookInfo == null
				|| mBookInfo != bookInfo
				|| !isBookLoaded()
				|| !mServiceLifecycle.isActive())
			return;
		CRDBService.LocalBinder db = mActivity.getDB();
		if (db == null)
			return;
		ReaderPositionPersistenceState.Request<BookInfo> request =
				positionPersistenceState.begin(
						bookInfo, bookmark.getStartPos());
		if (request == null)
			return;
		boolean saved = false;
		try {
			if (!mServiceLifecycle.isActive()
					|| mBookInfo != bookInfo
					|| !isBookLoaded())
				return;
			mHistory.updateRecentDir();
			db.saveBookInfo(bookInfo);
			db.flush();
			saved = true;
		} finally {
			if (saved)
				positionPersistenceState.complete(request);
			else
				positionPersistenceState.cancel(request);
		}
	}

	public void save() {
		BackgroundThread.ensureGUI();
		final BookInfo expectedBook = mBookInfo;
		final DocumentLoadLifecycle.Interaction interaction =
				documentLoadLifecycle.interaction();
		cancelPositionSave();
		if (isBookLoaded() && expectedBook != null) {
			captureCurrentPositionBookmarkSync(
					expectedBook, interaction);
			if (isDocumentInteractionCurrent(
						expectedBook, interaction)
					&& isBookLoaded()) {
				log.v("saving last immediately");
				log.d("bookmark count 1 = "
						+ expectedBook.getBookmarkCount());
				mHistory.updateBookAccess(
						expectedBook, getTimeElapsed());
				log.d("bookmark count 2 = "
						+ expectedBook.getBookmarkCount());
				mActivity.getDB().saveBookInfo(expectedBook);
				log.d("bookmark count 3 = "
						+ expectedBook.getBookmarkCount());
				mActivity.getDB().flush();
			}
		}
		//scheduleSaveCurrentPositionBookmark(0);
		//post( new SavePositionTask() );
	}

	public void close() {
		closeCurrentDocument(true);
	}

	public boolean cancelPendingDocumentLoad() {
		BackgroundThread.ensureGUI();
		stopTts();
		boolean cancelled =
				documentLoadLifecycle.cancelPending();
		if (cancelled)
			closeCurrentDocument(false);
		return cancelled;
	}

	private void closeCurrentDocument(boolean cancelDocumentLoad) {
		BackgroundThread.ensureGUI();
		log.i("ReaderView.close() is called");
		stopTts();
		resetTemporaryViewMode();
		timeTickLifecycle.cancel();
		if (cancelDocumentLoad)
			documentLoadLifecycle.cancel();
		bookInfoDialogLifecycle.cancel();
		readerOptionsDialogLifecycle.cancel();
		settingsSyncLifecycle.cancel();
		drawTaskLifecycle.cancel();
		stopTracking();
		cancelDocumentAnimation();
		invalidateTapHighlight();
		cancelSelectionUpdates();
		if (readerNativeLifecycle.isClosed())
			return;
		boolean wasOpened = mOpened;
		cancelSwapTask();
		stopAutoScroll();
		stopImageViewer();
		if (wasOpened)
			save();
		mOpened = false;
		final ReaderPageCacheClose<BitmapInfo> pageCacheClose =
				beginPageCacheClose();
		//scheduleSaveCurrentPositionBookmark(0);
		//save();
		post(new Task() {
			public void work() {
				BackgroundThread.ensureBackground();
				try {
					log.i("ReaderView().close() : closing current document");
					doc.doCommand(
							ReaderCommand.DCMD_CLOSE_BOOK.nativeId,
							0);
				} finally {
					publishSerializedPageCacheClose(
							pageCacheClose);
				}
			}

			public void done() {
				BackgroundThread.ensureGUI();
				finishPageCacheClose(pageCacheClose);
			}

			@Override
			public void fail(Exception e) {
				BackgroundThread.ensureGUI();
				finishPageCacheClose(pageCacheClose);
				super.fail(e);
			}
		});
	}

	private ReaderPageCacheClose<BitmapInfo>
			beginPageCacheClose() {
		synchronized (pageBitmapLifetime) {
			return ReaderPageCacheClose.begin(
					mCurrentPageInfo,
					mNextPageInfo);
		}
	}

	private void publishSerializedPageCacheClose(
			ReaderPageCacheClose<BitmapInfo> close) {
		synchronized (pageBitmapLifetime) {
			BitmapInfo current = mCurrentPageInfo;
			BitmapInfo next = mNextPageInfo;
			if (!close.publishSerialized(current, next))
				return;
			mCurrentPageInfo = null;
			mNextPageInfo = null;
			pageInvalidationState.invalidate();
		}
	}

	private void finishPageCacheClose(
			ReaderPageCacheClose<BitmapInfo> close) {
		ReaderPageCacheClose.Resources<BitmapInfo> resources =
				close.finish();
		if (resources == null)
			return;
		BitmapInfo[] images = {
			resources.initialCurrent(),
			resources.initialNext(),
			resources.serializedCurrent(),
			resources.serializedNext()
		};
		for (int i = 0; i < images.length; i++) {
			BitmapInfo image = images[i];
			if (image == null || image.isReleased())
				continue;
			boolean duplicate = false;
			for (int j = 0; j < i; j++) {
				if (images[j] == image) {
					duplicate = true;
					break;
				}
			}
			if (!duplicate)
				image.recycle();
		}
		factory.compact();
	}

	public void destroy() {
		log.i("ReaderView.destroy() is called");
		closeSurfaceCallbacks();
		recycleBackgroundBitmap(
				backgroundState.close());
		stopTts();
		ttsToolbarState.close();
		stopImageViewer();
		imageViewerState.close();
		resetTemporaryViewMode();
		readerViewModeState.close();
		cancelDelayedReaderWork();
		closeNativeDocument();
	}

	private void closeNativeDocument() {
		if (!readerNativeLifecycle.close())
			return;
		BackgroundThread.instance().postBackground(() -> {
			BackgroundThread.ensureBackground();
			if (!readerNativeLifecycle.claimDestroy())
				return;
			log.i("ReaderView.destroyInternal() calling");
			doc.destroy();
		});
	}

	private void closeSurfaceCallbacks() {
		BackgroundThread.ensureGUI();
		readerSurfaceState.close();
		einkRefreshScheduler.cancel();
		surface.setOnTouchListener(null);
		surface.setOnKeyListener(null);
		surface.setOnFocusChangeListener(null);
		surface.getHolder().removeCallback(this);
		applySurfaceMemoryChange(
				surfaceMemoryState.clear());
	}

	private void cancelDelayedReaderWork() {
		cancelDocumentAnimation();
		animationState.close();
		gcTask.cancel();
		closeSwapTasks();
		closeTapHighlight();
		closePositionSave();
		closeSelectionUpdates();
		drawTaskLifecycle.close();
		ttsInitializationLifecycle.close();
		bookInfoDialogLifecycle.close();
		readerOptionsDialogLifecycle.close();
		settingsSyncLifecycle.close();
		timeTickLifecycle.close();
		positionPersistenceState.close();
		pageInvalidationState.close();
		pageBitmapLifetime.close();
		documentLoadLifecycle.close();
		closeGestureTimeouts();
		synchronized (viewportResizeState) {
			viewportResizeState.close();
			resizeScheduler.cancel();
		}
		synchronized (autoScrollSessions) {
			autoScrollSessions.close();
			autoScrollScheduler.cancel();
		}
	}

	private String getCSSForFormat(DocumentFormat fileFormat) {
		if (fileFormat == null)
			fileFormat = DocumentFormat.FB2;
		File[] dataDirs = Engine.getDataDirectories(null, false, false);
		String defaultCss = mEngine.loadResourceUtf8(fileFormat.getCSSResourceId());
		for (File dir : dataDirs) {
			File file = new File(dir, fileFormat.getCssName());
			if (file.exists()) {
				String css = Engine.loadFileUtf8(file);
				if (css != null) {
					int p1 = css.indexOf("@import");
					if (p1 < 0)
						p1 = css.indexOf("@include");
					int p2 = css.indexOf("\";");
					if (p1 >= 0 && p2 >= 0 && p1 < p2) {
						css = css.substring(0, p1) + "\n" + defaultCss + "\n" + css.substring(p2 + 2);
					}
					return css;
				}
			}
		}
		return defaultCss;
	}

	boolean enable_progress_callback = true;
	ReaderCallback readerCallback = new ReaderCallback() {

		public boolean OnExportProgress(int percent) {
			log.d("readerCallback.OnExportProgress " + percent);
			return true;
		}

		public void OnExternalLink(String url, String nodeXPath) {
		}

		public void OnFormatEnd() {
			log.d("readerCallback.OnFormatEnd");
			//mEngine.hideProgress();
			hideProgress();
			drawPage();
			scheduleSwapTask();
		}

		public boolean OnFormatProgress(final int percent) {
			if (enable_progress_callback) {
				log.d("readerCallback.OnFormatProgress " + percent);
				showProgress(percent * 4 / 10 + 5000, R.string.progress_formatting);
			}
//			executeSync( new Callable<Object>() {
//				public Object call() {
//					BackgroundThread.ensureGUI();
//			    	log.d("readerCallback.OnFormatProgress " + percent);
//			    	showProgress( percent*4/10 + 5000, R.string.progress_formatting);
//			    	return null;
//				}
//			});
			return true;
		}

		public void OnFormatStart() {
			log.d("readerCallback.OnFormatStart");
		}

		public void OnLoadFileEnd() {
			log.d("readerCallback.OnLoadFileEnd");
			ensureAppliedViewportSize();
		}

		public void OnLoadFileError(String message) {
			log.d("readerCallback.OnLoadFileError(" + message + ")");
		}

		public void OnLoadFileFirstPagesReady() {
			log.d("readerCallback.OnLoadFileFirstPagesReady");
		}

		public String OnLoadFileFormatDetected(final DocumentFormat fileFormat) {
			log.i("readerCallback.OnLoadFileFormatDetected " + fileFormat);
			if (fileFormat != null) {
				return getCSSForFormat(fileFormat);
			}
			return null;
//
//			String res = executeSync( new Callable<String>() {
//				public String call() {
//					BackgroundThread.ensureGUI();
//					log.i("readerCallback.OnLoadFileFormatDetected " + fileFormat);
//					if (fileFormat != null) {
//						String s = getCSSForFormat(fileFormat);
//						log.i("setting .css for file format " + fileFormat + " from resource " + fileFormat.getCssName());
//						return s;
//					}
//			    	return null;
//				}
//			});
////			int internalStyles = mBookInfo.getFileInfo().getFlag(FileInfo.DONT_USE_DOCUMENT_STYLES_FLAG) ? 0 : 1;
////			int txtReflow = mBookInfo.getFileInfo().getFlag(FileInfo.DONT_REFLOW_TXT_FILES_FLAG) ? 0 : 2;
////			log.d("internalStyles: " + internalStyles);
////			doc.doCommand(ReaderCommand.DCMD_SET_INTERNAL_STYLES.nativeId, internalStyles | txtReflow);
//			return res;
		}

		public boolean OnLoadFileProgress(final int percent) {
			BackgroundThread.ensureBackground();
			if (enable_progress_callback) {
				log.d("readerCallback.OnLoadFileProgress " + percent);
				showProgress(percent * 4 / 10 + 1000, R.string.progress_loading);
			}
//			executeSync( new Callable<Object>() {
//				public Object call() {
//					BackgroundThread.ensureGUI();
//			    	log.d("readerCallback.OnLoadFileProgress " + percent);
//			    	showProgress( percent*4/10 + 1000, R.string.progress_loading);
//			    	return null;
//				}
//			});
			return true;
		}

		public void OnLoadFileStart(String filename) {
			cancelSwapTask();
			invalidateTapHighlight();
			cancelPositionSave();
			cancelSelectionUpdates();
			BackgroundThread.ensureBackground();
			log.d("readerCallback.OnLoadFileStart " + filename);
			if (enable_progress_callback) {
				showProgress(1000, R.string.progress_loading);
			}
		}

		/// Override to handle external links
		public void OnImageCacheClear() {
			//log.d("readerCallback.OnImageCacheClear");
			clearImageCache();
		}

		public boolean OnRequestReload() {
			//reloadDocument();
			return true;
		}

	};

	private final CloseableTaskGate swapTaskLifecycle =
			new CloseableTaskGate();
	private final DelayedExecutor swapTaskScheduler =
			DelayedExecutor.createGUI("swap-to-cache");

	private void scheduleSwapTask() {
		CloseableTaskGate.Token owner =
				swapTaskLifecycle.replace();
		if (owner != null)
			new SwapToCacheTask(owner).reschedule();
	}

	private void cancelSwapTask() {
		synchronized (swapTaskLifecycle) {
			swapTaskLifecycle.cancel();
			swapTaskScheduler.cancel();
		}
	}

	private void closeSwapTasks() {
		synchronized (swapTaskLifecycle) {
			swapTaskLifecycle.close();
			swapTaskScheduler.cancel();
		}
	}

	private class SwapToCacheTask extends Task {
		private final CloseableTaskGate.Token owner;
		private final long startTime;
		private boolean isTimeout;

		public SwapToCacheTask(
				CloseableTaskGate.Token owner) {
			this.owner = owner;
			startTime = System.currentTimeMillis();
		}

		public void reschedule() {
			synchronized (swapTaskLifecycle) {
				if (!swapTaskLifecycle.isActive(owner))
					return;
				swapTaskScheduler.postDelayed(() -> {
					if (swapTaskLifecycle.isActive(owner))
						post(SwapToCacheTask.this);
				}, 2000);
			}
		}

		@Override
		public void work() throws Exception {
			if (!swapTaskLifecycle.isActive(owner))
				return;
			int res = doc.swapToCache();
			isTimeout = res == DocView.SWAP_TIMEOUT;
			long duration = System.currentTimeMillis() - startTime;
			if (!isTimeout) {
				log.i("swapToCacheInternal is finished with result " + res + " in " + duration + " ms");
			} else {
				log.d("swapToCacheInternal exited by TIMEOUT in " + duration + " ms: rescheduling");
			}
		}

		@Override
		public void done() {
			if (!swapTaskLifecycle.isActive(owner))
				return;
			if (isTimeout) {
				reschedule();
			} else {
				swapTaskLifecycle.complete(owner);
			}
		}

		@Override
		public void fail(Exception e) {
			swapTaskLifecycle.complete(owner);
			super.fail(e);
		}

	}

	public void clearImageCache() {
		pageInvalidationState.invalidate();
	}

	public void setStyleSheet(final String css) {
		BackgroundThread.ensureGUI();
		if (css != null && css.length() > 0) {
			post(new Task() {
				public void work() {
					doc.setStylesheet(css);
				}
			});
		}
	}

	public void goToPosition(int position) {
		BackgroundThread.ensureGUI();
		doEngineCommand(ReaderCommand.DCMD_GO_POS, position);
	}

	public void moveBy(final int delta) {
		BackgroundThread.ensureGUI();
		log.d("moveBy(" + delta + ")");
		final BookInfo expectedBook = mBookInfo;
		final DocumentLoadLifecycle.Interaction interaction =
				documentLoadLifecycle.interaction();
		if (!isDocumentInteractionCurrent(
				expectedBook, interaction))
			return;
		post(new Task() {
			private boolean moved;

			public void work() {
				BackgroundThread.ensureBackground();
				if (!isDocumentInteractionCurrent(
						expectedBook, interaction))
					return;
				moved = doc.doCommand(
						ReaderCommand.DCMD_SCROLL_BY.nativeId,
						delta);
				if (moved)
					updateCurrentPositionStatus(
							expectedBook, interaction);
			}

			public void done() {
				if (!moved
						|| !isDocumentInteractionCurrent(
								expectedBook, interaction))
					return;
				drawPage();
				scheduleSaveCurrentPositionBookmark(
						DEF_SAVE_POSITION_INTERVAL,
						expectedBook, interaction);
			}
		});
	}

	public void goToPage(int pageNumber) {
		BackgroundThread.ensureGUI();
		goToPage(
				pageNumber, mBookInfo,
				documentLoadLifecycle.interaction());
	}

	private boolean goToPage(
			int pageNumber, BookInfo expectedBook,
			DocumentLoadLifecycle.Interaction interaction) {
		BackgroundThread.ensureGUI();
		if (!isDocumentInteractionCurrent(
				expectedBook, interaction))
			return false;
		doEngineCommand(ReaderCommand.DCMD_GO_PAGE, pageNumber - 1);
		return true;
	}

	public void goToPercent(final int percent) {
		BackgroundThread.ensureGUI();
		goToPercent(
				percent, mBookInfo,
				documentLoadLifecycle.interaction());
	}

	private boolean goToPercent(
			final int percent, final BookInfo expectedBook,
			final DocumentLoadLifecycle.Interaction interaction) {
		BackgroundThread.ensureGUI();
		if (percent < 0 || percent > 100
				|| !isDocumentInteractionCurrent(
						expectedBook, interaction))
			return false;
		post(new Task() {
			private boolean moved;

			public void work() {
				if (!isDocumentInteractionCurrent(
						expectedBook, interaction))
					return;
				PositionProperties pos =
						doc.getPositionProps(null, true);
				if (pos != null) {
					int pageNumber =
							DocumentPositionPolicy.pageIndexForPercent(
									pos.pageCount, percent);
					if (pageNumber < 0)
						return;
					moved = doc.doCommand(
							ReaderCommand.DCMD_GO_PAGE.nativeId,
							pageNumber);
					if (moved)
						updateCurrentPositionStatus(
								expectedBook, interaction);
				}
			}

			public void done() {
				if (!moved
						|| !isDocumentInteractionCurrent(
								expectedBook, interaction))
					return;
				drawPage();
				scheduleSaveCurrentPositionBookmark(
						DEF_SAVE_POSITION_INTERVAL,
						expectedBook, interaction);
			}
		});
		return true;
	}

	public interface MoveSelectionCallback {
		// selection is changed
		public void onNewSelection(Selection selection);

		// cannot move selection
		public void onFail();
	}

	public void moveSelection(final ReaderCommand command, final int param, final MoveSelectionCallback callback) {
		moveSelection(
				command, param, callback,
				mBookInfo, documentLoadLifecycle.interaction());
	}

	void moveSelection(
			final ReaderCommand command, final int param,
			final MoveSelectionCallback callback,
			final BookInfo expectedBook,
			final DocumentLoadLifecycle.Interaction interaction) {
		if (!isDocumentInteractionCurrent(
				expectedBook, interaction))
			return;
		post(new Task() {
			private boolean res;
			private Selection selection = new Selection();

			@Override
			public void work() throws Exception {
				if (!isDocumentInteractionCurrent(
						expectedBook, interaction))
					return;
				res = doc.moveSelection(selection, command.nativeId, param);
			}

			@Override
			public void done() {
				if (!isDocumentInteractionCurrent(
						expectedBook, interaction))
					return;
				if (callback != null) {
					clearImageCache();
					surface.invalidate();
					drawPage();
					if (res)
						callback.onNewSelection(selection);
					else
						callback.onFail();
				}
			}

			@Override
			public void fail(Exception e) {
				if (callback != null
						&& isDocumentInteractionCurrent(
								expectedBook, interaction))
					callback.onFail();
			}


		});
	}

	private void showSwitchProfileDialog() {
		BackgroundThread.ensureGUI();
		BookInfo expectedBook = mBookInfo;
		DocumentLoadLifecycle.Interaction interaction =
				documentLoadLifecycle.interaction();
		if (!isDocumentInteractionCurrent(
				expectedBook, interaction))
			return;
		SwitchProfileDialog dlg = new SwitchProfileDialog(
				mActivity,
				profileSwitchHandler(
						expectedBook, interaction));
		dlg.show();
	}

	private ProfileSwitchHandler profileSwitchHandler(
			final BookInfo expectedBook,
			final DocumentLoadLifecycle.Interaction interaction) {
		return new ProfileSwitchHandler() {
			@Override
			public boolean isActive() {
				return isDocumentInteractionCurrent(
						expectedBook, interaction);
			}

			@Override
			public int getCurrentProfile() {
				return mActivity.getCurrentProfile();
			}

			@Override
			public boolean selectProfile(int profile) {
				return applyProfileSelection(
						profile, expectedBook, interaction);
			}
		};
	}

	private boolean applyProfileSelection(
			int profile,
			BookInfo expectedBook,
			DocumentLoadLifecycle.Interaction interaction) {
		BackgroundThread.ensureGUI();
		if (profile < 1 || profile > Settings.MAX_PROFILES
				|| !isDocumentInteractionCurrent(
						expectedBook, interaction)
				|| expectedBook.getFileInfo() == null)
			return false;
		if (mActivity.getCurrentProfile() == profile)
			return true;
		expectedBook.getFileInfo().setProfileId(profile);
		if (mActivity.getDB() != null)
			mActivity.getDB().saveBookInfo(expectedBook);
		log.i("Apply new profile settings");
		mActivity.setCurrentProfile(profile);
		return true;
	}

	private final static String NOOK_TOUCH_COVERPAGE_DIR = "/media/screensavers/currentbook";

	private void updateNookTouchCoverpage(String bookFileName,
										  byte[] coverpageBytes) {
		try {
			String imageFileName;
			int lastSlash = bookFileName.lastIndexOf("/");
			// exclude path and extension
			if (lastSlash >= 0 && lastSlash < bookFileName.length()) {
				imageFileName = bookFileName.substring(lastSlash);
			} else {
				imageFileName = bookFileName;
			}
			int lastDot = imageFileName.lastIndexOf(".");
			if (lastDot > 0) {
				imageFileName = imageFileName.substring(0, lastDot);
			}
			// guess image type
			if (coverpageBytes.length > 8 // PNG signature length
					&& coverpageBytes[0] == (byte) 0x89 // PNG signature start 4 bytes
					&& coverpageBytes[1] == 0x50
					&& coverpageBytes[2] == 0x4E
					&& coverpageBytes[3] == 0x47) {
				imageFileName += ".png";
			} else if (coverpageBytes.length > 3 // Checking only the first 3
					// bytes of JPEG header
					&& coverpageBytes[0] == (byte) 0xFF
					&& coverpageBytes[1] == (byte) 0xD8
					&& coverpageBytes[2] == (byte) 0xFF) {
				imageFileName += ".jpg";
			} else if (coverpageBytes.length > 3 // Checking only the first 3
					// bytes of GIF header
					&& coverpageBytes[0] == 0x47
					&& coverpageBytes[1] == 0x49
					&& coverpageBytes[2] == 0x46) {
				imageFileName += ".gif";
			} else if (coverpageBytes.length > 2 // Checking only the first 2
					// bytes of BMP signature
					&& coverpageBytes[0] == 0x42 && coverpageBytes[1] == 0x4D) {
				imageFileName += ".bmp";
			} else {
				imageFileName += ".jpg"; // default image type
			}
			// create directory if it does not exist
			File d = new File(NOOK_TOUCH_COVERPAGE_DIR);
			if (!d.exists()) {
				d.mkdir();
			}
			// create file only if file with same name does not exist
			File f = new File(d, imageFileName);
			if (!f.exists()) {
				// delete other files in directory so that only current cover is
				// shown all the time
				File[] files = d.listFiles();
				for (File oldFile : files) {
					oldFile.delete();
				}
				// write the image file
				FileOutputStream fos = new FileOutputStream(f);
				fos.write(coverpageBytes);
				fos.close();
			}
		} catch (Exception ex) {
			log.e("Error writing cover page: ", ex);
		}
	}

	private static final int GC_INTERVAL = 15000; // 15 seconds
	private final DelayedExecutor gcTask =
			DelayedExecutor.createGUI("gc");

	public void scheduleGc() {
		try {
			gcTask.postDelayed(() -> {
				log.v("Initiating garbage collection");
				System.gc();
			}, GC_INTERVAL);
		} catch (Exception e) {
			// ignore
		}
	}

	public void cancelGc() {
		try {
			gcTask.cancel();
		} catch (Exception e) {
			// ignore
		}
	}

	private void switchFontFace(int direction) {
		String currentFontFace =
				readerSettingsState.getProperty(
						PROP_FONT_FACE, "");
		String selected = FontFaceSwitcher.select(
				currentFontFace,
				Engine.getFontFaceList(),
				direction);
		if (selected == null)
			return;
		saveSetting(PROP_FONT_FACE, selected);
		syncViewSettings(getSettings(), true, true);
	}

	public void showInputDialog(final String title, final String prompt, final boolean isNumberEdit, final int minValue, final int maxValue, final int lastValue, final InputHandler handler) {
		BackgroundThread.instance().executeGUI(() -> {
			final InputDialog dlg = new InputDialog(mActivity, title, prompt, isNumberEdit, minValue, maxValue, lastValue, handler);
			dlg.show();
		});
	}

	private void showInputDialog(
			final String title, final String prompt,
			final boolean isNumberEdit, final int minValue,
			final int maxValue, final int lastValue,
			final InputHandler handler,
			final BookInfo expectedBook,
			final DocumentLoadLifecycle.Interaction interaction) {
		BackgroundThread.instance().executeGUI(() -> {
			if (!isDocumentInteractionCurrent(
					expectedBook, interaction))
				return;
			final InputDialog dlg = new InputDialog(
					mActivity, title, prompt, isNumberEdit,
					minValue, maxValue, lastValue, handler);
			dlg.show();
		});
	}

	public void showGoToPageDialog() {
		BackgroundThread.ensureGUI();
		final BookInfo expectedBook = mBookInfo;
		final DocumentLoadLifecycle.Interaction interaction =
				documentLoadLifecycle.interaction();
		getCurrentPositionProperties(
				expectedBook, interaction,
				(props, positionText) -> {
			if (props == null)
				return;
			if (props.pageCount <= 0)
				return;
			String pos = mActivity.getString(R.string.dlg_goto_current_position) + " " + positionText;
			String prompt = mActivity.getString(R.string.dlg_goto_input_page_number);
			showInputDialog(mActivity.getString(R.string.mi_goto_page), pos + "\n" + prompt, true,
					1, props.pageCount,
					DocumentPositionPolicy.displayPageNumber(
							props.pageNumber, props.pageCount),
					new InputHandler() {
						int pageNumber = 0;

						@Override
						public boolean validate(String s) {
							pageNumber = Integer.parseInt(s);
							return pageNumber > 0 && pageNumber <= props.pageCount;
						}

						@Override
						public void onOk(String s) {
							goToPage(
									pageNumber,
									expectedBook,
									interaction);
						}

						@Override
						public void onCancel() {
						}
					}, expectedBook, interaction);
				});
	}

	public void showGoToPercentDialog() {
		BackgroundThread.ensureGUI();
		final BookInfo expectedBook = mBookInfo;
		final DocumentLoadLifecycle.Interaction interaction =
				documentLoadLifecycle.interaction();
		getCurrentPositionProperties(
				expectedBook, interaction,
				(props, positionText) -> {
			if (props == null)
				return;
			String pos = mActivity.getString(R.string.dlg_goto_current_position) + " " + positionText;
			String prompt = mActivity.getString(R.string.dlg_goto_input_percent);
			showInputDialog(mActivity.getString(R.string.mi_goto_percent), pos + "\n" + prompt, true,
					0, 100, props.getPercent() / 100,
					new InputHandler() {
						int percent = 0;

						@Override
						public boolean validate(String s) {
							percent = Integer.valueOf(s);
							return percent >= 0 && percent <= 100;
						}

						@Override
						public void onOk(String s) {
							goToPercent(
									percent,
									expectedBook,
									interaction);
						}

						@Override
						public void onCancel() {
						}
					}, expectedBook, interaction);
				});
	}

	@Override
	public boolean onKey(View v, int keyCode, KeyEvent event) {
		if (readerSurfaceState.isClosed())
			return false;
		// TODO Auto-generated method stub
		if (event.getAction() == KeyEvent.ACTION_DOWN)
			return onKeyDown(keyCode, event);
		else if (event.getAction() == KeyEvent.ACTION_UP)
			return onKeyUp(keyCode, event);
		return false;
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		if (readerSurfaceState.isClosed())
			return false;
		return onTouchEvent(event);
	}

	public boolean onKeyDown(int keyCode, final KeyEvent event) {
		if (readerSurfaceState.isClosed())
			return false;

		if (keyCode == 0)
			keyCode = event.getScanCode();
		keyCode = translateKeyCode(keyCode);

		mActivity.onUserActivity();

		ImageViewer imageViewer = imageViewerState.current();
		if (imageViewer != null && imageViewer.isActive())
			return imageViewer.onKeyDown(keyCode, event);

//		backKeyDownHere = false;
		if (event.getRepeatCount() == 0) {
			log.v("onKeyDown(" + keyCode + ", " + event + ")");

			if (keyCode == KeyEvent.KEYCODE_BACK) {
				// force saving position on BACK key press
				scheduleSaveCurrentPositionBookmark(1);
			}
		}
		if (keyCode == KeyEvent.KEYCODE_POWER || keyCode == KeyEvent.KEYCODE_ENDCALL) {
			mActivity.releaseBacklightControl();
			return false;
		}

		ReaderInputSettings inputSettings =
				ReaderInputSettings.capture(
						readerSettingsState.snapshot());
		if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
			if (isAutoScrollActive()) {
				if (keyCode == KeyEvent.KEYCODE_VOLUME_UP)
					changeAutoScrollSpeed(1);
				else
					changeAutoScrollSpeed(-1);
				return true;
			}
			if (!inputSettings.areVolumeKeysEnabled()) {
				return false;
			}
		}

		if (isAutoScrollActive())
			return true; // autoscroll will be stopped in onKeyUp

		keyCode = overrideKey(keyCode);
		ReaderAction action = findKeyAction(
				keyCode,
				ReaderAction.NORMAL,
				inputSettings);
		ReaderAction longAction = findKeyAction(
				keyCode,
				ReaderAction.LONG,
				inputSettings);
		if (event.getRepeatCount() == 0) {
			KeyDoubleClickState.PressResult<ReaderAction>
					pending =
					keyDoubleClickState.resolvePress(
							keyCode,
							android.os.SystemClock.uptimeMillis(),
							DOUBLE_CLICK_INTERVAL);
			if (pending != null) {
				keyDoubleClickScheduler.cancel();
				ReaderAction pendingAction = pending.action();
				if (pendingAction != null) {
					log.d("executing pending key action "
							+ pendingAction);
					onAction(pendingAction);
				}
				if (pending.consumesPress())
					return true;
			}
		}

		if (event.getRepeatCount() > 0) {
			KeyRepeatState.RepeatEvent<ReaderAction>
					repeatEvent =
					keyRepeatState.repeat(
							event.getKeyCode(),
							event.getDownTime(),
							event.getEventTime(),
							KEY_DOWN_TIME_TOLERANCE,
							AUTOREPEAT_KEYPRESS_TIME);
			if (!repeatEvent.isTracked()) {
				stopTracking();
				return true; // ignore
			}
			if (repeatEvent.isLongPress()) {
				if (repeatEvent.hasRepeatAction()) {
					runKeyRepeatAction(
							repeatEvent.repeat());
				} else {
					stopTracking();
					log.v("executing action on long press : " + longAction);
					onAction(longAction);
				}
			}
			return true;
		}

		if (!action.isNone() && action.canRepeat() && longAction.isRepeat()) {
			// start tracking repeat
			KeyRepeatState.Press<ReaderAction> press =
					startTrackingKey(event, action);
			KeyRepeatState.Repeat<ReaderAction> repeat =
					keyRepeatState.startRepeat(press);
			if (repeat == null)
				return false;
			runKeyRepeatAction(repeat);
			return true;
		}

/*		if ( keyCode>=KeyEvent.KEYCODE_0 && keyCode<=KeyEvent.KEYCODE_9 ) {
			// will process in keyup handler
			startTrackingKey(event, null);
			return true;
		}*/
		if (action.isNone() && longAction.isNone())
			return false;
		return startTrackingKey(event, null) != null;
	}

	public boolean onKeyUp(int keyCode, final KeyEvent event) {
		if (readerSurfaceState.isClosed())
			return false;
		if (keyCode == 0)
			keyCode = event.getScanCode();
		mActivity.onUserActivity();
		keyCode = translateKeyCode(keyCode);
		ImageViewer imageViewer = imageViewerState.current();
		if (imageViewer != null && imageViewer.isActive())
			return imageViewer.onKeyUp(keyCode, event);
		ReaderInputSettings inputSettings =
				ReaderInputSettings.capture(
						readerSettingsState.snapshot());
		if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
			if (isAutoScrollActive())
				return true;
			if (!inputSettings.areVolumeKeysEnabled())
				return false;
		}
		if (isAutoScrollActive()) {
			stopAutoScroll();
			return true;
		}
		if (keyCode == KeyEvent.KEYCODE_POWER || keyCode == KeyEvent.KEYCODE_ENDCALL) {
			mActivity.releaseBacklightControl();
			return false;
		}
		KeyRepeatState.Release keyRelease =
				releaseTrackedKey(event);
		boolean tracked = keyRelease.isTracked();
//		if ( keyCode!=KeyEvent.KEYCODE_BACK )
//			backKeyDownHere = false;

		if (keyCode == KeyEvent.KEYCODE_BACK && !tracked)
			return true;
		//backKeyDownHere = false;

		// apply orientation
		keyCode = overrideKey(keyCode);
		boolean isLongPress =
				keyRelease.isLongPress();
		ReaderAction action = findKeyAction(
				keyCode,
				ReaderAction.NORMAL,
				inputSettings);
		ReaderAction longAction = findKeyAction(
				keyCode,
				ReaderAction.LONG,
				inputSettings);
		ReaderAction dblAction = findKeyAction(
				keyCode,
				ReaderAction.DOUBLE,
				inputSettings);
		stopTracking();

/*		if ( keyCode>=KeyEvent.KEYCODE_0 && keyCode<=KeyEvent.KEYCODE_9 && tracked ) {
			// goto/set shortcut bookmark
			int shortcut = keyCode - KeyEvent.KEYCODE_0;
			if ( shortcut==0 )
				shortcut = 10;
			if ( isLongPress )
				addBookmark(shortcut);
			else
				goToBookmark(shortcut);
			return true;
		}*/
		if (action.isNone() || !tracked) {
			return false;
		}
		if (!action.isNone() && action.canRepeat() && longAction.isRepeat()) {
			// already processed by onKeyDown()
			return true;
		}

		if (isLongPress) {
			action = longAction;
		} else {
			if (!dblAction.isNone()) {
				// wait for possible double click
				KeyDoubleClickState.Pending<ReaderAction>
						pending =
						keyDoubleClickState.defer(
								keyCode,
								android.os.SystemClock
										.uptimeMillis(),
								action,
								dblAction);
				if (pending != null) {
					keyDoubleClickScheduler.postDelayed(
							() -> applyDeferredKeyAction(
									pending),
							DOUBLE_CLICK_INTERVAL);
				}
				// posted
				return true;
			}
		}
		if (!action.isNone()) {
			log.d("onKeyUp: action " + action.id + " found for key " + keyCode + (isLongPress ? " (long)" : ""));
			onAction(action);
			return true;
		}

		// not processed
		return false;
	}

	public boolean onTouchEvent(MotionEvent event) {
		if (readerSurfaceState.isClosed())
			return false;

		if (!isTouchScreenEnabled) {
			return true;
		}
		if (event.getX() == 0 && event.getY() == 0)
			return true;
		mActivity.onUserActivity();

		ImageViewer imageViewer = imageViewerState.current();
		if (imageViewer != null && imageViewer.isActive())
			return imageViewer.onTouchEvent(event);

		if (isAutoScrollActive()) {
			if (event.getAction() == MotionEvent.ACTION_DOWN) {
				int x = (int) event.getX();
				int y = (int) event.getY();
				int z = getTapZone(x, y, surface.getWidth(), surface.getHeight());
				if (z == 7)
					changeAutoScrollSpeed(-1);
				else if (z == 9)
					changeAutoScrollSpeed(1);
				else
					stopAutoScroll();
			}
			return true;
		}

		TapHandler handler = tapHandlerState.current();
		if (handler == null) {
			handler = tapHandlerState.installIfAbsent(
					new TapHandler());
		}
		if (handler == null)
			return false;
		handler.checkExpiration();
		handler = tapHandlerState.current();
		return handler != null
				&& handler.onTouchEvent(event);
	}

	@Override
	public void onFocusChange(View arg0, boolean arg1) {
		if (readerSurfaceState.isClosed())
			return;
		stopTracking();
		if (isAutoScrollActive())
			stopAutoScroll();
	}

	public void redraw() {
		BackgroundThread.instance().executeGUI(() -> {
			if (readerSurfaceState.isClosed())
				return;
			surface.invalidate();
			pageInvalidationState.invalidate();
			drawPage();
		});
	}

	public ReaderView(
			CoolReader activity,
			Engine engine,
			Scanner scanner,
			History history,
			CoverpageManager coverpageManager,
			GenresCollection genresCollection,
			DocumentFileCache documentCache,
			DocumentLoadLifecycle documentLoadLifecycle,
			ServiceLifecycle serviceLifecycle,
			Properties props) {
		//super(activity);
		log.i("Creating normal SurfaceView");
		surface = new ReaderSurface(activity);

		bookView = (BookView) surface;
		surface.setOnTouchListener(this);
		surface.setOnKeyListener(this);
		surface.setOnFocusChangeListener(this);
		doc = new DocView(Engine.lock);
		doc.setReaderCallback(readerCallback);
		SurfaceHolder holder = surface.getHolder();
		holder.addCallback(this);

		BackgroundThread.ensureGUI();
		this.mActivity = activity;
		this.mEngine = engine;
		this.mScanner = scanner;
		this.mHistory = history;
		this.mCoverpageManager = coverpageManager;
		this.mGenresCollection = genresCollection;
		this.mDocumentCache = documentCache;
		this.documentLoadLifecycle = documentLoadLifecycle;
		this.mServiceLifecycle = serviceLifecycle;
		this.mEinkScreen = activity.getEinkScreen();
		surface.setFocusable(true);
		surface.setFocusableInTouchMode(true);
		BackgroundThread.instance().postBackground(
				this::initializeNativeDocument);

		log.i("Posting create view task");
		post(new CreateViewTask(props));
	}

	private void initializeNativeDocument() {
		BackgroundThread.ensureBackground();
		if (!readerNativeLifecycle.claimCreate())
			return;
		log.d("ReaderView - in background thread: calling createInternal()");
		doc.create();
		readerNativeLifecycle.markCreated();
	}
}
