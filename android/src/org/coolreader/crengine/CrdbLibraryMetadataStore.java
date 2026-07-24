/*
 * CoolReader for Android
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 2
 * of the License, or (at your option) any later version.
 */

package org.coolreader.crengine;

import org.coolreader.db.CRDBService;

import java.util.ArrayList;
import java.util.Collection;

final class CrdbLibraryMetadataStore implements LibraryMetadataStore {
	private final CRDBService.LocalBinder database;

	CrdbLibraryMetadataStore(CRDBService.LocalBinder database) {
		if (database == null)
			throw new IllegalArgumentException(
					"database must not be null");
		this.database = database;
	}

	@Override
	public void load(ArrayList<String> pathNames,
			Scanner.ScanControl control, LoadCallback callback) {
		database.loadFileInfos(
				pathNames, control, callback::onLoaded);
	}

	@Override
	public void save(Collection<FileInfo> files) {
		database.saveFileInfos(files);
	}

	@Override
	public void loadDirectoryFingerprint(
			String pathname, DirectoryFingerprintCallback callback) {
		database.loadDirectoryFingerprint(
				pathname, callback::onLoaded);
	}

	@Override
	public void saveDirectoryFingerprint(
			String pathname, String fingerprint) {
		database.saveDirectoryFingerprint(pathname, fingerprint);
	}
}
