/*
 * CoolReader for Android
 * Copyright (C) 2026 CoolReader Next contributors
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 2
 * of the License, or (at your option) any later version.
 */

package org.coolreader;

import org.coolreader.crengine.CoverpageManager;
import org.coolreader.crengine.DocumentFileCache;
import org.coolreader.crengine.Engine;
import org.coolreader.crengine.FileSystemFolders;
import org.coolreader.crengine.History;
import org.coolreader.crengine.Scanner;
import org.coolreader.crengine.ServiceDependencies;
import org.coolreader.crengine.ServiceLifecycle;
import org.coolreader.genrescollection.GenresCollection;

/**
 * Owns one Activity generation of service dependencies.
 *
 * Dependencies are installed once from BaseActivity's service snapshot and
 * cleared on destroy so late work cannot observe a torn-down generation.
 */
final class ActivityServiceGraph {
	private Engine engine;
	private Scanner scanner;
	private History history;
	private CoverpageManager coverpageManager;
	private DocumentFileCache documentCache;
	private FileSystemFolders fileSystemFolders;
	private GenresCollection genresCollection;
	private ServiceLifecycle lifecycle;
	private boolean closed;

	synchronized boolean install(ServiceDependencies dependencies) {
		if (closed
				|| dependencies == null
				|| lifecycle != null)
			return false;
		engine = dependencies.getEngine();
		scanner = dependencies.getScanner();
		history = dependencies.getHistory();
		coverpageManager = dependencies.getCoverpageManager();
		documentCache = dependencies.getDocumentCache();
		fileSystemFolders = dependencies.getFileSystemFolders();
		genresCollection = dependencies.getGenresCollection();
		lifecycle = dependencies.getLifecycle();
		return true;
	}

	synchronized Engine engine() {
		return closed ? null : engine;
	}

	synchronized Scanner scanner() {
		return closed ? null : scanner;
	}

	synchronized History history() {
		return closed ? null : history;
	}

	synchronized CoverpageManager coverpageManager() {
		return closed ? null : coverpageManager;
	}

	synchronized DocumentFileCache documentCache() {
		return closed ? null : documentCache;
	}

	synchronized FileSystemFolders fileSystemFolders() {
		return closed ? null : fileSystemFolders;
	}

	synchronized GenresCollection genresCollection() {
		return closed ? null : genresCollection;
	}

	synchronized ServiceLifecycle lifecycle() {
		return closed ? null : lifecycle;
	}

	synchronized boolean isActive() {
		return !closed && lifecycle != null && lifecycle.isActive();
	}

	synchronized boolean close() {
		if (closed)
			return false;
		closed = true;
		engine = null;
		scanner = null;
		history = null;
		coverpageManager = null;
		documentCache = null;
		fileSystemFolders = null;
		genresCollection = null;
		lifecycle = null;
		return true;
	}

	synchronized boolean isClosed() {
		return closed;
	}
}
