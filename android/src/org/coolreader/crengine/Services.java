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

	private final ServicesGraphState graph = new ServicesGraphState();

	public ServiceDependencies startServices(BaseActivity activity) {
		if (graph.isStarted())
			throw new IllegalStateException(
					"Activity services are already started");
		log.i("First activity is created");
		ServiceLifecycle lifecycle = new ServiceLifecycle(System.nanoTime());
		// testing background thread
		//mSettings = activity.settings();
		BackgroundThread.instance().setGUIHandler(new Handler());
		Engine engine = new Engine(activity);
		Scanner scanner = new Scanner(activity, engine);
		scanner.initRoots(
				Engine.getMountedRootsMap(),
				engine.getAppPrivateDirs());
		History history = new History(scanner);
		scanner.setDirScanEnabled(
				activity.settings().getBool(
						ReaderView.PROP_APP_BOOK_PROPERTY_SCAN_ENABLED,
						true));
		CoverpageManager coverpageManager =
				new CoverpageManager(engine, lifecycle);
		FileSystemFolders fileSystemFolders = new FileSystemFolders(scanner);
		GenresCollection genresCollection =
				GenresCollection.getInstance(activity);
		DocumentFileCache documentCache = new DocumentFileCache(activity);
		if (!graph.install(
				engine,
				scanner,
				history,
				coverpageManager,
				fileSystemFolders,
				genresCollection,
				documentCache,
				lifecycle))
			throw new IllegalStateException(
					"Service graph install rejected");
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
		Engine liveEngine = graph.engine();
		if (liveEngine != null && !liveEngine.isAttachedTo(activity)) {
			log.i("Ignoring stop from a stale activity generation");
			return;
		}
		if (graph.coverpageManager() == null) {
			log.i("Will not destroy services: finish only activity creation detected");
			return;
		}
		ServicesGraphState.Snapshot stopped = graph.close();
		Engine stoppedEngine = stopped.engine();
		ServiceLifecycle stoppedLifecycle = stopped.lifecycle();
		if (stoppedLifecycle != null)
			stoppedLifecycle.close();
		if (stopped.coverpageManager() != null)
			stopped.coverpageManager().clear();
		if (stoppedEngine != null)
			stoppedEngine.detachActivity(activity);
		BackgroundThread.instance().postBackground(() -> {
			log.i("Stopping activity service generation");
			if (stoppedEngine == null)
				return;
			stoppedEngine.uninit();
		});
	}
}
