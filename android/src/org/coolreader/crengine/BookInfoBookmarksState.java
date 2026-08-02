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
import java.util.Comparator;
import java.util.List;

/**
 * Owns the non-last-position bookmarks list of one {@link BookInfo}.
 *
 * Mutations and reads are synchronized so concurrent DB/UI paths cannot
 * leave a half-updated list. Callers still serialize with BookInfo for
 * last-position interactions.
 */
final class BookInfoBookmarksState {
	private final ArrayList<Bookmark> bookmarks = new ArrayList<>();

	synchronized int size() {
		return bookmarks.size();
	}

	synchronized Bookmark get(int index) {
		return bookmarks.get(index);
	}

	synchronized void add(Bookmark bm) {
		if (bm != null)
			bookmarks.add(bm);
	}

	synchronized void set(int index, Bookmark bm) {
		bookmarks.set(index, bm);
	}

	synchronized Bookmark remove(int index) {
		return bookmarks.remove(index);
	}

	synchronized int findIndex(Bookmark bm) {
		if (bm == null)
			return -1;
		for (int i = 0; i < bookmarks.size(); i++) {
			if (bookmarks.get(i).equalUniqueKey(bm))
				return i;
		}
		return -1;
	}

	synchronized void sort(Comparator<Bookmark> cmp) {
		Collections.sort(bookmarks, cmp);
	}

	synchronized List<Bookmark> snapshot() {
		return Collections.unmodifiableList(new ArrayList<>(bookmarks));
	}

	synchronized ArrayList<Bookmark> copyAsArrayList() {
		return new ArrayList<>(bookmarks);
	}

	synchronized void replaceAll(ArrayList<Bookmark> list) {
		bookmarks.clear();
		if (list != null)
			bookmarks.addAll(list);
	}

	synchronized void clear() {
		bookmarks.clear();
	}
}
