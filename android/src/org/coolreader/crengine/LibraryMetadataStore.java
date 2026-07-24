/*
 * CoolReader for Android
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 2
 * of the License, or (at your option) any later version.
 */

package org.coolreader.crengine;

import java.util.ArrayList;
import java.util.Collection;

interface LibraryMetadataStore {
	interface LoadCallback {
		void onLoaded(ArrayList<FileInfo> files);
	}

	void load(ArrayList<String> pathNames,
			Scanner.ScanControl control, LoadCallback callback);

	void save(Collection<FileInfo> files);
}
