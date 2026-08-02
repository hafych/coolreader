/*
 * CoolReader for Android
 * Copyright (C) 2026 CoolReader Next contributors
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 2
 * of the License, or (at your option) any later version.
 */

package org.coolreader.db;

import org.coolreader.crengine.FileInfo;

import java.util.ArrayList;

/**
 * Owns the FileInfoCache MRU list and size counter.
 *
 * Put/get/remove share one synchronized structure so concurrent DB paths
 * cannot leave a mismatched size vs list length. Close permanently clears.
 */
final class FileInfoCacheState {
	private final int maxSize;
	private final ArrayList<FileInfo> list = new ArrayList<>();
	private int currentSize;
	private boolean closed;

	FileInfoCacheState(int maxSize) {
		this.maxSize = maxSize;
	}

	synchronized int size() {
		return closed ? 0 : currentSize;
	}

	synchronized int listSize() {
		return closed ? 0 : list.size();
	}

	synchronized FileInfo getAt(int index) {
		if (closed || index < 0 || index >= list.size())
			return null;
		return list.get(index);
	}

	synchronized void setAt(int index, FileInfo entry) {
		if (closed || index < 0 || index >= list.size())
			return;
		list.set(index, entry);
	}

	synchronized void add(FileInfo entry) {
		if (closed || entry == null)
			return;
		list.add(entry);
		currentSize++;
	}

	synchronized FileInfo removeAt(int index) {
		if (closed || index < 0 || index >= list.size())
			return null;
		FileInfo removed = list.remove(index);
		currentSize--;
		return removed;
	}

	synchronized void moveOnTop(int index) {
		if (closed || index < 0 || index >= list.size() - 1)
			return;
		FileInfo item = list.get(index);
		list.remove(index);
		list.add(item);
	}

	synchronized int findByPath(String path) {
		if (closed || path == null)
			return -1;
		for (int i = 0; i < list.size(); i++) {
			if (path.equals(list.get(i).getPathName()))
				return i;
		}
		return -1;
	}

	synchronized int findByBookKey(String bookKey) {
		if (closed || bookKey == null)
			return -1;
		for (int i = 0; i < list.size(); i++) {
			if (bookKey.equals(list.get(i).bookKey))
				return i;
		}
		return -1;
	}

	synchronized int findById(Long id) {
		if (closed || id == null)
			return -1;
		for (int i = 0; i < list.size(); i++) {
			if (id.equals(list.get(i).id))
				return i;
		}
		return -1;
	}

	/**
	 * Evicts from the front when over capacity (legacy threshold).
	 */
	synchronized void checkSize() {
		if (closed)
			return;
		int itemsToRemove = currentSize - maxSize;
		if (itemsToRemove < maxSize / 10)
			return;
		for (int i = itemsToRemove; i >= 0; i--) {
			if (!list.isEmpty())
				list.remove(0);
		}
		currentSize -= itemsToRemove;
		if (currentSize < 0)
			currentSize = 0;
	}

	synchronized void clear() {
		if (closed)
			return;
		list.clear();
		currentSize = 0;
	}

	synchronized boolean close() {
		if (closed)
			return false;
		closed = true;
		list.clear();
		currentSize = 0;
		return true;
	}

	synchronized boolean isClosed() {
		return closed;
	}
}
