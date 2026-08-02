/*
 * CoolReader for Android
 * Copyright (C) 2026 CoolReader Next contributors
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 2
 * of the License, or (at your option) any later version.
 */

package org.coolreader.plugins;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Owns a mutable plugin list (authors/books) for one response generation.
 *
 * Add/get/sort share one synchronized structure so concurrent UI reads
 * cannot observe a half-built list. Close permanently clears.
 */
final class OnlineStoreItemListState<T> {
	private final ArrayList<T> list = new ArrayList<>();
	private boolean closed;

	synchronized void add(T item) {
		if (closed || item == null)
			return;
		list.add(item);
	}

	synchronized int size() {
		return closed ? 0 : list.size();
	}

	synchronized T get(int index) {
		if (closed || index < 0 || index >= list.size())
			return null;
		return list.get(index);
	}

	synchronized void sort(Comparator<? super T> cmp) {
		if (!closed && cmp != null)
			Collections.sort(list, cmp);
	}

	synchronized List<T> snapshot() {
		if (closed)
			return Collections.emptyList();
		return Collections.unmodifiableList(new ArrayList<>(list));
	}

	synchronized boolean close() {
		if (closed)
			return false;
		closed = true;
		list.clear();
		return true;
	}

	synchronized boolean isClosed() {
		return closed;
	}
}
