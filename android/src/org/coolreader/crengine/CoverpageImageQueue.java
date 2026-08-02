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

/**
 * Ordered coverpage work queue keyed by {@link CoverpageManager.ImageItem}
 * identity matching.
 */
final class CoverpageImageQueue {
	private final ArrayList<CoverpageManager.ImageItem> list =
			new ArrayList<>();

	int indexOf(CoverpageManager.ImageItem file) {
		if (file == null)
			return -1;
		for (int i = list.size() - 1; i >= 0; i--) {
			if (file.matches(list.get(i)))
				return i;
		}
		return -1;
	}

	void remove(CoverpageManager.ImageItem file) {
		int index = indexOf(file);
		if (index >= 0)
			list.remove(index);
	}

	void moveOnTop(CoverpageManager.ImageItem file) {
		int index = indexOf(file);
		if (index == 0)
			return;
		if (index > 0)
			moveOnTop(index);
	}

	void moveOnTop(int index) {
		CoverpageManager.ImageItem item = list.get(index);
		list.remove(index);
		list.add(0, item);
	}

	boolean empty() {
		return list.isEmpty();
	}

	void add(CoverpageManager.ImageItem file) {
		if (file == null || indexOf(file) >= 0)
			return;
		list.add(file);
	}

	void clear() {
		list.clear();
	}

	boolean addOnTop(CoverpageManager.ImageItem file) {
		if (file == null)
			return false;
		int index = indexOf(file);
		if (index >= 0) {
			if (index > 0)
				moveOnTop(index);
			return false;
		}
		list.add(0, file);
		return true;
	}

	CoverpageManager.ImageItem next() {
		if (list.isEmpty())
			return null;
		return list.remove(0);
	}

	/**
	 * Drains the queue into a new list (ready-notification path).
	 */
	ArrayList<CoverpageManager.ImageItem> drain() {
		ArrayList<CoverpageManager.ImageItem> out =
				new ArrayList<>(list);
		list.clear();
		return out;
	}
}
