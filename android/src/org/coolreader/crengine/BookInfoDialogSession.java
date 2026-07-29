/*
 * CoolReader for Android
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 2
 * of the License, or (at your option) any later version.
 */

package org.coolreader.crengine;

/**
 * Owns the exact pending work of a book-info dialog lifecycle.
 */
public final class BookInfoDialogSession {
	private Request current;
	private boolean closed;

	public synchronized Request replace() {
		if (closed)
			return null;
		current = new Request();
		return current;
	}

	public synchronized boolean isActive(Request request) {
		return !closed
				&& request != null
				&& current == request;
	}

	public synchronized boolean complete(Request request) {
		if (!isActive(request))
			return false;
		current = null;
		return true;
	}

	public synchronized boolean close() {
		if (closed)
			return false;
		closed = true;
		current = null;
		return true;
	}

	public synchronized boolean isClosed() {
		return closed;
	}

	public static final class Request {
		private Request() {
		}
	}
}
