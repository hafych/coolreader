/*
 * CoolReader for Android
 * Copyright (C) 2026 CoolReader Next contributors
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 2
 * of the License, or (at your option) any later version.
 */

package org.coolreader.crengine;

import org.coolreader.genrescollection.GenresCollection;

/**
 * Owns one {@link Services} generation of engine/scanner/history graph.
 *
 * Start installs the full graph once; stop permanently closes and returns a
 * snapshot of the live components so teardown can run without parallel raw
 * fields on {@code Services}.
 */
final class ServicesGraphState {
	private Engine engine;
	private Scanner scanner;
	private History history;
	private CoverpageManager coverpageManager;
	private FileSystemFolders fileSystemFolders;
	private GenresCollection genresCollection;
	private DocumentFileCache documentCache;
	private ServiceLifecycle lifecycle;
	private boolean closed;

	/**
	 * Publishes a fully-built graph. Rejects a second install and any install
	 * after close.
	 */
	synchronized boolean install(
			Engine engine,
			Scanner scanner,
			History history,
			CoverpageManager coverpageManager,
			FileSystemFolders fileSystemFolders,
			GenresCollection genresCollection,
			DocumentFileCache documentCache,
			ServiceLifecycle lifecycle) {
		if (closed
				|| this.engine != null
				|| engine == null
				|| scanner == null
				|| history == null
				|| coverpageManager == null
				|| fileSystemFolders == null
				|| genresCollection == null
				|| documentCache == null
				|| lifecycle == null)
			return false;
		this.engine = engine;
		this.scanner = scanner;
		this.history = history;
		this.coverpageManager = coverpageManager;
		this.fileSystemFolders = fileSystemFolders;
		this.genresCollection = genresCollection;
		this.documentCache = documentCache;
		this.lifecycle = lifecycle;
		return true;
	}

	synchronized boolean isStarted() {
		return !closed && engine != null;
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

	synchronized FileSystemFolders fileSystemFolders() {
		return closed ? null : fileSystemFolders;
	}

	synchronized GenresCollection genresCollection() {
		return closed ? null : genresCollection;
	}

	synchronized DocumentFileCache documentCache() {
		return closed ? null : documentCache;
	}

	synchronized ServiceLifecycle lifecycle() {
		return closed ? null : lifecycle;
	}

	/**
	 * Permanently closes the graph and returns the previous components for
	 * ordered teardown. A second close returns null fields.
	 */
	synchronized Snapshot close() {
		if (closed)
			return Snapshot.empty();
		closed = true;
		Snapshot previous = new Snapshot(
				engine,
				scanner,
				history,
				coverpageManager,
				fileSystemFolders,
				genresCollection,
				documentCache,
				lifecycle);
		engine = null;
		scanner = null;
		history = null;
		coverpageManager = null;
		fileSystemFolders = null;
		genresCollection = null;
		documentCache = null;
		lifecycle = null;
		return previous;
	}

	synchronized boolean isClosed() {
		return closed;
	}

	static final class Snapshot {
		private final Engine engine;
		private final Scanner scanner;
		private final History history;
		private final CoverpageManager coverpageManager;
		private final FileSystemFolders fileSystemFolders;
		private final GenresCollection genresCollection;
		private final DocumentFileCache documentCache;
		private final ServiceLifecycle lifecycle;

		private Snapshot(
				Engine engine,
				Scanner scanner,
				History history,
				CoverpageManager coverpageManager,
				FileSystemFolders fileSystemFolders,
				GenresCollection genresCollection,
				DocumentFileCache documentCache,
				ServiceLifecycle lifecycle) {
			this.engine = engine;
			this.scanner = scanner;
			this.history = history;
			this.coverpageManager = coverpageManager;
			this.fileSystemFolders = fileSystemFolders;
			this.genresCollection = genresCollection;
			this.documentCache = documentCache;
			this.lifecycle = lifecycle;
		}

		static Snapshot empty() {
			return new Snapshot(
					null, null, null, null, null, null, null, null);
		}

		Engine engine() {
			return engine;
		}

		Scanner scanner() {
			return scanner;
		}

		History history() {
			return history;
		}

		CoverpageManager coverpageManager() {
			return coverpageManager;
		}

		FileSystemFolders fileSystemFolders() {
			return fileSystemFolders;
		}

		GenresCollection genresCollection() {
			return genresCollection;
		}

		DocumentFileCache documentCache() {
			return documentCache;
		}

		ServiceLifecycle lifecycle() {
			return lifecycle;
		}

		boolean hasCoverpageManager() {
			return coverpageManager != null;
		}
	}
}
