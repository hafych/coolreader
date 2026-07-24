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

	private Engine engine;
	private Scanner scanner;
	private History history;
	private CoverpageManager coverpageManager;
	private FileSystemFolders fileSystemFolders;
	private GenresCollection genresCollection;
	private DocumentFileCache documentCache;
	private ServiceLifecycle lifecycle;

	public ServiceDependencies startServices(BaseActivity activity) {
		if (engine != null)
			throw new IllegalStateException(
					"Activity services are already started");
		log.i("First activity is created");
		lifecycle = new ServiceLifecycle(System.nanoTime());
		// testing background thread
		//mSettings = activity.settings();
		BackgroundThread.instance().setGUIHandler(new Handler());
		engine = new Engine(activity);
		scanner = new Scanner(activity, engine);
		scanner.initRoots(
				Engine.getMountedRootsMap(),
				engine.getAppPrivateDirs());
		history = new History(scanner);
		scanner.setDirScanEnabled(
				activity.settings().getBool(
						ReaderView.PROP_APP_BOOK_PROPERTY_SCAN_ENABLED,
						true));
		coverpageManager =
				new CoverpageManager(engine, lifecycle);
		fileSystemFolders = new FileSystemFolders(scanner);
		genresCollection =
				GenresCollection.getInstance(activity);
		documentCache = new DocumentFileCache(activity);
		return new ServiceDependencies(
				engine,
				scanner,
				history,
				coverpageManager,
				fileSystemFolders,
				genresCollection,
				documentCache,
				lifecycle);
	}

	public void stopServices(BaseActivity activity) {
		log.i("Last activity is destroyed");
		if (engine != null && !engine.isAttachedTo(activity)) {
			log.i("Ignoring stop from a stale activity generation");
			return;
		}
		if (coverpageManager == null) {
			log.i("Will not destroy services: finish only activity creation detected");
			return;
		}
		Engine stoppedEngine = engine;
		ServiceLifecycle stoppedLifecycle = lifecycle;
		engine = null;
		lifecycle = null;
		if (stoppedLifecycle != null)
			stoppedLifecycle.close();
		coverpageManager.clear();
		if (stoppedEngine != null)
			stoppedEngine.detachActivity(activity);
		BackgroundThread.instance().postBackground(() -> {
			log.i("Stopping activity service generation");
			if (stoppedEngine == null)
				return;
			stoppedEngine.uninit();
		});
		history = null;
		scanner = null;
		coverpageManager = null;
		fileSystemFolders = null;
		genresCollection = null;
		documentCache = null;
	}
}
