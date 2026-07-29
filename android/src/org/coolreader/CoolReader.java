/*
 * CoolReader for Android
 * Copyright (C) 2010-2015,2020,2021 Vadim Lopatin <coolreader.org@gmail.com>
 * Copyright (C) 2012 Michael Berganovsky <mike0berg@gmail.com>
 * Copyright (C) 2012 klush
 * Copyright (C) 2012 Jeff Doozan <jeff@doozan.com>
 * Copyright (C) 2018,2020 Yuri Plotnikov <plotnikovya@gmail.com>
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

// Main Class
package org.coolreader;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.UriPermission;
import android.database.Cursor;
import android.media.AudioManager;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Debug;
import android.os.ParcelFileDescriptor;
import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;
import android.provider.DocumentsContract;
import android.provider.OpenableColumns;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;

import org.coolreader.Dictionaries.DictionaryException;
import org.coolreader.crengine.AboutDialog;
import org.coolreader.crengine.BackgroundThread;
import org.coolreader.crengine.BaseActivity;
import org.coolreader.crengine.BatteryStatus;
import org.coolreader.crengine.BookInfo;
import org.coolreader.crengine.BookInfoEditDialog;
import org.coolreader.crengine.BookInfoDialogSession;
import org.coolreader.crengine.Bookmark;
import org.coolreader.crengine.BrowserViewLayout;
import org.coolreader.crengine.CRRootView;
import org.coolreader.crengine.CRToolBar;
import org.coolreader.crengine.CoverpageManager;
import org.coolreader.crengine.DeletionSnapshot;
import org.coolreader.crengine.DeviceInfo;
import org.coolreader.crengine.DocumentFileCache;
import org.coolreader.crengine.DocumentLoadLifecycle;
import org.coolreader.crengine.DocumentsContractWrapper;
import org.coolreader.crengine.DocumentFormat;
import org.coolreader.crengine.DocumentFormatDetector;
import org.coolreader.crengine.DocumentSource;
import org.coolreader.crengine.DocumentTreeRequestState;
import org.coolreader.crengine.Engine;
import org.coolreader.crengine.ErrorDialog;
import org.coolreader.crengine.ExternalDocumentValidator;
import org.coolreader.crengine.FileBrowser;
import org.coolreader.crengine.FileInfo;
import org.coolreader.crengine.FileInfoOperationListener;
import org.coolreader.crengine.FileSystemFolders;
import org.coolreader.crengine.History;
import org.coolreader.crengine.InterfaceTheme;
import org.coolreader.crengine.L;
import org.coolreader.crengine.LibraryRootStore;
import org.coolreader.crengine.LogcatExportSession;
import org.coolreader.crengine.LogcatSaver;
import org.coolreader.crengine.Logger;
import org.coolreader.crengine.OPDSCatalogEditDialog;
import org.coolreader.crengine.OptionsDialog;
import org.coolreader.crengine.ParseBudget;
import org.coolreader.crengine.PositionProperties;
import org.coolreader.crengine.Properties;
import org.coolreader.crengine.ReaderAction;
import org.coolreader.crengine.ReaderCommand;
import org.coolreader.crengine.ReaderView;
import org.coolreader.crengine.ReaderViewLayout;
import org.coolreader.crengine.Scanner;
import org.coolreader.crengine.ServiceDependencies;
import org.coolreader.crengine.ServiceLifecycle;
import org.coolreader.crengine.TTSToolbarDlg;
import org.coolreader.crengine.Utils;
import org.coolreader.db.CRDBService;
import org.coolreader.genrescollection.GenresCollection;
import org.coolreader.tts.OnTTSCreatedListener;
import org.coolreader.tts.TTSControlServiceAccessor;
import org.koekak.android.ebookdownloader.SonyBookSelector;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CoolReader extends BaseActivity {
	public static final Logger log = L.create("cr");

	private ReaderView mReaderView;
	private ReaderViewLayout mReaderFrame;
	private FileBrowser mBrowser;
	private View mBrowserTitleBar;
	private CRToolBar mBrowserToolBar;
	private BrowserViewLayout mBrowserFrame;
	private CRRootView mHomeFrame;
	private Engine mEngine;
	private Scanner mScanner;
	private History mHistory;
	private CoverpageManager mCoverpageManager;
	private DocumentFileCache mDocumentCache;
	private FileSystemFolders mFileSystemFolders;
	private GenresCollection mGenresCollection;
	private ServiceLifecycle mServiceLifecycle;
	private final DocumentLoadLifecycle documentLoadLifecycle =
			new DocumentLoadLifecycle();
	private final BookInfoDialogSession bookInfoDialogRequests =
			new BookInfoDialogSession();
	private final LogcatExportSession logcatExportRequests =
			new LogcatExportSession();
	private final ExternalDocumentValidator mExternalDocumentValidator =
			new ExternalDocumentValidator();
	//View startupView;
	//CRDB mDB;
	private ViewGroup mCurrentFrame;
	private ViewGroup mPreviousFrame;

	/*
	  Commented until the appearance of free implementation of the binding to the Google Drive (R)
	private final SyncOptions mGoogleDriveSyncOpts = new SyncOptions();
	private boolean mSyncGoogleDriveEnabledPrev = false;
	private int mSyncGoogleDriveErrorsCount = 0;
	private Synchronizer mGoogleDriveSync;
	private OnSyncStatusListener mGoogleDriveSyncStatusListener;
	private Timer mGoogleDriveAutoSaveTimer = null;
	private SyncServiceAccessor syncServiceAccessor = null;
	// can be add more synchronizers
	private boolean mSuppressSettingsCopyToCloud;
	 */

	private String mOptionAppearance = "0";

	private DocumentSource mExternalDocumentSource = null;
	private LibraryRootStore mLibraryRootStore;
	private Uri mPendingLibraryRootUri;
	private final DocumentTreeRequestState<FileInfo>
			openDocumentTreeRequests =
					new DocumentTreeRequestState<>();
	private final ActivityResultLauncher<Intent> mSelectLibraryRootLauncher =
			registerForActivityResult(
					new ActivityResultContracts.StartActivityForResult(),
					result -> handleSelectLibraryRootResult(
							result.getResultCode(), result.getData()));
	private final ActivityResultLauncher<Intent> mOpenLibraryDocumentLauncher =
			registerForActivityResult(
					new ActivityResultContracts.StartActivityForResult(),
					result -> handleOpenLibraryDocumentResult(
							result.getResultCode(), result.getData()));
	private final ActivityResultLauncher<Intent> mOpenDocumentTreeLauncher =
			registerForActivityResult(
					new ActivityResultContracts.StartActivityForResult(),
					result -> handleOpenDocumentTreeResult(
							result.getResultCode(), result.getData()));

	private BatteryStatus initialBatteryStatus =
			BatteryStatus.unavailable();

	private boolean isFirstStart = true;
	private boolean justCreated = false;
	private boolean activityIsRunning = false;
	private boolean isInterfaceCreated = false;

	private String ttsEnginePackage = "";
	private TTSControlServiceAccessor ttsControlServiceAccessor = null;

	private static final String STATE_PENDING_LIBRARY_ROOT =
			"pendingLibraryRoot";
	private static final String STATE_OPEN_DOCUMENT_TREE_COMMAND =
			"openDocumentTreeCommand";
	private static final String STATE_OPEN_DOCUMENT_TREE_ARG =
			"openDocumentTreeArg";
	private static final String STATE_OPEN_DOCUMENT_TREE_ATTEMPT =
			"openDocumentTreeAttempt";
	private static final int MAX_FOLDER_DELETE_PICKER_ATTEMPTS = 3;

	private final BroadcastReceiver batteryChangeReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			int status = intent.getIntExtra(
					BatteryManager.EXTRA_STATUS,
					BatteryManager.BATTERY_STATUS_UNKNOWN);
			int plugged = intent.getIntExtra(
					BatteryManager.EXTRA_PLUGGED, 0);
			int level = intent.getIntExtra(
					BatteryManager.EXTRA_LEVEL, 0);
			int scale = intent.getIntExtra(
					BatteryManager.EXTRA_SCALE, 100);
			// Translate android values to cr3 values
			switch (plugged) {
				case BatteryManager.BATTERY_PLUGGED_AC:
					plugged = BatteryStatus.CHARGER_AC;
					break;
				case BatteryManager.BATTERY_PLUGGED_USB:
					plugged = BatteryStatus.CHARGER_USB;
					break;
				case BatteryManager.BATTERY_PLUGGED_WIRELESS:
					plugged = BatteryStatus.CHARGER_WIRELESS;
					break;
				default:
					plugged = BatteryStatus.CHARGER_NO;
			}
			switch (status) {
				case BatteryManager.BATTERY_STATUS_CHARGING:
					status = BatteryStatus.STATE_CHARGING;
					break;
				case BatteryManager.BATTERY_STATUS_DISCHARGING:
				default:
					status = BatteryStatus.STATE_DISCHARGING;
					break;
			}
			BatteryStatus batteryStatus =
					BatteryStatus.fromRawLevel(
							status, plugged, level, scale);
			if (mReaderView != null)
				mReaderView.setBatteryStatus(batteryStatus);
			else
				initialBatteryStatus = batteryStatus;
		}
	};
	private BroadcastReceiver timeTickReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (activityIsRunning && null != mReaderView) {
				mReaderView.onTimeTickReceived();
			}
		}
	};

	/**
	 * Called when the activity is first created.
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		startServices();

		log.i("CoolReader.onCreate() entered");
		super.onCreate(savedInstanceState);
		mLibraryRootStore = new LibraryRootStore(this);
		if (savedInstanceState != null) {
			String pendingRoot = savedInstanceState.getString(
					STATE_PENDING_LIBRARY_ROOT);
			if (pendingRoot != null)
				mPendingLibraryRootUri = Uri.parse(pendingRoot);
			DocumentTreeRequestState.Command command =
					DocumentTreeRequestState.Command.fromCode(
							savedInstanceState.getInt(
									STATE_OPEN_DOCUMENT_TREE_COMMAND,
									-1));
			FileInfo argument =
					savedInstanceState.getParcelable(
							STATE_OPEN_DOCUMENT_TREE_ARG);
			int attempt =
					savedInstanceState.getInt(
							STATE_OPEN_DOCUMENT_TREE_ATTEMPT,
							0);
			openDocumentTreeRequests.begin(
					command,
					argument,
					attempt);
		}

		isFirstStart = true;
		justCreated = true;
		activityIsRunning = false;
		isInterfaceCreated = false;

		ServiceDependencies dependencies = getServiceDependencies();
		mEngine = dependencies.getEngine();
		mScanner = dependencies.getScanner();
		mHistory = dependencies.getHistory();
		mCoverpageManager = dependencies.getCoverpageManager();
		mDocumentCache = dependencies.getDocumentCache();
		mFileSystemFolders = dependencies.getFileSystemFolders();
		mGenresCollection = dependencies.getGenresCollection();
		mServiceLifecycle = dependencies.getLifecycle();

		// Service-backed settings require the captured generation.
		onSettingsChanged(settings(), null);

		//requestWindowFeature(Window.FEATURE_NO_TITLE);

		// Get battery level
		// ACTION_BATTERY_CHANGED is a sticky broadcast & we pass null instead of receiver, then
		// no receiver is registered -- the function simply returns the sticky Intent that matches filter.
		Intent intent = ContextCompat.registerReceiver(
				this, null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED),
				ContextCompat.RECEIVER_NOT_EXPORTED);
		if (null != intent) {
			// and process this Intent: save received values
			batteryChangeReceiver.onReceive(null, intent);
		}

		// For TTS volume control
		//  See TTSControlService
		setVolumeControlStream(AudioManager.STREAM_MUSIC);

		showRootWindow();

		log.i("CoolReader.onCreate() exiting");
	}

	public final static boolean CLOSE_BOOK_ON_STOP = false;

	boolean mDestroyed = false;

	@Override
	protected void onDestroy() {

		log.i("CoolReader.onDestroy() entered");
		if (mReaderView != null) {
			mReaderView.stopTtsForDocumentChange();
			if (!CLOSE_BOOK_ON_STOP)
				mReaderView.close();
		}
		documentLoadLifecycle.close();
		bookInfoDialogRequests.close();
		logcatExportRequests.close();

		// Shutdown TTS service if running
		if (null != ttsControlServiceAccessor) {
			ttsControlServiceAccessor.unbind();
			ttsControlServiceAccessor = null;
		}

		/*
		  Commented until the appearance of free implementation of the binding to the Google Drive (R)
		// Unbind from Cloud Sync service
		if (null != syncServiceAccessor) {
			syncServiceAccessor.unbind();
			syncServiceAccessor = null;
		}
		 */

		if (mHomeFrame != null)
			mHomeFrame.onClose();
		if (mBrowser != null) {
			mBrowser.onClose();
			mBrowser = null;
		}
		mDestroyed = true;

		//if ( mReaderView!=null )
		//	mReaderView.close();

		//if ( mHistory!=null && mDB!=null ) {
		//history.saveToDB();
		//}


//		if ( BackgroundThread.instance()!=null ) {
//			BackgroundThread.instance().quit();
//		}

		//mEngine = null;

		if (mReaderView != null) {
			mReaderView.destroy();
		}
		mReaderView = null;

		log.i("CoolReader.onDestroy() exiting");
		super.onDestroy();

		stopServices();
	}

	public ReaderView getReaderView() {
		return mReaderView;
	}

	// Absolute screen rotation
	int screenRotation = Surface.ROTATION_0;

	@Override
	protected void onScreenRotationChanged(int rotation) {
		screenRotation = rotation;
		if (null != mReaderView) {
			mReaderView.doEngineCommand(ReaderCommand.DCMD_SET_ROTATION_INFO_FOR_AA, rotation);
		}
	}

	@Override
	public void applyAppSetting(String key, String value) {
		super.applyAppSetting(key, value);
		boolean flg = "1".equals(value);
		if (key.equals(PROP_APP_DICTIONARY)) {
			setDict(value);
		} else if (key.equals(PROP_APP_DICTIONARY_2)) {
			setDict2(value);
		} else if (key.equals(PROP_TOOLBAR_APPEARANCE)) {
			setToolbarAppearance(value);
		} else if (key.equals(PROP_APP_BOOK_SORT_ORDER)) {
			if (mBrowser != null)
				mBrowser.setSortOrder(value);
		} else if (key.equals(PROP_APP_SHOW_COVERPAGES)) {
			if (mBrowser != null)
				mBrowser.setCoverPagesEnabled(flg);
		} else if (key.equals(PROP_APP_BOOK_PROPERTY_SCAN_ENABLED)) {
			mScanner.setDirScanEnabled(flg);
		} else if (key.equals(PROP_FONT_FACE)) {
			if (mBrowser != null)
				mBrowser.setCoverPageFontFace(value);
		} else if (key.equals(PROP_APP_COVERPAGE_SIZE)) {
			if (mBrowser != null)
				mBrowser.setCoverPageSizeOption(Utils.parseInt(value, 0, 0, 2));
		} else if (key.equals(PROP_APP_FILE_BROWSER_SIMPLE_MODE)) {
			if (mBrowser != null)
				mBrowser.setSimpleViewMode(flg);
		}
		/* See notes for buildGoogleDriveSynchronizer() function
		else if (key.equals(PROP_APP_CLOUDSYNC_GOOGLEDRIVE_ENABLED)) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
				mSyncGoogleDriveEnabledPrev = mGoogleDriveSyncOpts.Enabled;
				mGoogleDriveSyncOpts.Enabled = flg;
				updateGoogleDriveSynchronizer();
			}
		} else if (key.equals(PROP_APP_CLOUDSYNC_CONFIRMATIONS)) {
			mGoogleDriveSyncOpts.AskConfirmations = flg;
		} else if (key.equals(PROP_APP_CLOUDSYNC_GOOGLEDRIVE_SETTINGS)) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
				mGoogleDriveSyncOpts.SyncSettings = flg;
				updateGoogleDriveSynchronizer();
			}
		} else if (key.equals(PROP_APP_CLOUDSYNC_GOOGLEDRIVE_BOOKMARKS)) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
				mGoogleDriveSyncOpts.SyncBookmarks = flg;
				updateGoogleDriveSynchronizer();
			}
		} else if (key.equals(PROP_APP_CLOUDSYNC_GOOGLEDRIVE_CURRENTBOOK_INFO)) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
				mGoogleDriveSyncOpts.SyncCurrentBookInfo = flg;
				updateGoogleDriveSynchronizer();
			}
		} else if (key.equals(PROP_APP_CLOUDSYNC_GOOGLEDRIVE_CURRENTBOOK_BODY)) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
				mGoogleDriveSyncOpts.SyncCurrentBookBody = flg;
				updateGoogleDriveSynchronizer();
			}
		} else if (key.equals(PROP_APP_CLOUDSYNC_GOOGLEDRIVE_AUTOSAVEPERIOD)) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
				mGoogleDriveSyncOpts.AutoSavePeriod = Utils.parseInt(value, 0, 0, 30);
				updateGoogleDriveSynchronizer();
			}
		} else if (key.equals(PROP_APP_CLOUDSYNC_DATA_KEEPALIVE)) {
			mGoogleDriveSyncOpts.DataKeepAlive = Utils.parseInt(value, 14, 0, 365);
			updateGoogleDriveSynchronizer();
		} */
		else if (key.equals(PROP_APP_FILE_BROWSER_HIDE_EMPTY_FOLDERS)) {
			// already in super method:
			// mScanner.setHideEmptyDirs(flg);
			// Here only refresh the file browser
			if (null != mBrowser) {
				mBrowser.showLastDirectory();
			}
		} else if (key.equals(PROP_APP_FILE_BROWSER_HIDE_EMPTY_GENRES)) {
			if (null != mBrowser) {
				mBrowser.setHideEmptyGenres(flg);
			}
		} else if (key.equals(PROP_APP_TTS_ENGINE)) {
			ttsEnginePackage = value;
			if (null != mReaderView && mReaderView.isTTSActive()) {
				// Set new TTS engine if running
				initTTS(null);
			}
		}
		//
	}

	/*
	 * NOTE: Unfortunately, Services Google Play has a proprietary license,
	 * so we cannot use it in the program under GPL license.
	 * This code must be rewritten using free libraries compatible with
	 * the GPL license or write its implementation from scratch.
	private void buildGoogleDriveSynchronizer() {
		if (null != syncServiceAccessor && null != mGoogleDriveSync) {
			if (!syncServiceAccessor.isServiceBound()) {
				// lost connection to service, nullify sync instance
				mGoogleDriveSync = null;
			}
		}
		if (null != mGoogleDriveSync)
			return;
		// build synchronizer instance
		// DeviceInfo.getSDKLevel() not applicable here -> compile error about Android API compatibility
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			GoogleDriveRemoteAccess googleDriveRemoteAccess = new GoogleDriveRemoteAccess(this, 30);
			mGoogleDriveSync = new Synchronizer(this, mScanner, googleDriveRemoteAccess, getString(R.string.app_name), REQUEST_CODE_GOOGLE_DRIVE_SIGN_IN);
			mGoogleDriveSyncStatusListener = new OnSyncStatusListener() {
				@Override
				public void onSyncStarted(Synchronizer.SyncDirection direction, boolean showProgress, boolean interactively) {
					if (Synchronizer.SyncDirection.SyncFrom == direction) {
						log.d("Starting synchronization from Google Drive");
					} else if (Synchronizer.SyncDirection.SyncTo == direction) {
						log.d("Starting synchronization to Google Drive");
					}
					if (null != mReaderView) {
						if (showProgress) {
							mReaderView.showCloudSyncProgress(100);
						}
					}
				}

				@Override
				public void OnSyncProgress(Synchronizer.SyncDirection direction, boolean showProgress, int current, int total, boolean interactively) {
					log.v("sync progress: current=" + current + "; total=" + total);
					if (null != mReaderView) {
						if (showProgress) {
							int total_ = total;
							if (current > total_)
								total_ = current;
							mReaderView.showCloudSyncProgress(10000 * current / total_);
						}
					}
				}

				@Override
				public void onSyncCompleted(Synchronizer.SyncDirection direction, boolean showProgress, boolean interactively) {
					if (Synchronizer.SyncDirection.SyncFrom == direction) {
						log.d("Google Drive SyncFrom successfully completed");
					} else if (Synchronizer.SyncDirection.SyncTo == direction) {
						log.d("Google Drive SyncTo successfully completed");
					}
					if (interactively)
						showToast(R.string.googledrive_sync_completed);
					if (showProgress) {
						if (null != mReaderView) {
							// Hide sync indicator
							mReaderView.hideCloudSyncProgress();
						}
					}
					if (mGoogleDriveSyncOpts.Enabled)
						mSyncGoogleDriveErrorsCount = 0;
				}

				@Override
				public void onSyncError(Synchronizer.SyncDirection direction, String errorString) {
					// Hide sync indicator
					if (null != mReaderView) {
						mReaderView.hideCloudSyncProgress();
					}
					if (null != errorString)
						showToast(R.string.googledrive_sync_failed_with, errorString);
					else
						showToast(R.string.googledrive_sync_failed);
					if (mGoogleDriveSyncOpts.Enabled) {
						mSyncGoogleDriveErrorsCount++;
						if (mSyncGoogleDriveErrorsCount >= 3) {
							showToast(R.string.googledrive_sync_failed_disabled);
							log.e("More than 3 sync failures in a row, auto sync disabled.");
							mGoogleDriveSyncOpts.Enabled = false;
						}
					}
				}

				@Override
				public void onAborted(Synchronizer.SyncDirection direction) {
					// Hide sync indicator
					if (null != mReaderView) {
						mReaderView.hideCloudSyncProgress();
					}
					showToast(R.string.googledrive_sync_aborted);
				}

				@Override
				public void onSettingsLoaded(Properties settings, boolean interactively) {
					// Apply downloaded (filtered) settings
					mSuppressSettingsCopyToCloud = true;
					mergeSettings(settings, true);
				}

				@Override
				public void onBookmarksLoaded(BookInfo bookInfo, boolean interactively) {
					waitForCRDBService(() -> {
						// TODO: ask the user whether to import new bookmarks.
						BookInfo currentBook = null;
						int currentPos = -1;
						if (null != mReaderView) {
							currentBook = mReaderView.getBookInfo();
							if (null != currentBook) {
								Bookmark lastPos = currentBook.getLastPosition();
								if (null != lastPos)
									currentPos = lastPos.getPercent();
							}
						}
						mHistory.updateBookInfo(bookInfo);
						getDB().saveBookInfo(bookInfo);
						if (null != currentBook) {
							FileInfo currentFileInfo = currentBook.getFileInfo();
							if (null != currentFileInfo) {
								if (currentFileInfo.baseEquals((bookInfo.getFileInfo()))) {
									// if the book indicated by the bookInfo is currently open.
									Bookmark lastPos = bookInfo.getLastPosition();
									if (null != lastPos) {
										if (!interactively) {
											mReaderView.goToBookmark(lastPos);
										} else {
											if (Math.abs(currentPos - lastPos.getPercent()) > 10) {		// 0.1%
												askQuestion(R.string.cloud_synchronization_from_, R.string.sync_confirmation_new_reading_position,
														() -> mReaderView.goToBookmark(lastPos), null);
											}
										}
									}
								}
							}
						}
					});
				}

				@Override
				public void onCurrentBookInfoLoaded(FileInfo fileInfo, boolean interactively) {
					FileInfo current = null;
					if (null != mReaderView) {
						BookInfo bookInfo = mReaderView.getBookInfo();
						if (null != bookInfo)
							current = bookInfo.getFileInfo();
					}
					if (!fileInfo.baseEquals(current)) {
						if (!interactively) {
							loadDocument(fileInfo, false);
						} else {
							String shortBookInfo = "";
							if (null != fileInfo.authors && !fileInfo.authors.isEmpty())
								shortBookInfo = "\"" + fileInfo.authors + ", ";
							else
								shortBookInfo = "\"";
							shortBookInfo += fileInfo.title + "\"";
							String question = getString(R.string.sync_confirmation_other_book, shortBookInfo);
							askQuestion(getString(R.string.cloud_synchronization_from_), question, () -> loadDocument(fileInfo, false), null);
						}
					}
				}

				@Override
				public void onFileNotFound(FileInfo fileInfo) {
					if (null == fileInfo)
						return;
					String docInfo = "Unknown";
					if (null != fileInfo.title && !fileInfo.authors.isEmpty())
						docInfo = fileInfo.title;
					if (null != fileInfo.authors && !fileInfo.authors.isEmpty())
						docInfo = fileInfo.authors + ", " + docInfo;
					if (null != fileInfo.filename && !fileInfo.filename.isEmpty())
						docInfo += " (" + fileInfo.filename + ")";
					showToast(R.string.sync_info_no_such_document, docInfo);
				}
			};
			syncServiceAccessor = new SyncServiceAccessor(this);
		}
	}

	private void updateGoogleDriveSynchronizer() {
		// DeviceInfo.getSDKLevel() not applicable here -> lint error about Android API compatibility
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			if (mGoogleDriveSyncOpts.Enabled) {
				if (null == mGoogleDriveSync) {
					log.d("Google Drive sync is enabled.");
					buildGoogleDriveSynchronizer();
				}
				mGoogleDriveSync.setTarget(Synchronizer.SyncTarget.SETTINGS, mGoogleDriveSyncOpts.SyncSettings);
				mGoogleDriveSync.setTarget(Synchronizer.SyncTarget.BOOKMARKS, mGoogleDriveSyncOpts.SyncBookmarks);
				mGoogleDriveSync.setTarget(Synchronizer.SyncTarget.CURRENTBOOKINFO, mGoogleDriveSyncOpts.SyncCurrentBookInfo);
				mGoogleDriveSync.setTarget(Synchronizer.SyncTarget.CURRENTBOOKBODY, mGoogleDriveSyncOpts.SyncCurrentBookBody);
				mGoogleDriveSync.setBookmarksKeepAlive(mGoogleDriveSyncOpts.DataKeepAlive);
				if (null != mGoogleDriveAutoSaveTimer) {
					mGoogleDriveAutoSaveTimer.cancel();
					mGoogleDriveAutoSaveTimer = null;
				}
				if (mGoogleDriveSyncOpts.AutoSavePeriod > 0) {
					mGoogleDriveAutoSaveTimer = new Timer();
					mGoogleDriveAutoSaveTimer.schedule(new TimerTask() {
						@Override
						public void run() {
							if (activityIsRunning && null != mGoogleDriveSync) {
								//mGoogleDriveSync.startSyncTo(getCurrentBookInfo(), Synchronizer.SYNC_FLAG_QUIETLY);
								syncServiceAccessor.bind(sync -> {
									sync.setSynchronizer(mGoogleDriveSync);
									sync.setOnSyncStatusListener(mGoogleDriveSyncStatusListener);
									sync.startSyncTo(getCurrentBookInfo(), Synchronizer.SYNC_FLAG_QUIETLY);
								});
							}
						}
					}, mGoogleDriveSyncOpts.AutoSavePeriod * 60000L, mGoogleDriveSyncOpts.AutoSavePeriod * 60000L);
				}
			} else {
				if (null != mGoogleDriveAutoSaveTimer) {
					mGoogleDriveAutoSaveTimer.cancel();
					mGoogleDriveAutoSaveTimer = null;
				}
				if (mSyncGoogleDriveEnabledPrev && null != mGoogleDriveSync) {
					log.d("Google Drive autosync is disabled.");
					if (false) {
						// TODO: Don't remove authorization on Google Account here, move this into OptionsDialog
						// ask user: cleanup & sign out
						askConfirmation(R.string.googledrive_disabled_cleanup_question,
								() -> {
									if (null != mGoogleDriveSync) {
										mGoogleDriveSync.abort(() -> {
											if (null != mGoogleDriveSync) {
												mGoogleDriveSync.cleanupAndSignOut();
												mGoogleDriveSync = null;
											}
										});
									}
								},
								() -> {
									if (null != mGoogleDriveSync) {
										mGoogleDriveSync.abort(() -> {
											if (null != mGoogleDriveSync) {
												mGoogleDriveSync.signOut();
												mGoogleDriveSync = null;
											}
										});
									}
								}
						);
					}
				}
			}
		}
	}

	public void forceSyncToGoogleDrive() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			if (null == mGoogleDriveSync)
				buildGoogleDriveSynchronizer();
			mGoogleDriveSync.setBookmarksKeepAlive(mGoogleDriveSyncOpts.DataKeepAlive);
			//mGoogleDriveSync.startSyncTo(getCurrentBookInfo(), Synchronizer.SYNC_FLAG_SHOW_SIGN_IN | Synchronizer.SYNC_FLAG_FORCE | Synchronizer.SYNC_FLAG_SHOW_PROGRESS | Synchronizer.SYNC_FLAG_ASK_CHANGED);
			syncServiceAccessor.bind(sync -> {
				sync.setSynchronizer(mGoogleDriveSync);
				sync.setOnSyncStatusListener(mGoogleDriveSyncStatusListener);
				sync.startSyncTo(getCurrentBookInfo(), Synchronizer.SYNC_FLAG_SHOW_SIGN_IN | Synchronizer.SYNC_FLAG_FORCE | Synchronizer.SYNC_FLAG_SHOW_PROGRESS | Synchronizer.SYNC_FLAG_ASK_CHANGED);
			});
		}
	}

	public void forceSyncFromGoogleDrive() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			if (null == mGoogleDriveSync)
				buildGoogleDriveSynchronizer();
			mGoogleDriveSync.setBookmarksKeepAlive(mGoogleDriveSyncOpts.DataKeepAlive);
			//mGoogleDriveSync.startSyncFrom(Synchronizer.SYNC_FLAG_SHOW_SIGN_IN | Synchronizer.SYNC_FLAG_FORCE | Synchronizer.SYNC_FLAG_SHOW_PROGRESS | Synchronizer.SYNC_FLAG_ASK_CHANGED);
			syncServiceAccessor.bind(sync -> {
				sync.setSynchronizer(mGoogleDriveSync);
				sync.setOnSyncStatusListener(mGoogleDriveSyncStatusListener);
				sync.startSyncFrom(Synchronizer.SYNC_FLAG_SHOW_SIGN_IN | Synchronizer.SYNC_FLAG_FORCE | Synchronizer.SYNC_FLAG_SHOW_PROGRESS | Synchronizer.SYNC_FLAG_ASK_CHANGED);
			});
		}
	}
	*/

	private BookInfo getCurrentBookInfo() {
		BookInfo bookInfo = null;
		if (mReaderView != null) {
			bookInfo = mReaderView.getBookInfo();
			if (null != bookInfo && null == bookInfo.getFileInfo()) {
				// nullify if fileInfo is null
				bookInfo = null;
			}
		}
		return bookInfo;
	}

	@Override
	public void setFullscreen(boolean fullscreen) {
		super.setFullscreen(fullscreen);
		if (mReaderFrame != null)
			mReaderFrame.updateFullscreen(fullscreen);
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		log.i("onNewIntent : " + intent);
		if (mDestroyed) {
			log.e("engine is already destroyed");
			return;
		}
		processIntent(intent);
	}

	private boolean processIntent(Intent intent) {
		log.d("intent=" + intent);
		if (intent == null)
			return false;
		DocumentSource sourceToOpen = null;
		mExternalDocumentSource = null;
		Uri uri = null;
		if (Intent.ACTION_VIEW.equals(intent.getAction())) {
			uri = intent.getData();
			boolean persistedReadPermission =
					persistReadPermission(uri, intent.getFlags());
			intent.setData(null);
			if (uri != null) {
				String localPath = filePathFromUri(uri);
				if (localPath != null)
					sourceToOpen = DocumentSource.fromLegacyLocation(localPath);
				else if (ContentResolver.SCHEME_CONTENT.equalsIgnoreCase(
						uri.getScheme()))
					sourceToOpen = DocumentSource.contentUri(
							uri.toString(), persistedReadPermission);
			}
		} else {
			for (ReaderAction ra: ReaderAction.availableActions()) {
				String raIntentName = "org.coolreader.cmd." + ra.id;
				if (raIntentName.equals(intent.getAction())) {
					mReaderView.onCommand(ra.cmd, ra.param, null);
					return true;
				}
			}
		}

		if (sourceToOpen == null && intent.getExtras() != null) {
			log.d("Open intent contains extras");
			String fileToOpen = intent.getExtras().getString(OPEN_FILE_PARAM);
			if (fileToOpen != null)
				sourceToOpen = DocumentSource.fromLegacyLocation(fileToOpen);
		}
		if (sourceToOpen != null) {
			sourceToOpen = mExternalDocumentValidator.validate(
					sourceToOpen, intent.getType());
			if (sourceToOpen == null) {
				showToast(R.string.unsupported_document_format);
				showRootWindow();
				return true;
			}
			mExternalDocumentSource = sourceToOpen;
			log.d("DOCUMENT_TO_OPEN = "
					+ safePathForLog(sourceToOpen.getIdentity()));
			final String errorLabel = sourceToOpen.getDisplayName() != null
					? sourceToOpen.getDisplayName()
					: sourceToOpen.getIdentity();
			loadDocument(sourceToOpen, null, () ->
					BackgroundThread.instance().postGUI(() -> {
				// if document not loaded show error & then root window
				ErrorDialog errDialog = new ErrorDialog(
						CoolReader.this,
						CoolReader.this.getString(R.string.error),
						CoolReader.this.getString(
								R.string.cant_open_file, errorLabel));
				errDialog.setOnDismissListener(dialog -> showRootWindow());
				errDialog.show();
			}, 500), true);
			return true;
		} else {
			log.d("No file to open");
			return false;
		}
	}

	private String filePathFromUri(Uri uri) {
		if (null == uri)
			return null;
		String filePath = null;
		String scheme = uri.getScheme();
		String host = uri.getHost();
		if ("file".equals(scheme)) {
			filePath = uri.getPath();
			// patch for opening of books from ReLaunch (under Nook Simple Touch)
			if (null != filePath) {
				if (filePath.contains("%2F"))
					filePath = filePath.replace("%2F", "/");
			}
		} else if ("content".equals(scheme)) {
			String encodedPath = uri.getEncodedPath();
			if (encodedPath != null && encodedPath.contains("%00"))
				filePath = encodedPath;
			else
				filePath = uri.getPath();
			if (null != filePath) {
				// parse uri from system filemanager
				if (filePath.contains("%00")) {
					// splitter between archive file name and inner file.
					filePath = filePath.replace("%00", "@/");
					filePath = Uri.decode(filePath);
				}
				if ("com.google.android.apps.nbu.files.provider".equals(host)) {
					// application "Files" by Google, package="com.google.android.apps.nbu.files"
					if (filePath.startsWith("/1////")) {
						// skip "/1///"
						filePath = filePath.substring(5);
						filePath = Uri.decode(filePath);
					} else if (filePath.startsWith("/1/file:///")) {
						// skip "/1/file://"
						filePath = filePath.substring(10);
						filePath = Uri.decode(filePath);
					}
				} else {
					// Try some common conversions...
					if (filePath.startsWith("/file%3A%2F%2F")) {
						filePath = filePath.substring(14);
						filePath = Uri.decode(filePath);
						if (filePath.contains("%20")) {
							filePath = filePath.replace("%20", " ");
						}
					}
				}
			}
		}
		if (null != filePath) {
			File file;
			int pos = filePath.indexOf("@/");
			if (pos > 0)
				file = new File(filePath.substring(0, pos));
			else
				file = new File(filePath);
			if (!file.exists())
				filePath = null;
		}
		return filePath;
	}

	@Override
	protected void onPause() {
		activityIsRunning = false;
		if (mReaderView != null) {
			mReaderView.onAppPause();
		}
		if (mBrowser != null) {
			mBrowser.stopCurrentScan();
		}
		try {
			unregisterReceiver(batteryChangeReceiver);
		} catch (IllegalArgumentException e) {
			log.e("Failed to unregister receiver: " + e.toString());
		}
		try {
			unregisterReceiver(timeTickReceiver);
		} catch (IllegalArgumentException e) {
			log.e("Failed to unregister receiver: " + e.toString());
		}
		if (mHomeFrame != null)
			mHomeFrame.onPause();
		/*
		  Commented until the appearance of free implementation of the binding to the Google Drive (R)
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			if (mGoogleDriveSyncOpts.Enabled && mGoogleDriveSync != null) {
				//mGoogleDriveSync.startSyncTo(getCurrentBookInfo(), Synchronizer.SYNC_FLAG_QUIETLY | Synchronizer.SYNC_FLAG_SHOW_PROGRESS);
				syncServiceAccessor.bind(sync -> {
					sync.setSynchronizer(mGoogleDriveSync);
					sync.setOnSyncStatusListener(mGoogleDriveSyncStatusListener);
					sync.startSyncTo(getCurrentBookInfo(), Synchronizer.SYNC_FLAG_QUIETLY | Synchronizer.SYNC_FLAG_SHOW_PROGRESS);
				});
				try {
					// start SyncService to prevent service destroying on unbinding in onDestroy()
					Intent intent = new Intent(SyncService.SYNC_ACTION_NOOP, Uri.EMPTY, this, SyncService.class);
					startService(intent);
				} catch (Exception ignored) {}
			}
		}
		 */
		super.onPause();
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		log.i("CoolReader.onPostCreate()");
		super.onPostCreate(savedInstanceState);
	}

	@Override
	protected void onPostResume() {
		log.i("CoolReader.onPostResume()");
		super.onPostResume();
	}

	//	private boolean restarted = false;
	@Override
	protected void onRestart() {
		log.i("CoolReader.onRestart()");
		//restarted = true;
		super.onRestart();
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		log.i("CoolReader.onRestoreInstanceState()");
		super.onRestoreInstanceState(savedInstanceState);
	}

	@Override
	protected void onResume() {
		if (null == mExternalDocumentSource)
			log.i("CoolReader.onResume()");
		else
			log.i("CoolReader.onResume(), externalDocumentSource="
					+ safePathForLog(mExternalDocumentSource.getIdentity()));
		super.onResume();
		//Properties props = SettingsManager.instance(this).get();

		if (mReaderView != null)
			mReaderView.onAppResume();
		if (mHomeFrame != null)
			mHomeFrame.onResume();
		// ACTION_BATTERY_CHANGED: This is a sticky broadcast containing the charging state, level, and other information about the battery.
		Intent intent = ContextCompat.registerReceiver(
				this, batteryChangeReceiver,
				new IntentFilter(Intent.ACTION_BATTERY_CHANGED),
				ContextCompat.RECEIVER_NOT_EXPORTED);
		if (null != intent) {
			// process this Intent
			batteryChangeReceiver.onReceive(null, intent);
		}
		// ACTION_TIME_TICK: The current time has changed. Sent every minute.
		ContextCompat.registerReceiver(
				this, timeTickReceiver, new IntentFilter(Intent.ACTION_TIME_TICK),
				ContextCompat.RECEIVER_NOT_EXPORTED);

		if (DeviceInfo.EINK_SCREEN) {
			if (DeviceInfo.EINK_SONY) {
				SharedPreferences pref = getSharedPreferences(PREF_FILE, 0);
				String res = pref.getString(PREF_LAST_BOOK, null);
				if (res != null && res.length() > 0) {
					SonyBookSelector selector = new SonyBookSelector(this);
					long l = selector.getContentId(res);
					if (l != 0) {
						selector.setReadingTime(l);
						selector.requestBookSelection(l);
					}
				}
			}
		}
		/*
		  Commented until the appearance of free implementation of the binding to the Google Drive (R)
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			if (mGoogleDriveSyncOpts.Enabled && mGoogleDriveSync != null) {
				// when the program starts, the local settings file is already updated, so the local file is always newer than the remote one
				// Therefore, the synchronization mode is quiet, i.e. without comparing modification times and without prompting the user for action.
				// If the file is opened from an external file manager, we must disable the "currently reading book" sync operation with google drive.
				if (null == mExternalDocumentSource) {
					//mGoogleDriveSync.startSyncFrom(Synchronizer.SYNC_FLAG_SHOW_SIGN_IN | Synchronizer.SYNC_FLAG_QUIETLY | Synchronizer.SYNC_FLAG_SHOW_PROGRESS | (mGoogleDriveSyncOpts.AskConfirmations ? Synchronizer.SYNC_FLAG_ASK_CHANGED : 0));
					syncServiceAccessor.bind(sync -> {
						sync.setSynchronizer(mGoogleDriveSync);
						sync.setOnSyncStatusListener(mGoogleDriveSyncStatusListener);
						sync.startSyncFrom(Synchronizer.SYNC_FLAG_SHOW_SIGN_IN | Synchronizer.SYNC_FLAG_QUIETLY | Synchronizer.SYNC_FLAG_SHOW_PROGRESS | (mGoogleDriveSyncOpts.AskConfirmations ? Synchronizer.SYNC_FLAG_ASK_CHANGED : 0));
					});
				} else {
					//mGoogleDriveSync.startSyncFromOnly(Synchronizer.SYNC_FLAG_SHOW_SIGN_IN | Synchronizer.SYNC_FLAG_QUIETLY | Synchronizer.SYNC_FLAG_SHOW_PROGRESS | (mGoogleDriveSyncOpts.AskConfirmations ? Synchronizer.SYNC_FLAG_ASK_CHANGED : 0), Synchronizer.SyncTarget.SETTINGS, Synchronizer.SyncTarget.BOOKMARKS);
					syncServiceAccessor.bind(sync -> {
						sync.setSynchronizer(mGoogleDriveSync);
						sync.setOnSyncStatusListener(mGoogleDriveSyncStatusListener);
						sync.startSyncFromOnly(Synchronizer.SYNC_FLAG_SHOW_SIGN_IN | Synchronizer.SYNC_FLAG_QUIETLY | Synchronizer.SYNC_FLAG_SHOW_PROGRESS | (mGoogleDriveSyncOpts.AskConfirmations ? Synchronizer.SYNC_FLAG_ASK_CHANGED : 0), Synchronizer.SyncTarget.SETTINGS, Synchronizer.SyncTarget.BOOKMARKS);
					});
				}
			}
		}
		 */
		activityIsRunning = true;
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		log.i("CoolReader.onSaveInstanceState()");
		if (mPendingLibraryRootUri != null) {
			outState.putString(
					STATE_PENDING_LIBRARY_ROOT,
					mPendingLibraryRootUri.toString());
		}
		DocumentTreeRequestState.Request<FileInfo> treeRequest =
				openDocumentTreeRequests.peek();
		if (treeRequest != null) {
			outState.putInt(
					STATE_OPEN_DOCUMENT_TREE_COMMAND,
					treeRequest.getCommand().getCode());
			outState.putParcelable(
					STATE_OPEN_DOCUMENT_TREE_ARG,
					treeRequest.getArgument());
			outState.putInt(
					STATE_OPEN_DOCUMENT_TREE_ATTEMPT,
					treeRequest.getAttempt());
		}
		super.onSaveInstanceState(outState);
	}

	static final boolean LOAD_LAST_DOCUMENT_ON_START = true;

	@Override
	protected void onStart() {
		log.i("CoolReader.onStart() version=" + getVersion());
		super.onStart();

		//		BackgroundThread.instance().postGUI(new Runnable() {
//			public void run() {
//				// fixing font settings
//				Properties settings = mReaderView.getSettings();
//				if (SettingsManager.instance(CoolReader.this).fixFontSettings(settings)) {
//					log.i("Missing font settings were fixed");
//					mBrowser.setCoverPageFontFace(settings.getProperty(ReaderView.PROP_FONT_FACE, DeviceInfo.DEF_FONT_FACE));
//					mReaderView.setSettings(settings, null);
//				}
//			}
//		});

		if (mHomeFrame == null) {
			waitForCRDBService(() -> {
				if (!mServiceLifecycle.isActive()
						|| mDestroyed)
					return;
				mHistory.loadFromDB(getDB(), 200);

				mHomeFrame = new CRRootView(
						CoolReader.this,
						mScanner,
						mHistory,
						mCoverpageManager,
						mFileSystemFolders);
				if (activityIsRunning)
					mHomeFrame.onResume();
				mHomeFrame.requestFocus();

				showRootWindow();
				setSystemUiVisibility();

				notifySettingsChanged();

				showNotifications();

				isInterfaceCreated = true;
			});
		}

		if (isBookOpened()) {
			showOpenedBook();
			return;
		}

		if (!isFirstStart)
			return;
		isFirstStart = false;

		if (justCreated) {
			justCreated = false;
			if (!processIntent(getIntent()))
				showLastLocation();
		}
		stopped = false;

		log.i("CoolReader.onStart() exiting");
	}


	private boolean stopped = false;

	@Override
	protected void onStop() {
		log.i("CoolReader.onStop() entering");
		// Donations support code
		super.onStop();
		stopped = true;
		// will close book at onDestroy()
		if (CLOSE_BOOK_ON_STOP)
			mReaderView.close();

		log.i("CoolReader.onStop() exiting");
	}

	private static String dumpFields(Field[] fields, Object obj) {
		StringBuilder buf = new StringBuilder();
		try {
			for (Field f : fields) {
				if (buf.length() > 0)
					buf.append(", ");
				buf.append(f.getName());
				buf.append("=");
				buf.append(f.get(obj));
			}
		} catch (Exception e) {

		}
		return buf.toString();
	}

	public static void dumpHeapAllocation() {
		final Debug.MemoryInfo info = new Debug.MemoryInfo();
		Debug.getMemoryInfo(info);
		log.d(
				"nativeHeapAlloc=" + Debug.getNativeHeapAllocatedSize()
						+ ", nativeHeapSize=" + Debug.getNativeHeapSize()
						+ ", info: "
						+ dumpFields(
								Debug.MemoryInfo.class.getFields(), info));
	}


	@Override
	public void setCurrentTheme(InterfaceTheme theme) {
		super.setCurrentTheme(theme);
		if (mHomeFrame != null)
			mHomeFrame.onThemeChange(theme);
		if (mBrowser != null)
			mBrowser.onThemeChanged();
		if (mBrowserFrame != null)
			mBrowserFrame.onThemeChanged(theme);
		//getWindow().setBackgroundDrawable(theme.getActionBarBackgroundDrawableBrowser());
	}

	public void directoryUpdated(FileInfo dir, FileInfo selected) {
		if (dir.isOPDSRoot())
			mHomeFrame.refreshOnlineCatalogs();
		else if (dir.isRecentDir())
			mHomeFrame.refreshRecentBooks();
		if (mBrowser != null)
			mBrowser.refreshDirectory(dir, selected);
	}

	public void directoryUpdated(FileInfo dir) {
		directoryUpdated(dir, null);
	}

	@Override
	public void onSettingsChanged(Properties props, Properties oldProps) {
		Properties changedProps = oldProps != null ? props.diff(oldProps) : props;
		if (mHomeFrame != null) {
			mHomeFrame.refreshOnlineCatalogs();
		}
		if (mReaderFrame != null) {
			mReaderFrame.updateSettings(props);
			if (mReaderView != null)
				mReaderView.updateSettings(props);
		}
		for (Map.Entry<Object, Object> entry : changedProps.entrySet()) {
			String key = (String) entry.getKey();
			String value = (String) entry.getValue();
			applyAppSetting(key, value);
		}
		// Show/Hide soft navbar after OptionDialog is closed.
		applyFullscreen(getWindow());
		if (!justCreated && isInterfaceCreated) {
			// Only after onStart()!
			/*
			  Commented until the appearance of free implementation of the binding to the Google Drive (R)
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
				if (mGoogleDriveSyncOpts.Enabled && !mSyncGoogleDriveEnabledPrev && null != mGoogleDriveSync) {
					// if cloud sync has just been enabled in options dialog
					//mGoogleDriveSync.startSyncFrom(Synchronizer.SYNC_FLAG_SHOW_SIGN_IN | Synchronizer.SYNC_FLAG_QUIETLY | Synchronizer.SYNC_FLAG_SHOW_PROGRESS | (mGoogleDriveSyncOpts.AskConfirmations ? Synchronizer.SYNC_FLAG_ASK_CHANGED : 0) );
					syncServiceAccessor.bind(sync -> {
						sync.setSynchronizer(mGoogleDriveSync);
						sync.setOnSyncStatusListener(mGoogleDriveSyncStatusListener);
						sync.startSyncFrom(Synchronizer.SYNC_FLAG_SHOW_SIGN_IN | Synchronizer.SYNC_FLAG_QUIETLY | Synchronizer.SYNC_FLAG_SHOW_PROGRESS | (mGoogleDriveSyncOpts.AskConfirmations ? Synchronizer.SYNC_FLAG_ASK_CHANGED : 0) );
					});
					mSyncGoogleDriveEnabledPrev = mGoogleDriveSyncOpts.Enabled;
					return;
				}
				if (changedProps.size() > 0) {
					// After options dialog is closed, sync new settings to the cloud with delay
					BackgroundThread.instance().postGUI(() -> {
						if (mGoogleDriveSyncOpts.Enabled && mGoogleDriveSyncOpts.SyncSettings && null != mGoogleDriveSync) {
							if (mSuppressSettingsCopyToCloud) {
								// Immediately after downloading settings from Google Drive
								// prevent uploading settings file
								mSuppressSettingsCopyToCloud = false;
							} else {
								// After setting changed in OptionsDialog
								log.d("Some settings is changed, uploading to cloud...");
								//mGoogleDriveSync.startSyncToOnly(null, Synchronizer.SYNC_FLAG_SHOW_SIGN_IN | Synchronizer.SYNC_FLAG_QUIETLY | Synchronizer.SYNC_FLAG_SHOW_PROGRESS, Synchronizer.SyncTarget.SETTINGS);
								syncServiceAccessor.bind(sync -> {
									sync.setSynchronizer(mGoogleDriveSync);
									sync.setOnSyncStatusListener(mGoogleDriveSyncStatusListener);
									sync.startSyncToOnly(null, Synchronizer.SYNC_FLAG_SHOW_SIGN_IN | Synchronizer.SYNC_FLAG_QUIETLY | Synchronizer.SYNC_FLAG_SHOW_PROGRESS, Synchronizer.SyncTarget.SETTINGS);
								});
							}
						}
					}, 500);
				}
			}
			 */
			validateSettings();
		}
	}

	protected boolean allowLowBrightness() {
		// override to force higher brightness in non-reading mode (to avoid black screen on some devices when brightness level set to small value)
		return mCurrentFrame == mReaderFrame;
	}


	public ViewGroup getPreviousFrame() {
		return mPreviousFrame;
	}

	public boolean isPreviousFrameHome() {
		return mPreviousFrame != null && mPreviousFrame == mHomeFrame;
	}

	private void setCurrentFrame(ViewGroup newFrame) {
		if (mCurrentFrame != newFrame) {
			mPreviousFrame = mCurrentFrame;
			log.i("New current frame: " + newFrame.getClass().toString());
			mCurrentFrame = newFrame;
			setContentView(mCurrentFrame);
			mCurrentFrame.requestFocus();
			if (mCurrentFrame != mReaderFrame)
				releaseBacklightControl();
			if (mCurrentFrame == mHomeFrame) {
				// update recent books
				mHomeFrame.refreshRecentBooks();
				setLastLocationRoot();
				mCurrentFrame.invalidate();
			}
			if (mCurrentFrame == mBrowserFrame) {
				// update recent books directory
				mBrowser.refreshDirectory(mScanner.getRecentDir(), null);
			} else {
				if (null != mBrowser)
					mBrowser.stopCurrentScan();
			}
			onUserActivity();
		}
	}

	public void showReader() {
		runInReader(() -> {
			// do nothing
		});
	}

	private void stopTtsForDocumentChange() {
		if (mReaderView != null)
			mReaderView.stopTtsForDocumentChange();
	}

	private DocumentLoadLifecycle.Request replaceDocumentLoad() {
		stopTtsForDocumentChange();
		return documentLoadLifecycle.replace();
	}

	private void cancelPendingDocumentLoad() {
		stopTtsForDocumentChange();
		documentLoadLifecycle.cancelPending();
	}

	public void showRootWindow() {
		cancelPendingDocumentLoad();
		if (null != mBrowser)
			mBrowser.stopCurrentScan();
		setCurrentFrame(mHomeFrame);
		if (isInterfaceCreated) {
			/*
			  Commented until the appearance of free implementation of the binding to the Google Drive (R)
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
				// Save bookmarks and current reading position on the cloud
				if (mGoogleDriveSyncOpts.Enabled && null != mGoogleDriveSync) {
					//mGoogleDriveSync.startSyncToOnly(getCurrentBookInfo(), Synchronizer.SYNC_FLAG_QUIETLY, Synchronizer.SyncTarget.BOOKMARKS);
					syncServiceAccessor.bind(sync -> {
						sync.setSynchronizer(mGoogleDriveSync);
						sync.setOnSyncStatusListener(mGoogleDriveSyncStatusListener);
						sync.startSyncToOnly(getCurrentBookInfo(), Synchronizer.SYNC_FLAG_QUIETLY, Synchronizer.SyncTarget.BOOKMARKS);
					});
				}
			}
			 */
		}
	}

	private void runInReader(final Runnable task) {
		runInReader(null, task);
	}

	private void runInReader(
			final DocumentLoadLifecycle.Request loadOwner,
			final Runnable task) {
		if (null != mBrowser)
			mBrowser.stopCurrentScan();
		waitForCRDBService(() -> {
			if (loadOwner != null
					&& !documentLoadLifecycle.isActive(loadOwner))
				return;
			if (mReaderFrame != null) {
				task.run();
				if (loadOwner != null
						&& !documentLoadLifecycle.isActive(loadOwner))
					return;
				setCurrentFrame(mReaderFrame);
				if (mReaderView != null && mReaderView.getSurface() != null) {
					mReaderView.getSurface().setFocusable(true);
					mReaderView.getSurface().setFocusableInTouchMode(true);
					mReaderView.getSurface().requestFocus();
				} else {
					log.w("runInReader: mReaderView or mReaderView.getSurface() is null");
				}
			} else {
				mReaderView = new ReaderView(
						CoolReader.this,
						mEngine,
						mScanner,
						mHistory,
						mCoverpageManager,
						mGenresCollection,
						mDocumentCache,
						documentLoadLifecycle,
						mServiceLifecycle,
						settings());
				mReaderFrame = new ReaderViewLayout(CoolReader.this, mReaderView);
				mReaderFrame.getToolBar().setOnActionHandler(item -> {
					if (mReaderView != null)
						mReaderView.onAction(item);
					return true;
				});
				task.run();
				mReaderView.setBatteryStatus(initialBatteryStatus);
				mReaderView.doEngineCommand(
						ReaderCommand.DCMD_SET_ROTATION_INFO_FOR_AA,
						screenRotation);
				if (loadOwner != null
						&& !documentLoadLifecycle.isActive(loadOwner))
					return;
				setCurrentFrame(mReaderFrame);
				if (mReaderView.getSurface() != null) {
					mReaderView.getSurface().setFocusable(true);
					mReaderView.getSurface().setFocusableInTouchMode(true);
					mReaderView.getSurface().requestFocus();
				}
			}
		});
	}

	public boolean isBrowserCreated() {
		return mBrowserFrame != null;
	}

	private void runInBrowser(final Runnable task) {
		cancelPendingDocumentLoad();
		waitForCRDBService(() -> {
			if (mBrowserFrame == null) {
				mBrowser = new FileBrowser(
						CoolReader.this,
						mEngine,
						mScanner,
						mHistory,
						mCoverpageManager,
						mServiceLifecycle,
						mFileSystemFolders,
						settings().getBool(
								PROP_APP_FILE_BROWSER_HIDE_EMPTY_GENRES,
								false));
				mBrowser.setCoverPagesEnabled(settings().getBool(ReaderView.PROP_APP_SHOW_COVERPAGES, true));
				mBrowser.setCoverPageFontFace(settings().getProperty(ReaderView.PROP_FONT_FACE, DeviceInfo.DEF_FONT_FACE));
				mBrowser.setCoverPageSizeOption(settings().getInt(ReaderView.PROP_APP_COVERPAGE_SIZE, 1));
				mBrowser.setSortOrder(settings().getProperty(ReaderView.PROP_APP_BOOK_SORT_ORDER));
				mBrowser.setSimpleViewMode(settings().getBool(ReaderView.PROP_APP_FILE_BROWSER_SIMPLE_MODE, false));
				mBrowser.init();

				LayoutInflater inflater = LayoutInflater.from(CoolReader.this);// activity.getLayoutInflater();

				mBrowserTitleBar = inflater.inflate(R.layout.browser_status_bar, null);
				setBrowserTitle("Cool Reader browser window");

				mBrowserToolBar = new CRToolBar(CoolReader.this, ReaderAction.createList(
						ReaderAction.FILE_BROWSER_UP,
						ReaderAction.CURRENT_BOOK,
						ReaderAction.OPTIONS,
						ReaderAction.FILE_BROWSER_ROOT,
						ReaderAction.RECENT_BOOKS,
						ReaderAction.CURRENT_BOOK_DIRECTORY,
						ReaderAction.OPDS_CATALOGS,
						ReaderAction.SEARCH,
						ReaderAction.SCAN_DIRECTORY_RECURSIVE,
						ReaderAction.FILE_BROWSER_SORT_ORDER,
						ReaderAction.SAVE_LOGCAT,
						ReaderAction.EXIT
				), false);
				mBrowserToolBar.setBackgroundResource(R.drawable.ui_status_background_browser_dark);
				mBrowserToolBar.setOnActionHandler(item -> {
					switch (item.cmd) {
						case DCMD_EXIT:
							//
							finish();
							break;
						case DCMD_FILE_BROWSER_ROOT:
							showRootWindow();
							break;
						case DCMD_FILE_BROWSER_UP:
							mBrowser.showParentDirectory();
							break;
						case DCMD_OPDS_CATALOGS:
							mBrowser.showOPDSRootDirectory();
							break;
						case DCMD_RECENT_BOOKS_LIST:
							mBrowser.showRecentBooks();
							break;
						case DCMD_SEARCH:
							mBrowser.showFindBookDialog();
							break;
						case DCMD_CURRENT_BOOK:
							showCurrentBook();
							break;
						case DCMD_OPTIONS_DIALOG:
							showBrowserOptionsDialog();
							break;
						case DCMD_SCAN_DIRECTORY_RECURSIVE:
							mBrowser.scanCurrentDirectoryRecursive();
							break;
						case DCMD_FILE_BROWSER_SORT_ORDER:
							mBrowser.showSortOrderMenu();
							break;
						case DCMD_SAVE_LOGCAT:
							createLogcatFile();
							break;
						default:
							// do nothing
							break;
					}
					return false;
				});
				mBrowserFrame = new BrowserViewLayout(CoolReader.this, mBrowser, mBrowserToolBar, mBrowserTitleBar);

				//					if (getIntent() == null)
//						mBrowser.showDirectory(mScanner.getDownloadDirectory(), null);
			}
			task.run();
			setCurrentFrame(mBrowserFrame);
		});

	}

	public void showBrowser() {
		runInBrowser(() -> {
			// do nothing, browser is shown
		});
	}

	public void showManual() {
		DocumentLoadLifecycle.Request loadOwner =
				replaceDocumentLoad();
		if (loadOwner == null)
			return;
		runInReader(
				loadOwner,
				() -> mReaderView.showManual(loadOwner));
	}

	public static final String OPEN_FILE_PARAM = "FILE_TO_OPEN";

	public void loadDocument(final DocumentSource source,
							 final Runnable doneCallback,
							 final Runnable errorCallback,
							 final boolean forceSync) {
		if (source == null) {
			runOpenError(errorCallback);
			return;
		}
		final DocumentLoadLifecycle.Request loadOwner =
				replaceDocumentLoad();
		if (loadOwner == null)
			return;
		if (source.getKind() == DocumentSource.Kind.CONTENT_URI) {
			if (!source.isDurable()
					&& documentLoadLifecycle.isActive(loadOwner))
				showToast(R.string.temporary_uri_access_warning);
			loadDocumentFromUri(
					loadOwner, source, doneCallback, errorCallback);
			return;
		}
		runInReader(loadOwner, () -> mReaderView.loadDocument(
				loadOwner, source, forceSync ? () -> {
			if (null != doneCallback)
				doneCallback.run();
			/*
			  Commented until the appearance of free implementation of the binding to the Google Drive (R)
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
				// Save last opened document on cloud
				if (mGoogleDriveSyncOpts.Enabled && null != mGoogleDriveSync) {
					ArrayList<Synchronizer.SyncTarget> targets = new ArrayList<Synchronizer.SyncTarget>();
					if (mGoogleDriveSyncOpts.SyncCurrentBookInfo)
						targets.add(Synchronizer.SyncTarget.CURRENTBOOKINFO);
					if (mGoogleDriveSyncOpts.SyncCurrentBookBody)
						targets.add(Synchronizer.SyncTarget.CURRENTBOOKBODY);
					if (!targets.isEmpty()) {
						//mGoogleDriveSync.startSyncToOnly(getCurrentBookInfo(), Synchronizer.SYNC_FLAG_SHOW_SIGN_IN | Synchronizer.SYNC_FLAG_QUIETLY | Synchronizer.SYNC_FLAG_SHOW_PROGRESS, targets.toArray(new Synchronizer.SyncTarget[0]));
						syncServiceAccessor.bind(sync -> {
							sync.setSynchronizer(mGoogleDriveSync);
							sync.setOnSyncStatusListener(mGoogleDriveSyncStatusListener);
							sync.startSyncToOnly(getCurrentBookInfo(), Synchronizer.SYNC_FLAG_SHOW_SIGN_IN | Synchronizer.SYNC_FLAG_QUIETLY | Synchronizer.SYNC_FLAG_SHOW_PROGRESS, targets.toArray(new Synchronizer.SyncTarget[0]));
						});
					}
				}
			}
			 */
		} : doneCallback, errorCallback));
	}

	public void loadDocumentFromUri(Uri uri, Runnable doneCallback, Runnable errorCallback) {
		if (uri == null) {
			runOpenError(errorCallback);
			return;
		}
		loadDocument(
				DocumentSource.contentUri(
						uri.toString(), hasPersistedReadPermission(uri)),
				doneCallback, errorCallback, false);
	}

	private void loadDocumentFromUri(
									 DocumentLoadLifecycle.Request loadOwner,
									 DocumentSource source,
									 Runnable doneCallback,
									 Runnable errorCallback) {
		runInReader(loadOwner, () -> {
			if (!documentLoadLifecycle.isActive(loadOwner))
				return;
			ContentResolver contentResolver = getContentResolver();
			ParcelFileDescriptor pfd = null;
			try {
				Uri uri = Uri.parse(source.getLocator());
				SafDocumentMetadata metadata = readSafDocumentMetadata(contentResolver, uri);
				if (!documentLoadLifecycle.isActive(loadOwner))
					return;
				pfd = contentResolver.openFileDescriptor(uri, "r");
				if (pfd == null)
					throw new IOException("Content provider returned no file descriptor");
				long statSize = pfd.getStatSize();
				if (statSize >= 0)
					ParseBudget.requireDocumentBytes(statSize);
				if (isSeekable(pfd) && statSize >= 0) {
					DocumentFormat detectedFormat =
							resolveSafDocumentFormat(pfd, metadata);
					if (!documentLoadLifecycle.isActive(loadOwner))
						return;
					if (detectedFormat == null) {
						if (documentLoadLifecycle.isActive(loadOwner))
							showToast(R.string.unsupported_document_format);
						throw new IOException("Unsupported document format");
					}
					DocumentSource resolvedSource = source.withMetadata(
							metadata.displayName, metadata.mimeType, statSize,
							detectedFormat);
					mReaderView.loadDocumentFromFileDescriptor(
							loadOwner, pfd, resolvedSource,
							doneCallback, errorCallback);
					pfd = null; // ownership transferred to ReaderView
					return;
				}

				if (statSize > 0 && getCacheDir().getUsableSpace() < statSize + SAF_DISK_RESERVE_BYTES)
					throw new IOException("Not enough free space to cache SAF document");

				final ParcelFileDescriptor fallbackPfd = pfd;
				pfd = null; // AutoCloseInputStream owns it from here
				BackgroundThread.instance().postBackground(() -> cacheAndOpenSafDocument(
						loadOwner, source, metadata, fallbackPfd,
						doneCallback, errorCallback));
			} catch (Exception e) {
				Uri uri = Uri.parse(source.getLocator());
				log.e("Cannot open SAF document " + safeUriForLog(uri), e);
				runOpenError(loadOwner, errorCallback);
			} finally {
				if (pfd != null) {
					try {
						pfd.close();
					} catch (IOException e) {
						log.w("Cannot close failed SAF descriptor", e);
					}
				}
			}
		});
	}

	private static final long SAF_DISK_RESERVE_BYTES = 32L * 1024L * 1024L;

	private boolean isSeekable(ParcelFileDescriptor pfd) {
		try {
			Os.lseek(pfd.getFileDescriptor(), 0, OsConstants.SEEK_CUR);
			return true;
		} catch (ErrnoException e) {
			return false;
		}
	}

	private DocumentFormat resolveSafDocumentFormat(
			ParcelFileDescriptor pfd, SafDocumentMetadata metadata)
			throws IOException, ErrnoException {
		if (!DocumentFormatDetector.requiresContentInspection(metadata.mimeType))
			return DocumentFormatDetector.resolve(
					null, metadata.displayName, metadata.mimeType);

		Os.lseek(pfd.getFileDescriptor(), 0, OsConstants.SEEK_SET);
		ParcelFileDescriptor probeDescriptor =
				ParcelFileDescriptor.dup(pfd.getFileDescriptor());
		try (InputStream inputStream =
					 new ParcelFileDescriptor.AutoCloseInputStream(probeDescriptor)) {
			return DocumentFormatDetector.resolve(
					inputStream, metadata.displayName, metadata.mimeType);
		} finally {
			Os.lseek(pfd.getFileDescriptor(), 0, OsConstants.SEEK_SET);
		}
	}

	private void cacheAndOpenSafDocument(
										DocumentLoadLifecycle.Request loadOwner,
										DocumentSource source,
										SafDocumentMetadata metadata,
										ParcelFileDescriptor pfd,
										Runnable doneCallback, Runnable errorCallback) {
		Uri uri = Uri.parse(source.getLocator());
		FileInfo sourceInfo = new FileInfo();
		sourceInfo.pathname = source.getIdentity();
		sourceInfo.filename = metadata.displayName;
		sourceInfo.format = DocumentFormat.byMimeType(metadata.mimeType);
		if (sourceInfo.format == null)
			sourceInfo.format = DocumentFormat.byExtension(sourceInfo.filename);

		BookInfo cachedBook;
		ParcelFileDescriptor cachedPfd = null;
		DocumentSource resolvedSource = null;
		try (InputStream inputStream = new ParcelFileDescriptor.AutoCloseInputStream(pfd)) {
			if (!documentLoadLifecycle.isActive(loadOwner))
				return;
			cachedBook = mDocumentCache.saveStream(
					sourceInfo, inputStream, ParseBudget.MAX_DOCUMENT_INPUT_BYTES);
			if (cachedBook != null
					&& documentLoadLifecycle.isActive(loadOwner)) {
				File cachedFile = new File(cachedBook.getFileInfo().pathname);
				DocumentFormat detectedFormat;
				try (InputStream probe = new FileInputStream(cachedFile)) {
					detectedFormat = DocumentFormatDetector.resolve(
							probe, metadata.displayName, metadata.mimeType);
				}
				if (detectedFormat == null) {
					if (!cachedFile.delete())
						log.w("Cannot delete unsupported cached document");
					BackgroundThread.instance().postGUI(
							() -> {
								if (documentLoadLifecycle.isActive(loadOwner))
									showToast(R.string.unsupported_document_format);
							});
					throw new IOException("Unsupported document format");
				}
				if (!documentLoadLifecycle.isActive(loadOwner))
					return;
				cachedBook.getFileInfo().format = detectedFormat;
				resolvedSource = source.withMetadata(
						metadata.displayName, metadata.mimeType,
						cachedFile.length(), detectedFormat);
				cachedPfd = ParcelFileDescriptor.open(
						cachedFile, ParcelFileDescriptor.MODE_READ_ONLY);
			}
		} catch (Exception e) {
			log.e("Cannot cache non-seekable SAF document " + safeUriForLog(uri), e);
			cachedBook = null;
		}
		if (!documentLoadLifecycle.isActive(loadOwner)) {
			closeDescriptorQuietly(cachedPfd);
			return;
		}

		final ParcelFileDescriptor resultPfd = cachedPfd;
		final DocumentSource resultSource = resolvedSource;
		BackgroundThread.instance().postGUI(() -> {
			if (!documentLoadLifecycle.isActive(loadOwner)) {
				closeDescriptorQuietly(resultPfd);
				return;
			}
			if (resultPfd == null || resultSource == null) {
				runOpenError(loadOwner, errorCallback);
				return;
			}
			if (mReaderView == null) {
				closeDescriptorQuietly(resultPfd);
				runOpenError(loadOwner, errorCallback);
				return;
			}
			try {
				mReaderView.loadDocumentFromFileDescriptor(
						loadOwner, resultPfd, resultSource,
						doneCallback, errorCallback);
			} catch (RuntimeException e) {
				closeDescriptorQuietly(resultPfd);
				log.e("Cannot transfer cached SAF document "
						+ safeUriForLog(uri), e);
				runOpenError(loadOwner, errorCallback);
			}
		});
	}

	private SafDocumentMetadata readSafDocumentMetadata(ContentResolver resolver, Uri uri) {
		String displayName = null;
		String mimeType = resolver.getType(uri);
		try (Cursor cursor = resolver.query(
				uri, new String[] {OpenableColumns.DISPLAY_NAME}, null, null, null)) {
			if (cursor != null && cursor.moveToFirst()) {
				int nameColumn = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
				if (nameColumn >= 0)
					displayName = cursor.getString(nameColumn);
			}
		} catch (Exception e) {
			log.w("Cannot read SAF document metadata for " + safeUriForLog(uri), e);
		}
		if (displayName == null || displayName.length() == 0)
			displayName = uri.getLastPathSegment();
		if (displayName == null || displayName.length() == 0)
			displayName = "document";
		return new SafDocumentMetadata(displayName, mimeType);
	}

	private static final class SafDocumentMetadata {
		final String displayName;
		final String mimeType;

		SafDocumentMetadata(String displayName, String mimeType) {
			this.displayName = displayName;
			this.mimeType = mimeType;
		}
	}

	private boolean persistReadPermission(Uri uri, int intentFlags) {
		if (uri == null || !ContentResolver.SCHEME_CONTENT.equalsIgnoreCase(uri.getScheme()))
			return false;
		if (hasPersistedReadPermission(uri))
			return true;
		if ((intentFlags & Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION) == 0
				|| (intentFlags & Intent.FLAG_GRANT_READ_URI_PERMISSION) == 0)
			return false;
		try {
			getContentResolver().takePersistableUriPermission(
					uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
		} catch (SecurityException e) {
			log.w("Provider did not grant persistable read access for " + safeUriForLog(uri), e);
		}
		return hasPersistedReadPermission(uri);
	}

	private boolean hasPersistedReadPermission(Uri uri) {
		if (uri == null
				|| !ContentResolver.SCHEME_CONTENT.equalsIgnoreCase(uri.getScheme()))
			return false;
		for (UriPermission permission
				: getContentResolver().getPersistedUriPermissions()) {
			if (permission.isReadPermission() && uri.equals(permission.getUri()))
				return true;
		}
		return false;
	}

	private String safeUriForLog(Uri uri) {
		if (uri == null)
			return "<null>";
		Uri.Builder builder = uri.buildUpon().clearQuery().fragment(null);
		if (uri.getUserInfo() != null && uri.getHost() != null) {
			String authority = uri.getHost();
			if (uri.getPort() >= 0)
				authority += ":" + uri.getPort();
			builder.encodedAuthority(authority);
		}
		return builder.build().toString();
	}

	private String safePathForLog(String path) {
		if (path == null)
			return "<null>";
		Uri uri = Uri.parse(path);
		return uri.getScheme() != null ? safeUriForLog(uri) : path;
	}

	private void runOpenError(Runnable errorCallback) {
		if (errorCallback != null)
			BackgroundThread.instance().postGUI(errorCallback);
	}

	private void runOpenError(
			DocumentLoadLifecycle.Request loadOwner,
			Runnable errorCallback) {
		BackgroundThread.instance().postGUI(() -> {
			if (!documentLoadLifecycle.complete(loadOwner))
				return;
			if (errorCallback != null)
				errorCallback.run();
		});
	}

	private static void closeDescriptorQuietly(
			ParcelFileDescriptor descriptor) {
		if (descriptor == null)
			return;
		try {
			descriptor.close();
		} catch (IOException ignored) {
		}
	}

	public void loadDocument(FileInfo item, boolean forceSync) {
		loadDocument(item, null, null, forceSync);
	}

	public void loadDocument(FileInfo item, Runnable doneCallback, Runnable errorCallback, boolean forceSync) {
		if (item == null) {
			runOpenError(errorCallback);
			return;
		}
		log.d("Activities.loadDocument(" + item.pathname + ")");
		DocumentSource source = DocumentSource.fromFileInfo(item);
		if (source.getKind() == DocumentSource.Kind.CONTENT_URI) {
			source = DocumentSource.contentUri(
					source.getIdentity(),
					hasPersistedReadPermission(Uri.parse(source.getIdentity())))
					.withMetadata(
							source.getDisplayName(), source.getMimeType(),
							source.getSize(), source.getFormat());
		}
		loadDocument(source, doneCallback, errorCallback, forceSync);
	}

	/**
	 * When current book is opened, switch to previous book.
	 *
	 * @param errorCallback
	 */
	public void loadPreviousDocument(Runnable errorCallback) {
		BookInfo bi = mHistory.getPreviousBook();
		if (bi != null && bi.getFileInfo() != null) {
			log.i("loadPreviousDocument() is called, prevBookName = " + bi.getFileInfo().getPathName());
			loadDocument(bi.getFileInfo(), null, errorCallback, true);
			return;
		}
		errorCallback.run();
	}

	public void showOpenedBook() {
		showReader();
	}

	public static final String OPEN_DIR_PARAM = "DIR_TO_OPEN";

	public void showBrowser(final FileInfo dir) {
		runInBrowser(() -> mBrowser.showDirectory(dir, null));
	}

	public void showBrowser(final String dir) {
		runInBrowser(() -> mBrowser.showDirectory(
				mScanner.pathToFileInfo(dir), null));
	}

	public void showRecentBooks() {
		log.d("Activities.showRecentBooks() is called");
		runInBrowser(() -> mBrowser.showRecentBooks());
	}

	public void showOnlineCatalogs() {
		log.d("Activities.showOnlineCatalogs() is called");
		runInBrowser(() -> mBrowser.showOPDSRootDirectory());
	}

	public void showDirectory(FileInfo path) {
		log.d("Activities.showDirectory(" + path + ") is called");
		showBrowser(path);
	}

	public List<LibraryRootStore.Entry> getLibraryRoots() {
		return mLibraryRootStore.getRoots();
	}

	public void addLibraryRoot() {
		selectLibraryRoot(null);
	}

	public void reselectLibraryRoot(LibraryRootStore.Entry root) {
		selectLibraryRoot(root != null ? root.getUri() : null);
	}

	private void selectLibraryRoot(Uri previousUri) {
		mPendingLibraryRootUri = previousUri;
		Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
		intent.addFlags(
				Intent.FLAG_GRANT_READ_URI_PERMISSION
						| Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
						| Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
				&& previousUri != null) {
			intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, previousUri);
		}
		mSelectLibraryRootLauncher.launch(intent);
	}

	public void openLibraryRoot(LibraryRootStore.Entry root) {
		if (root == null)
			return;
		if (!root.isAccessGranted()) {
			reselectLibraryRoot(root);
			return;
		}
		Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
		intent.addCategory(Intent.CATEGORY_OPENABLE);
		intent.setType("*/*");
		intent.addFlags(
				Intent.FLAG_GRANT_READ_URI_PERMISSION
						| Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
			intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, root.getUri());
		mOpenLibraryDocumentLauncher.launch(intent);
	}

	public void showLibraryRootActions(LibraryRootStore.Entry root) {
		if (root == null)
			return;
		String[] actions = {
				getString(R.string.library_root_reselect),
				getString(R.string.library_root_remove)
		};
		new AlertDialog.Builder(this)
				.setTitle(root.getLabel())
				.setItems(actions, (dialog, which) -> {
					if (which == 0) {
						reselectLibraryRoot(root);
					} else {
						askConfirmation(
								getString(R.string.library_root_remove_confirm),
								() -> {
									mLibraryRootStore.remove(root.getUri());
									refreshLibraryRoots();
								});
					}
				})
				.show();
	}

	private void refreshLibraryRoots() {
		if (mHomeFrame != null)
			mHomeFrame.refreshFileSystemFolders();
	}

	public void showCatalog(final FileInfo path) {
		log.d("Activities.showCatalog(" + path + ") is called");
		runInBrowser(() -> mBrowser.showDirectory(path, null));
	}


	public void setBrowserTitle(String title) {
		if (mBrowserFrame != null)
			mBrowserFrame.setBrowserTitle(title);
	}

	public void setBrowserProgressStatus(boolean enable) {
		if (mBrowserFrame != null)
			mBrowserFrame.setBrowserProgressStatus(enable);
	}


	// Dictionary support


	public void findInDictionary(String s) {
		if (s != null && s.length() != 0) {
			int start, end;

			// Skip over non-letter characters at the beginning and end of the search string
			for (start = 0; start < s.length(); start++)
				if (Character.isLetterOrDigit(s.charAt(start)))
					break;
			for (end = s.length() - 1; end >= start; end--)
				if (Character.isLetterOrDigit(s.charAt(end)))
					break;

			if (end > start) {
				final String pattern = s.substring(start, end + 1);

				BackgroundThread.instance().postBackground(() -> BackgroundThread.instance()
						.postGUI(() -> findInDictionaryInternal(pattern), 100));
			}
		}
	}

	private void findInDictionaryInternal(String s) {
		log.d("lookup in dictionary: " + s);
		try {
			mDictionaries.findInDictionary(s);
		} catch (DictionaryException e) {
			showToast(e.getMessage());
		}
	}

	public void showDictionary() {
		findInDictionaryInternal(null);
	}

	private void handleSelectLibraryRootResult(
			int resultCode, Intent intent) {
		if (resultCode == Activity.RESULT_OK && intent != null) {
			Uri selectedUri = intent.getData();
			boolean persisted = persistReadPermission(
					selectedUri, intent.getFlags());
			if (persisted && mLibraryRootStore.addOrReplace(
					mPendingLibraryRootUri, selectedUri)) {
				showToast(R.string.library_root_selected);
			} else {
				showToast(R.string.library_root_grant_failed);
			}
		}
		mPendingLibraryRootUri = null;
		refreshLibraryRoots();
	}

	private void handleOpenLibraryDocumentResult(
			int resultCode, Intent intent) {
		if (resultCode == Activity.RESULT_OK
				&& intent != null && intent.getData() != null) {
			Uri uri = intent.getData();
			persistReadPermission(uri, intent.getFlags());
			loadDocumentFromUri(uri, null, this::showRootWindow);
		}
	}

	private void handleOpenDocumentTreeResult(
			int resultCode, Intent intent) {
		DocumentTreeRequestState.Request<FileInfo> request =
				openDocumentTreeRequests.take();
		if (request == null) {
			log.w("Ignoring document tree result without an owner");
			return;
		}
		if (resultCode != Activity.RESULT_OK || intent == null) {
			if (request.getCommand()
					== DocumentTreeRequestState.Command.DELETE_FOLDER) {
				refreshFolderDeletionParent(
						request.getArgument());
			}
			return;
		}
		switch (request.getCommand()) {
			case DELETE_FILE:
				handleDeleteFileTreeResult(
						intent.getData(),
						request.getArgument());
				break;
			case DELETE_FOLDER:
				handleDeleteFolderTreeResult(
						intent.getData(),
						request.getArgument(),
						request.getAttempt());
				break;
			case SAVE_LOGCAT:
				handleSaveLogcatTreeResult(
						intent.getData(),
						request.getArgument());
				break;
		}
	}

	private void refreshFolderDeletionParent(FileInfo target) {
		if (!mServiceLifecycle.isActive())
			return;
		DeletionSnapshot<FileInfo> deletion =
				captureDeletion(target);
		FileInfo parent =
				deletion != null
						? deletion.getParent()
						: null;
		if (parent != null && mServiceLifecycle.isActive())
			directoryUpdated(parent, null);
	}

	private boolean launchOpenDocumentTree(
			DocumentTreeRequestState.Command command,
			FileInfo argument) {
		return launchOpenDocumentTree(
				command,
				argument,
				0);
	}

	private boolean launchOpenDocumentTree(
			DocumentTreeRequestState.Command command,
			FileInfo argument,
			int attempt) {
		DocumentTreeRequestState.Request<FileInfo> request =
				openDocumentTreeRequests.begin(
						command,
						argument,
						attempt);
		if (request == null) {
			log.w("Document tree request is already pending");
			return false;
		}
		try {
			mOpenDocumentTreeLauncher.launch(
					new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE));
			return true;
		} catch (RuntimeException e) {
			openDocumentTreeRequests.cancel(request);
			log.e("Cannot launch document tree picker", e);
			return false;
		}
	}

	private void handleDeleteFileTreeResult(
			Uri sdCardUri,
			FileInfo target) {
		if (target == null
				|| target.isDirectory
				|| sdCardUri == null)
			return;
		Uri docUri = DocumentsContractWrapper.getDocumentUri(
				target, this, sdCardUri);
		if (docUri == null) {
			showToast(R.string.could_not_delete_on_sd);
			return;
		}
		if (!DocumentsContractWrapper.deleteFile(this, docUri)) {
			showToast(R.string.could_not_delete_file, target);
			return;
		}
		finishDeletedBook(
				captureDeletion(target));
		updateExtSDURI(target, sdCardUri);
	}

	private void handleDeleteFolderTreeResult(
			Uri sdCardUri,
			FileInfo target,
			int pickerAttempt) {
		if (target == null || !target.isDirectory)
			return;
		DeletionSnapshot<FileInfo> deletion =
				captureDeletion(target);
		Uri documentUri = sdCardUri != null
				? DocumentsContractWrapper.getDocumentUri(
						target, this, sdCardUri)
				: null;
		if (documentUri == null) {
			postFolderDeletionFailure(
					mServiceLifecycle,
					deletion,
					new ArrayList<>(),
					pickerAttempt);
			return;
		}
		if (DocumentsContractWrapper.fileExists(this, documentUri)) {
			updateExtSDURI(target, sdCardUri);
			deleteFolder(
					deletion,
					pickerAttempt);
		} else {
			postFolderDeletionFailure(
					mServiceLifecycle,
					deletion,
					new ArrayList<>(),
					pickerAttempt);
		}
	}

	private void handleSaveLogcatTreeResult(
			Uri uri,
			FileInfo target) {
		if (target == null || uri == null)
			return;
		Context appContext = getApplicationContext();
		ContentResolver resolver =
				appContext.getContentResolver();
		String fileName = target.filename;
		startLogcatExport(fileName, () -> {
			Uri docFolderUri =
					DocumentsContractWrapper
							.buildDocumentUriUsingTree(uri);
			if (docFolderUri == null)
				throw new IOException(
						"Cannot resolve selected document tree");
			Uri fileUri = DocumentsContractWrapper.createFile(
					appContext,
					docFolderUri,
					"text/x-log",
					fileName);
			if (fileUri == null)
				throw new IOException(
						"Cannot create logcat document");
			OutputStream output =
					resolver.openOutputStream(fileUri);
			if (output == null)
				throw new IOException(
						"Cannot open logcat document");
			return output;
		});
	}

	public void setDict(String id) {
		mDictionaries.setDict(id);
	}

	public void setDict2(String id) {
		mDictionaries.setDict2(id);
	}

	public void setToolbarAppearance(String id) {
		mOptionAppearance = id;
	}

	public String getToolbarAppearance() {
		return mOptionAppearance;
	}

	public void showAboutDialog() {
		AboutDialog dlg = new AboutDialog(this, mEngine);
		dlg.show();
	}


	public void initTTS(TTSControlServiceAccessor.Callback callback) {
		initTTS(callback, null);
	}

	public void initTTS(
			TTSControlServiceAccessor.Callback callback,
			Runnable failureCallback) {
		ServiceLifecycle lifecycle = mServiceLifecycle;
		if (mDestroyed
				|| lifecycle == null
				|| !lifecycle.isActive())
			return;
		showToast("Initializing TTS");
		if (null == ttsControlServiceAccessor)
			ttsControlServiceAccessor = new TTSControlServiceAccessor(this);
		final TTSControlServiceAccessor accessor =
				ttsControlServiceAccessor;
		final String requestedEngine = ttsEnginePackage;
		boolean bindingStarted = accessor.bind(ttsbinder -> {
			ttsbinder.initTTS(requestedEngine, new OnTTSCreatedListener() {
				@Override
				public void onCreated() {
					if (callback != null) {
						postForActiveTtsGeneration(
								() -> callback.run(accessor));
					}
				}

				@Override
				public void onFailed() {
					postTtsInitializationFailure(failureCallback);
				}

				@Override
				public void onTimedOut() {
					log.e("TTS engine \"" + requestedEngine
							+ "\" init failure");
					postForActiveTtsGeneration(() -> {
						if (requestedEngine.equals(
								ttsEnginePackage)) {
							showToast(
									R.string.tts_init_failure,
									requestedEngine);
							setSetting(
									PROP_APP_TTS_ENGINE,
									"",
									false);
							ttsEnginePackage = "";
							TTSToolbarDlg toolbar =
									mReaderView != null
											? mReaderView
													.getTTSToolbar()
											: null;
							if (toolbar != null)
								toolbar.stopAndClose();
						}
						if (failureCallback != null)
							failureCallback.run();
					});
				}
			});
		});
		if (!bindingStarted)
			postTtsInitializationFailure(failureCallback);
	}

	private void postTtsInitializationFailure(
			Runnable failureCallback) {
		postForActiveTtsGeneration(() -> {
			showToast("Cannot initialize TTS");
			if (failureCallback != null)
				failureCallback.run();
		});
	}

	private void postForActiveTtsGeneration(Runnable callback) {
		BackgroundThread.instance().executeGUI(() -> {
			ServiceLifecycle lifecycle = mServiceLifecycle;
			if (!mDestroyed
					&& lifecycle != null
					&& lifecycle.isActive())
				callback.run();
		});
	}

	public void showOptionsDialog(final OptionsDialog.Mode mode) {
		if (mode == OptionsDialog.Mode.READER) {
			if (mReaderView != null)
				mReaderView.showOptionsDialog();
			return;
		}
		BackgroundThread.instance().postBackground(() -> {
			final String[] mFontFaces = Engine.getFontFaceList();
			BackgroundThread.instance().executeGUI(() -> {
				OptionsDialog dlg = new OptionsDialog(
						CoolReader.this,
						mEngine,
						mode,
						mFontFaces,
						null);
				dlg.show();
			});
		});
	}

	public void updateCurrentPositionStatus(FileInfo book, Bookmark position, PositionProperties props) {
		mReaderFrame.getStatusBar().updateCurrentPositionStatus(book, position, props);
	}


	@Override
	protected void setDimmingAlpha(int dimmingAlpha) {
		if (mReaderView != null)
			mReaderView.setDimmingAlpha(dimmingAlpha);
	}

	public void showReaderMenu() {
		//
		if (mReaderFrame != null) {
			mReaderFrame.showMenu();
		}
	}


	public void sendBookFragment(BookInfo bookInfo, String text) {
		final Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
		emailIntent.setType("text/plain");
		emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, bookInfo.getFileInfo().getAuthors() + " " + bookInfo.getFileInfo().getTitle());
		emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, text);
		startActivity(Intent.createChooser(emailIntent, null));
	}

	public void showBookmarksDialog() {
		if (mReaderView != null)
			mReaderView.showBookmarksDialog();
	}

	public void openURL(String url) {
		try {
			Intent i = new Intent(Intent.ACTION_VIEW);
			i.setData(Uri.parse(url));
			startActivity(i);
		} catch (Exception e) {
			log.e("Exception while trying to open URL "
					+ safeUriForLog(Uri.parse(url)), e);
			showToast("Cannot open URL " + url);
		}
	}


	public boolean isBookOpened() {
		if (mReaderView == null)
			return false;
		return mReaderView.isBookLoaded();
	}

	public void closeBookIfOpened(FileInfo book) {
		if (mReaderView == null)
			return;
		mReaderView.closeIfOpened(book);
	}

	public void askDeleteBook(final FileInfo item) {
		DeletionSnapshot<FileInfo> requested =
				captureDeletion(item);
		if (requested == null)
			return;
		askConfirmation(R.string.win_title_confirm_book_delete, () -> {
			if (!mServiceLifecycle.isActive())
				return;
			FileInfo requestedTarget = requested.getTarget();
			closeBookIfOpened(requestedTarget);
			FileInfo file =
					mScanner.findFileInTree(requestedTarget);
			if (file == null)
				file = requestedTarget;
			DeletionSnapshot<FileInfo> deletion =
					captureDeletion(file);
			if (file.deleteFile()) {
				finishDeletedBook(deletion);
			} else {
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
					Uri docUri = null;
					Uri sdCardUri = getExtSDURIByFileInfo(file);
					if (sdCardUri != null)
						docUri = DocumentsContractWrapper.getDocumentUri(file, this, sdCardUri);
					if (null != docUri) {
						if (DocumentsContractWrapper.deleteFile(this, docUri)) {
							finishDeletedBook(deletion);
						} else {
							showToast(R.string.could_not_delete_file, file);
						}
					} else {
						showToast(R.string.choose_root_sd);
						launchOpenDocumentTree(
								DocumentTreeRequestState.Command
										.DELETE_FILE,
								deletion.getTarget());
					}
				} else {
					showToast(R.string.could_not_delete_file, file);
				}
			}
		});
	}

	public void askDeleteRecent(final FileInfo item) {
		DeletionSnapshot<FileInfo> requested =
				captureDeletion(item);
		if (requested == null)
			return;
		askConfirmation(
				R.string.win_title_confirm_history_record_delete,
				() -> removeRecentBook(requested));
	}

	private void finishDeletedBook(
			DeletionSnapshot<FileInfo> deletion) {
		if (deletion == null || !mServiceLifecycle.isActive())
			return;
		ServiceLifecycle lifecycle = mServiceLifecycle;
		FileInfo target = deletion.getTarget();
		FileInfo parent = deletion.getParent();
		waitForCRDBService(() -> {
			if (!lifecycle.isActive())
				return;
			CRDBService.LocalBinder db = getDB();
			if (db == null)
				return;
			mHistory.removeBookInfo(
					db,
					target,
					true,
					true);
			if (parent != null) {
				BackgroundThread.instance().postGUI(() -> {
					if (lifecycle.isActive())
						directoryUpdated(parent, null);
				}, 700);
			}
		});
	}

	private void removeRecentBook(
			DeletionSnapshot<FileInfo> deletion) {
		if (deletion == null || !mServiceLifecycle.isActive())
			return;
		ServiceLifecycle lifecycle = mServiceLifecycle;
		FileInfo target = deletion.getTarget();
		waitForCRDBService(() -> {
			if (!lifecycle.isActive())
				return;
			CRDBService.LocalBinder db = getDB();
			if (db == null)
				return;
			mHistory.removeBookInfo(
					db,
					target,
					true,
					false);
			if (lifecycle.isActive())
				directoryUpdated(
						mScanner.createRecentRoot());
		});
	}

	private static DeletionSnapshot<FileInfo> captureDeletion(
			FileInfo target) {
		return DeletionSnapshot.capture(
				target,
				target != null ? target.parent : null,
				CoolReader::copyDeletionFile);
	}

	private static FileInfo copyDeletionFile(FileInfo value) {
		FileInfo copy = new FileInfo(value);
		copy.parent =
				value.parent != null
						? new FileInfo(value.parent)
						: null;
		return copy;
	}

	public void askDeleteCatalog(final FileInfo item) {
		if (item == null || !item.isOPDSDir()
				|| item.id == null)
			return;
		Long catalogId = item.id;
		ServiceLifecycle lifecycle = mServiceLifecycle;
		askConfirmation(R.string.win_title_confirm_catalog_delete, () -> {
			if (!lifecycle.isActive())
				return;
			waitForCRDBService(() -> {
				if (!lifecycle.isActive())
					return;
				CRDBService.LocalBinder db = getDB();
				if (db == null)
					return;
				db.removeOPDSCatalog(catalogId);
				if (lifecycle.isActive())
					directoryUpdated(
							mScanner.createOPDSRoot());
			});
		});
	}

	public void askDeleteFolder(final FileInfo item) {
		DeletionSnapshot<FileInfo> requested =
				captureDeletion(item);
		FileInfo requestedTarget =
				requested != null
						? requested.getTarget()
						: null;
		if (!isDeletableFolder(requestedTarget))
			return;
		ServiceLifecycle lifecycle = mServiceLifecycle;
		askConfirmation(R.string.win_title_confirm_folder_delete, () -> {
			if (lifecycle.isActive())
				deleteFolder(requested, 0);
		});
	}

	private void deleteFolder(
			DeletionSnapshot<FileInfo> deletion,
			int pickerAttempt) {
		if (deletion == null
				|| pickerAttempt < 0
				|| !mServiceLifecycle.isActive())
			return;
		ServiceLifecycle lifecycle = mServiceLifecycle;
		if (pickerAttempt > MAX_FOLDER_DELETE_PICKER_ATTEMPTS) {
			postFolderDeletionFailure(
					lifecycle,
					deletion,
					new ArrayList<>(),
					pickerAttempt);
			return;
		}
		FileInfo target = deletion.getTarget();
		if (!isDeletableFolder(target))
			return;
		Scanner scanner = mScanner;
		Context appContext = getApplicationContext();
		Uri knownSdCardUri =
				Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
						? getExtSDURIByFileInfo(target)
						: null;
		BackgroundThread.instance().postBackground(() -> {
			if (!lifecycle.isActive())
				return;
			List<FileInfo> deletedBooks = new ArrayList<>();
			FileInfoOperationListener bookDeleteCallback =
					(fileInfo, errorStatus) -> {
						if (errorStatus == 0
								&& fileInfo != null
								&& fileInfo.format != null) {
							deletedBooks.add(
									copyDeletionFile(fileInfo));
						}
					};
			boolean deleted = Utils.deleteFolder(
					target,
					scanner,
					bookDeleteCallback,
					(fileInfo, errorStatus) -> {
					});
			if (deleted) {
				postFolderDeletionSuccess(
						lifecycle,
						deletion,
						deletedBooks);
				return;
			}
			if (!lifecycle.isActive())
				return;
			if (knownSdCardUri != null) {
				deleted = Utils.deleteFolderDocTree(
						target,
						scanner,
						appContext,
						knownSdCardUri,
						bookDeleteCallback,
						(fileInfo, errorStatus) -> {
						});
				if (deleted) {
					postFolderDeletionSuccess(
							lifecycle,
							deletion,
							deletedBooks);
					return;
				}
			}
			postFolderDeletionFailure(
					lifecycle,
					deletion,
					deletedBooks,
					pickerAttempt);
		});
	}

	private void postFolderDeletionSuccess(
			ServiceLifecycle lifecycle,
			DeletionSnapshot<FileInfo> deletion,
			List<FileInfo> deletedBooks) {
		List<FileInfo> books =
				copyDeletionFiles(deletedBooks);
		FileInfo parent = deletion.getParent();
		BackgroundThread.instance().executeGUI(() ->
				applyFolderDeletionEffects(
						lifecycle,
						books,
						parent));
	}

	private void postFolderDeletionFailure(
			ServiceLifecycle lifecycle,
			DeletionSnapshot<FileInfo> deletion,
			List<FileInfo> deletedBooks,
			int pickerAttempt) {
		List<FileInfo> books =
				copyDeletionFiles(deletedBooks);
		FileInfo target = deletion.getTarget();
		FileInfo parent = deletion.getParent();
		BackgroundThread.instance().executeGUI(() -> {
			if (!lifecycle.isActive())
				return;
			boolean retryAllowed =
					Build.VERSION.SDK_INT
							>= Build.VERSION_CODES.LOLLIPOP
					&& pickerAttempt
							< MAX_FOLDER_DELETE_PICKER_ATTEMPTS;
			applyFolderDeletionEffects(
					lifecycle,
					books,
					retryAllowed ? null : parent);
			if (!retryAllowed) {
				showToast(
						R.string.could_not_delete_file,
						target);
				return;
			}
			showToast(R.string.choose_root_sd);
			if (!launchOpenDocumentTree(
					DocumentTreeRequestState.Command
							.DELETE_FOLDER,
					target,
					pickerAttempt + 1)) {
				if (parent != null && lifecycle.isActive())
					directoryUpdated(parent, null);
				showToast(
						R.string.could_not_delete_file,
						target);
			}
		});
	}

	private void applyFolderDeletionEffects(
			ServiceLifecycle lifecycle,
			List<FileInfo> deletedBooks,
			FileInfo parent) {
		if (!lifecycle.isActive())
			return;
		if (!deletedBooks.isEmpty()) {
			waitForCRDBService(() -> {
				if (!lifecycle.isActive())
					return;
				CRDBService.LocalBinder db = getDB();
				if (db == null)
					return;
				for (FileInfo book : deletedBooks) {
					mHistory.removeBookInfo(
							db,
							book,
							true,
							true);
				}
			});
		}
		if (parent != null && lifecycle.isActive())
			directoryUpdated(parent, null);
	}

	private static List<FileInfo> copyDeletionFiles(
			List<FileInfo> files) {
		List<FileInfo> copies =
				new ArrayList<>(files.size());
		for (FileInfo file : files) {
			if (file != null)
				copies.add(copyDeletionFile(file));
		}
		return Collections.unmodifiableList(copies);
	}

	private static boolean isDeletableFolder(FileInfo item) {
		return item != null
				&& item.isDirectory
				&& !item.isOPDSDir()
				&& !item.isOnlineCatalogPluginDir();
	}

	private interface LogcatOutputFactory {
		OutputStream open() throws Exception;
	}

	public void createLogcatFile() {
		String fileName =
				new SimpleDateFormat(
						"'cr3-'yyyy-MM-dd_HH_mm_ss'.log'",
						Locale.US)
						.format(new Date());
		FileInfo dir = mScanner.getSharedDownloadDirectory();
		if (dir == null) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
				log.d("logcat: no access to download directory, opening document tree...");
				ServiceLifecycle lifecycle = mServiceLifecycle;
				askConfirmation(R.string.confirmation_select_folder_for_log, () -> {
					if (lifecycle.isActive()) {
						FileInfo target = new FileInfo();
						target.filename = fileName;
						launchOpenDocumentTree(
								DocumentTreeRequestState.Command
										.SAVE_LOGCAT,
								target);
					}
				});
			} else {
				log.e("Can't create logcat file: no access to download directory!");
			}
			return;
		}
		if (dir.pathname == null) {
			log.e("Can't create logcat file: download path is missing");
			return;
		}
		File outputFile =
				new File(dir.pathname, fileName);
		startLogcatExport(
				outputFile.getAbsolutePath(),
				() -> new FileOutputStream(outputFile));
	}

	private void startLogcatExport(
			String displayName,
			LogcatOutputFactory outputFactory) {
		ServiceLifecycle lifecycle = mServiceLifecycle;
		if (!lifecycle.isActive())
			return;
		if (displayName == null
				|| displayName.isEmpty()
				|| outputFactory == null) {
			log.e("Ignoring invalid logcat export target");
			return;
		}
		long sinceMillis =
				Math.max(
						0L,
						getLastLogcatTimestamp());
		long completedThroughMillis =
				Math.max(
						sinceMillis,
						System.currentTimeMillis());
		LogcatExportSession.Request request =
				logcatExportRequests.begin(
						displayName,
						sinceMillis,
						completedThroughMillis);
		if (request == null) {
			log.w("Ignoring overlapping logcat export");
			return;
		}
		BackgroundThread.instance().postBackground(() -> {
			if (!lifecycle.isActive()
					|| !logcatExportRequests.isActive(request))
				return;
			boolean saved = false;
			try (OutputStream output = outputFactory.open()) {
				if (output == null)
					throw new IOException(
							"Logcat output is unavailable");
				if (lifecycle.isActive()
						&& logcatExportRequests.isActive(request)) {
					saved = LogcatSaver.saveLogcat(
							new Date(request.getSinceMillis()),
							output);
				}
			} catch (Exception e) {
				saved = false;
				log.e(
						"Logcat export failed: "
								+ e.getClass().getSimpleName());
			}
			boolean result = saved;
			BackgroundThread.instance().executeGUI(() ->
					finishLogcatExport(
							lifecycle,
							request,
							result));
		});
	}

	private void finishLogcatExport(
			ServiceLifecycle lifecycle,
			LogcatExportSession.Request request,
			boolean saved) {
		if (!lifecycle.isActive()
				|| !logcatExportRequests.complete(request))
			return;
		String displayName = request.getDisplayName();
		if (saved) {
			setLastLogcatTimestamp(
					request.getCompletedThroughMillis());
			log.i("Logcat export completed");
			showMessage(
					getString(R.string.win_title_log),
					getString(
							R.string.notice_log_saved_to_,
							displayName));
		} else {
			log.e("Logcat export did not complete");
			showToast(
					"Failed to save logcat to "
							+ displayName);
		}
	}

	public void saveSetting(String name, String value) {
		if (mReaderView != null)
			mReaderView.saveSetting(name, value);
	}

	public void editBookInfo(final FileInfo currDirectory, final FileInfo item) {
		BookInfoDialogSession.Request owner =
				bookInfoDialogRequests.replace();
		if (owner == null)
			return;
		waitForCRDBService(() -> {
			if (!mServiceLifecycle.isActive()
					|| mDestroyed
					|| !bookInfoDialogRequests.isActive(owner))
				return;
			CRDBService.LocalBinder db = getDB();
			if (db == null) {
				bookInfoDialogRequests.complete(owner);
				return;
			}
			mHistory.getOrCreateBookInfo(db, item, bookInfo -> {
				if (!mServiceLifecycle.isActive()
						|| mDestroyed
						|| !bookInfoDialogRequests.complete(owner))
					return;
				BookInfo dialogBook =
						bookInfo != null
								? bookInfo
								: new BookInfo(item);
				BookInfoEditDialog dlg =
						new BookInfoEditDialog(
								CoolReader.this,
								mCoverpageManager,
								mGenresCollection,
								mHistory,
								currDirectory,
								dialogBook,
								currDirectory.isRecentDir());
				dlg.show();
			});
		});
	}

	public void editOPDSCatalog(FileInfo opds) {
		if (opds == null) {
			opds = new FileInfo();
			opds.isDirectory = true;
			opds.pathname = FileInfo.OPDS_DIR_PREFIX + "http://";
			opds.filename = "New Catalog";
			opds.isListed = true;
			opds.isScanned = true;
			opds.parent = mScanner.getOPDSRoot();
		}
		OPDSCatalogEditDialog dlg = new OPDSCatalogEditDialog(CoolReader.this, opds,
				() -> refreshOPDSRootDirectory(true));
		dlg.show();
	}

	public void refreshOPDSRootDirectory(boolean showInBrowser) {
		if (mBrowser != null)
			mBrowser.refreshOPDSRootDirectory(showInBrowser);
		if (mHomeFrame != null)
			mHomeFrame.refreshOnlineCatalogs();
	}


	private SharedPreferences mPreferences;
	private final static String BOOK_LOCATION_PREFIX = "@book:";
	private final static String DIRECTORY_LOCATION_PREFIX = "@dir:";

	private SharedPreferences getPrefs() {
		if (mPreferences == null)
			mPreferences = getSharedPreferences(PREF_FILE, 0);
		return mPreferences;
	}

	public void setLastBook(String path) {
		setLastLocation(BOOK_LOCATION_PREFIX + path);
	}

	public void setLastDirectory(String path) {
		setLastLocation(DIRECTORY_LOCATION_PREFIX + path);
	}

	public void setLastLocationRoot() {
		setLastLocation(FileInfo.ROOT_DIR_TAG);
	}

	/**
	 * Store last location - to resume after program restart.
	 *
	 * @param location is file name, directory, or special folder tag
	 */
	public void setLastLocation(String location) {
		try {
			String oldLocation = getPrefs().getString(PREF_LAST_LOCATION, null);
			if (oldLocation != null && oldLocation.equals(location))
				return; // not changed
			SharedPreferences.Editor editor = getPrefs().edit();
			editor.putString(PREF_LAST_LOCATION, location);
			editor.commit();
		} catch (Exception e) {
			// ignore
		}
	}

	private static final int NOTIFICATION_READER_MENU_MASK = 0x01;
	private static final int NOTIFICATION_LOGCAT_MASK = 0x02;
	private static final int NOTIFICATION_MASK_ALL = NOTIFICATION_READER_MENU_MASK |
			NOTIFICATION_LOGCAT_MASK;

	public void setLastNotificationMask(int notificationId) {
		try {
			SharedPreferences.Editor editor = getPrefs().edit();
			editor.putInt(PREF_LAST_NOTIFICATION_MASK, notificationId);
			editor.commit();
		} catch (Exception e) {
			// ignore
		}
	}

	public int getLastNotificationMask() {
		int res = getPrefs().getInt(PREF_LAST_NOTIFICATION_MASK, 0);
		log.i("getLastNotification() = " + res);
		return res;
	}


	public void showNotifications() {
		int lastNoticeMask = getLastNotificationMask();
		if ((lastNoticeMask & NOTIFICATION_MASK_ALL) == NOTIFICATION_MASK_ALL)
			return;
		if (DeviceInfo.getSDKLevel() >= DeviceInfo.HONEYCOMB) {
			if ((lastNoticeMask & NOTIFICATION_READER_MENU_MASK) == 0) {
				notification1();
				return;
			}
		}
		if ((lastNoticeMask & NOTIFICATION_LOGCAT_MASK) == 0) {
			notification2();
		}
	}

	public void notification1() {
		if (hasHardwareMenuKey())
			return; // don't show notice if hard key present
		showNotice(R.string.note1_reader_menu,
				R.string.dlg_button_yes, () -> {
					setSetting(PROP_TOOLBAR_LOCATION, String.valueOf(VIEWER_TOOLBAR_SHORT_SIDE), false);
					setLastNotificationMask(getLastNotificationMask() | NOTIFICATION_READER_MENU_MASK);
					showNotifications();
				},
				R.string.dlg_button_no, () -> {
					setSetting(PROP_TOOLBAR_LOCATION, String.valueOf(VIEWER_TOOLBAR_NONE), false);
					setLastNotificationMask(getLastNotificationMask() | NOTIFICATION_READER_MENU_MASK);
					showNotifications();
				}
		);
	}

	public void notification2() {
		showNotice(R.string.note2_logcat,
				() -> {
					setLastNotificationMask(getLastNotificationMask() | NOTIFICATION_LOGCAT_MASK);
					showNotifications();
				}
		);
	}

	/**
	 * Get last stored location.
	 *
	 * @return
	 */
	private String getLastLocation() {
		String res = getPrefs().getString(PREF_LAST_LOCATION, null);
		if (res == null) {
			// import last book value from previous releases
			res = getPrefs().getString(PREF_LAST_BOOK, null);
			if (res != null) {
				res = BOOK_LOCATION_PREFIX + res;
				try {
					getPrefs().edit().remove(PREF_LAST_BOOK).commit();
				} catch (Exception e) {
					// ignore
				}
			}
		}
		log.i("getLastLocation() = " + res);
		return res;
	}

	/**
	 * Open location - book, root view, folder...
	 */
	public void showLastLocation() {
		String location = getLastLocation();
		if (location == null)
			location = FileInfo.ROOT_DIR_TAG;
		if (location.startsWith(BOOK_LOCATION_PREFIX)) {
			location = location.substring(BOOK_LOCATION_PREFIX.length());
			DocumentSource source = DocumentSource.fromLegacyLocation(location);
			if (source.getKind() == DocumentSource.Kind.CONTENT_URI) {
				Uri uri = Uri.parse(source.getIdentity());
				source = DocumentSource.contentUri(
						source.getIdentity(), hasPersistedReadPermission(uri));
			}
			loadDocument(source, null, () -> BackgroundThread.instance().postGUI(() -> {
				// if document not loaded show error & then root window
				ErrorDialog errDialog = new ErrorDialog(CoolReader.this, "Error", "Can't open file!");
				errDialog.setOnDismissListener(dialog -> showRootWindow());
				errDialog.show();
			}, 1000), false);
			return;
		}
		if (location.startsWith(DIRECTORY_LOCATION_PREFIX)) {
			location = location.substring(DIRECTORY_LOCATION_PREFIX.length());
			showBrowser(location);
			return;
		}
		if (location.equals(FileInfo.RECENT_DIR_TAG)) {
			showBrowser(location);
			return;
		}
		// TODO: support other locations as well
		showRootWindow();
	}

	private boolean updateExtSDURI(FileInfo fi, Uri extSDUri) {
		String prefKey = null;
		String filePath = null;
		if (fi.isArchive && fi.arcname != null) {
			filePath = fi.arcname;
		} else
			filePath = fi.pathname;
		if (null != filePath) {
			File f = new File(filePath);
			filePath = f.getAbsolutePath();
			String[] parts = filePath.split("\\/");
			if (parts.length >= 3) {
				// For example,
				// parts[0] = ""
				// parts[1] = "storage"
				// parts[2] = "1501-3F19"
				// then prefKey = "/storage/1501-3F19"
				prefKey = "uri_for_/" + parts[1] + "/" + parts[2];
			}
		}
		if (null != prefKey) {
			SharedPreferences prefs = getPrefs();
			return prefs.edit().putString(prefKey, extSDUri.toString()).commit();
		}
		return false;
	}

	private Uri getExtSDURIByFileInfo(FileInfo fi) {
		Uri uri = null;
		String prefKey = null;
		String filePath = null;
		if (fi.isArchive && fi.arcname != null) {
			filePath = fi.arcname;
		} else
			filePath = fi.pathname;
		if (null != filePath) {
			File f = new File(filePath);
			filePath = f.getAbsolutePath();
			String[] parts = filePath.split("\\/");
			if (parts.length >= 3) {
				prefKey = "uri_for_/" + parts[1] + "/" + parts[2];
			}
		}
		if (null != prefKey) {
			SharedPreferences prefs = getPrefs();
			String strUri = prefs.getString(prefKey, null);
			if (null != strUri)
				uri = Uri.parse(strUri);
		}
		return uri;
	}

	private long getLastLogcatTimestamp() {
		return getPrefs().getLong(PREF_LAST_LOGCAT, 0);
	}

	private void setLastLogcatTimestamp(long timestamp) {
		SharedPreferences.Editor editor = getPrefs().edit();
		editor.putLong(PREF_LAST_LOGCAT, timestamp);
		editor.commit();
	}

	public void showCurrentBook() {
		BookInfo bi = mHistory.getLastBook();
		if (bi != null)
			loadDocument(bi.getFileInfo(), false);
	}

}
