/*
 * CoolReader for Android
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 2
 * of the License, or (at your option) any later version.
 */

package org.coolreader.crengine;

import java.util.Objects;

/**
 * Owns duplicate suppression for persisted reader positions.
 *
 * Book identity is intentionally compared by reference: two model snapshots
 * that describe the same file can still belong to different document
 * generations.
 */
final class ReaderPositionPersistenceState<T> {
	static final class Request<T> {
		private final T book;
		private final String position;

		private Request(T book, String position) {
			this.book = book;
			this.position = position;
		}
	}

	private T currentBook;
	private String persistedPosition;
	private boolean hasPersistedPosition;
	private Request<T> pending;
	private boolean closed;

	synchronized void replace(T book) {
		if (closed)
			return;
		currentBook = book;
		persistedPosition = null;
		hasPersistedPosition = false;
		pending = null;
	}

	synchronized Request<T> begin(T book, String position) {
		if (closed || book == null || currentBook != book)
			return null;
		if (hasPersistedPosition
				&& Objects.equals(
						persistedPosition, position))
			return null;
		if (pending != null
				&& pending.book == book
				&& Objects.equals(
						pending.position, position))
			return null;
		Request<T> request =
				new Request<>(book, position);
		pending = request;
		return request;
	}

	synchronized boolean complete(Request<T> request) {
		if (closed
				|| request == null
				|| pending != request
				|| currentBook != request.book)
			return false;
		persistedPosition = request.position;
		hasPersistedPosition = true;
		pending = null;
		return true;
	}

	synchronized boolean cancel(Request<T> request) {
		if (request == null || pending != request)
			return false;
		pending = null;
		return true;
	}

	synchronized void invalidate(T book) {
		if (closed || currentBook != book)
			return;
		persistedPosition = null;
		hasPersistedPosition = false;
		pending = null;
	}

	synchronized void close() {
		closed = true;
		currentBook = null;
		persistedPosition = null;
		hasPersistedPosition = false;
		pending = null;
	}
}
