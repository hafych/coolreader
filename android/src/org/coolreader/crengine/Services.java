/*
 * CoolReader for Android
 * Copyright (C) 2012 Vadim Lopatin <coolreader.org@gmail.com>
 * Copyright (C) 2013 Alexey Kabelitskiy <akabelytskyi@hmstn.com>
 * Copyright (C) 2018,2020,2021 Aleksey Chernov <valexlin@gmail.com>
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

import android.os.Handler;

import org.coolreader.genrescollection.GenresCollection;

public class Services {

	public static final Logger log = L.create("sv");

	private static volatile Engine mEngine;
	private static Scanner mScanner;
	private static History mHistory;
	private static CoverpageManager mCoverpageManager;
	private static FileSystemFolders mFSFolders;
	private static GenresCollection mGenresCollection;
	private static DocumentFileCache mDocumentCache;
	private static volatile long mGeneration;
	private static ServiceLifecycle mLifecycle;

	public static Engine getEngine() {
		if (null != mEngine)
			return mEngine;
		throw new RuntimeException("Services.getEngine(): trying to get null object");
	}

	public static Scanner getScanner() {
		if (null != mScanner)
			return mScanner;
		throw new RuntimeException("Services.getScanner(): trying to get null object");
	}

	public static History getHistory() {
		if (null != mHistory)
			return mHistory;
		throw new RuntimeException("Services.getHistory(): trying to get null object");
	}

	public static CoverpageManager getCoverpageManager() {
		if (null != mCoverpageManager)
			return mCoverpageManager;
		throw new RuntimeException("Services.getCoverpageManager(): trying to get null object");
	}

	public static FileSystemFolders getFileSystemFolders() {
		if (null != mFSFolders)
			return mFSFolders;
		throw new RuntimeException("Services.getFileSystemFolders(): trying to get null object");
	}

	public static GenresCollection getGenresCollection() {
		if (null != mGenresCollection)
			return mGenresCollection;
		throw new RuntimeException("Services.getGenresCollection(): trying to get null object");
	}

	public static DocumentFileCache getDocumentCache() {
		if (null != mDocumentCache)
			return mDocumentCache;
		throw new RuntimeException("Services.getDocumentCache(): trying to get null object");
	}

	public static ServiceLifecycle getLifecycle() {
		if (mLifecycle != null)
			return mLifecycle;
		throw new RuntimeException(
				"Services.getLifecycle(): trying to get null object");
	}

	public static boolean isStopped() {
		return null == mEngine || null == mScanner || null == mHistory || null == mCoverpageManager || null == mFSFolders || null == mGenresCollection || null == mDocumentCache;
	}

	public static ServiceDependencies startServices(BaseActivity activity) {
		log.i("First activity is created");
		mGeneration++;
		mLifecycle = new ServiceLifecycle(mGeneration);
		// testing background thread
		//mSettings = activity.settings();
		BackgroundThread.instance().setGUIHandler(new Handler());
		mEngine = new Engine(activity);
		mScanner = new Scanner(activity, mEngine);
		mScanner.initRoots(Engine.getMountedRootsMap(), mEngine.getAppPrivateDirs());
		mHistory = new History(mScanner);
		mScanner.setDirScanEnabled(activity.settings().getBool(ReaderView.PROP_APP_BOOK_PROPERTY_SCAN_ENABLED, true));
		mCoverpageManager = new CoverpageManager(mEngine, mLifecycle);
		mFSFolders = new FileSystemFolders(mScanner);
		mGenresCollection = GenresCollection.getInstance(activity);
		mDocumentCache = new DocumentFileCache(activity);
		return new ServiceDependencies(
				mEngine,
				mScanner,
				mHistory,
				mCoverpageManager,
				mFSFolders,
				mGenresCollection,
				mDocumentCache,
				mLifecycle);
	}

	public static void stopServices(BaseActivity activity) {
		log.i("Last activity is destroyed");
		if (mEngine != null && !mEngine.isAttachedTo(activity)) {
			log.i("Ignoring stop from a stale activity generation");
			return;
		}
		if (mCoverpageManager == null) {
			log.i("Will not destroy services: finish only activity creation detected");
			return;
		}
		Engine engine = mEngine;
		ServiceLifecycle lifecycle = mLifecycle;
		long stoppedGeneration = mGeneration;
		mEngine = null;
		mLifecycle = null;
		if (lifecycle != null)
			lifecycle.close();
		mCoverpageManager.clear();
		if (engine != null)
			engine.detachActivity(activity);
		BackgroundThread.instance().postBackground(() -> {
			log.i("Stopping background thread");
			if (engine == null)
				return;
			engine.uninit();
			if (mGeneration == stoppedGeneration && mEngine == null)
				BackgroundThread.instance().quit();
		});
		mHistory = null;
		mScanner = null;
		mCoverpageManager = null;
		mFSFolders = null;
		mGenresCollection = null;
		mDocumentCache = null;
	}
}
