/*
 * CoolReader for Android
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 2
 * of the License, or (at your option) any later version.
 */

package org.coolreader.crengine;

import org.coolreader.genrescollection.GenresCollection;

/**
 * Immutable snapshot of one application-service generation.
 */
public final class ServiceDependencies {
	private final Engine engine;
	private final Scanner scanner;
	private final History history;
	private final CoverpageManager coverpageManager;
	private final FileSystemFolders fileSystemFolders;
	private final GenresCollection genresCollection;
	private final DocumentFileCache documentCache;
	private final ServiceLifecycle lifecycle;

	ServiceDependencies(
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

	public Engine getEngine() {
		return engine;
	}

	public Scanner getScanner() {
		return scanner;
	}

	public History getHistory() {
		return history;
	}

	public CoverpageManager getCoverpageManager() {
		return coverpageManager;
	}

	public FileSystemFolders getFileSystemFolders() {
		return fileSystemFolders;
	}

	public GenresCollection getGenresCollection() {
		return genresCollection;
	}

	public DocumentFileCache getDocumentCache() {
		return documentCache;
	}

	public ServiceLifecycle getLifecycle() {
		return lifecycle;
	}
}
