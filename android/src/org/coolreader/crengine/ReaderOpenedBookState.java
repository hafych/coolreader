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

/**
 * Owns the current reader book identity together with the opened flag.
 *
 * Load may bind a pending book before native open succeeds. Successful
 * publication marks the book opened. Close and failure clear only the opened
 * flag or the exact pending/published identity, so replacement books cannot be
 * cleared by a stale load path. Destroy permanently closes the owner.
 */
final class ReaderOpenedBookState {
	private static final Snapshot EMPTY = new Snapshot(null, false);

	private volatile Snapshot snapshot = EMPTY;
	private boolean closed;

	/**
	 * Binds the current book identity without forcing the opened flag.
	 * Used when a load starts and when stream reconciliation replaces the
	 * published book for the same open session.
	 */
	synchronized Snapshot bind(BookInfo bookInfo) {
		if (closed)
			return snapshot;
		Snapshot next = new Snapshot(bookInfo, snapshot.opened);
		snapshot = next;
		return next;
	}

	/**
	 * Publishes a fully opened book. Both identity and opened flag change in
	 * one publication so readers never observe opened=true with another book.
	 */
	synchronized Snapshot publishOpened(BookInfo bookInfo) {
		if (closed || bookInfo == null)
			return snapshot;
		Snapshot next = new Snapshot(bookInfo, true);
		snapshot = next;
		return next;
	}

	/**
	 * Marks the reader closed while retaining the last book identity, matching
	 * the historical close path that only cleared the opened flag.
	 */
	synchronized boolean markClosed() {
		if (closed)
			return false;
		boolean wasOpened = snapshot.opened;
		if (!wasOpened && snapshot.book == null)
			return false;
		snapshot = new Snapshot(snapshot.book, false);
		return wasOpened;
	}

	/**
	 * Clears the book only when it is still the exact identity. Used by load
	 * failure recovery so a replacement book is not wiped by a stale task.
	 */
	synchronized boolean clearIf(BookInfo bookInfo) {
		if (closed || bookInfo == null || snapshot.book != bookInfo)
			return false;
		snapshot = EMPTY;
		return true;
	}

	BookInfo book() {
		return snapshot.book;
	}

	boolean isOpened() {
		return snapshot.opened;
	}

	Snapshot snapshot() {
		return snapshot;
	}

	synchronized Snapshot close() {
		if (closed)
			return snapshot;
		closed = true;
		Snapshot previous = snapshot;
		snapshot = EMPTY;
		return previous;
	}

	synchronized boolean isClosed() {
		return closed;
	}

	static final class Snapshot {
		private final BookInfo book;
		private final boolean opened;

		private Snapshot(BookInfo book, boolean opened) {
			this.book = book;
			this.opened = opened;
		}

		BookInfo book() {
			return book;
		}

		boolean isOpened() {
			return opened;
		}

		boolean hasBook() {
			return book != null;
		}
	}
}
