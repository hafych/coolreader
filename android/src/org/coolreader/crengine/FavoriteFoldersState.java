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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Owns the favorite-folders list for one {@link FileSystemFolders} generation.
 *
 * Lazy DB load, add/move/remove and UI reads share one synchronized owner so
 * concurrent binder callbacks cannot leave a half-updated list. Close clears
 * the list permanently.
 */
final class FavoriteFoldersState {
	private ArrayList<FileInfo> folders;
	private boolean closed;

	synchronized boolean isLoaded() {
		return !closed && folders != null;
	}

	/**
	 * Installs a loaded list once. Rejects null, second install and close.
	 */
	synchronized boolean install(ArrayList<FileInfo> loaded) {
		if (closed || folders != null || loaded == null)
			return false;
		folders = new ArrayList<>(loaded);
		return true;
	}

	synchronized int size() {
		return (!closed && folders != null) ? folders.size() : 0;
	}

	synchronized boolean isEmpty() {
		return size() == 0;
	}

	synchronized FileInfo get(int index) {
		if (closed || folders == null || index < 0 || index >= folders.size())
			return null;
		return folders.get(index);
	}

	synchronized void set(int index, FileInfo folder) {
		if (closed || folders == null || index < 0 || index >= folders.size())
			return;
		folders.set(index, folder);
	}

	synchronized void add(FileInfo folder) {
		if (closed || folders == null || folder == null)
			return;
		folders.add(folder);
	}

	synchronized FileInfo removeAt(int index) {
		if (closed || folders == null || index < 0 || index >= folders.size())
			return null;
		return folders.remove(index);
	}

	synchronized int find(FileInfo folder) {
		if (closed || folders == null || folder == null)
			return -1;
		for (int idx = 0; idx < folders.size(); idx++) {
			if (folders.get(idx).pathNameEquals(folder))
				return idx;
		}
		return -1;
	}

	/**
	 * Snapshot for UI composition. Empty when not yet loaded or closed.
	 */
	synchronized List<FileInfo> snapshot() {
		if (closed || folders == null)
			return Collections.emptyList();
		return Collections.unmodifiableList(new ArrayList<>(folders));
	}

	/**
	 * Mutable view for legacy filter/update that needs a List. Null when
	 * not loaded.
	 */
	synchronized ArrayList<FileInfo> orNull() {
		return closed ? null : folders;
	}

	/**
	 * ArrayList copy for callbacks that require {@code ArrayList}.
	 */
	synchronized ArrayList<FileInfo> copyAsArrayList() {
		if (closed || folders == null)
			return new ArrayList<>();
		return new ArrayList<>(folders);
	}

	synchronized boolean close() {
		if (closed)
			return false;
		closed = true;
		folders = null;
		return true;
	}

	synchronized boolean isClosed() {
		return closed;
	}
}
