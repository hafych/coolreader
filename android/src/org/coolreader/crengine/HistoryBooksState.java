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
 * Owns the in-memory recent-books list and recent folder for one History
 * generation.
 *
 * Mutations are synchronized so concurrent DB callbacks and UI paths cannot
 * leave a half-updated list. Snapshot accessors return unmodifiable views of
 * the current list order without escaping the mutable ArrayList.
 */
final class HistoryBooksState {
	private final ArrayList<BookInfo> books = new ArrayList<>();
	private FileInfo recentBooksFolder;
	private boolean closed;

	synchronized BookInfo get(int index) {
		if (closed || index < 0 || index >= books.size())
			return null;
		return books.get(index);
	}

	synchronized int size() {
		return closed ? 0 : books.size();
	}

	synchronized boolean isEmpty() {
		return closed || books.isEmpty();
	}

	synchronized void addFirst(BookInfo bookInfo) {
		if (closed || bookInfo == null)
			return;
		books.add(0, bookInfo);
	}

	synchronized BookInfo removeAt(int index) {
		if (closed || index < 0 || index >= books.size())
			return null;
		return books.remove(index);
	}

	/**
	 * Moves an existing index to the front. Returns the book, or null when
	 * the index is invalid.
	 */
	synchronized BookInfo moveToFront(int index) {
		if (closed || index < 0 || index >= books.size())
			return null;
		BookInfo info = books.get(index);
		if (index > 0) {
			books.remove(index);
			books.add(0, info);
		}
		return info;
	}

	synchronized void clear() {
		if (!closed)
			books.clear();
	}

	/**
	 * Replaces the entire recent list (DB load path).
	 */
	synchronized void replaceAll(ArrayList<BookInfo> bookList) {
		if (closed)
			return;
		books.clear();
		if (bookList != null)
			books.addAll(bookList);
	}

	/**
	 * Returns a mutable ArrayList copy for legacy callbacks that require
	 * {@code ArrayList<BookInfo>}.
	 */
	synchronized ArrayList<BookInfo> copyAsArrayList() {
		return closed ? new ArrayList<>() : new ArrayList<>(books);
	}

	/**
	 * Returns an unmodifiable snapshot of current order.
	 */
	synchronized List<BookInfo> snapshot() {
		if (closed)
			return Collections.emptyList();
		return Collections.unmodifiableList(new ArrayList<>(books));
	}

	synchronized int findByPathname(String pathname) {
		if (closed || pathname == null)
			return -1;
		for (int i = 0; i < books.size(); i++) {
			if (pathname.equals(
					books.get(i).getFileInfo().getPathName()))
				return i;
		}
		return -1;
	}

	synchronized int findByFile(FileInfo file) {
		if (closed || file == null)
			return -1;
		for (int i = 0; i < books.size(); i++) {
			if (file.sameBook(books.get(i).getFileInfo()))
				return i;
		}
		return -1;
	}

	synchronized void setRecentFolder(FileInfo folder) {
		if (closed)
			return;
		recentBooksFolder = folder;
	}

	synchronized FileInfo getRecentFolder() {
		return closed ? null : recentBooksFolder;
	}

	synchronized boolean close() {
		if (closed)
			return false;
		closed = true;
		books.clear();
		recentBooksFolder = null;
		return true;
	}

	synchronized boolean isClosed() {
		return closed;
	}
}
