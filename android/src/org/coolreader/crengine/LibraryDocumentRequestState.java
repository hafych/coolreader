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
 * Owns one exact document picker launched from a library root.
 */
public final class LibraryDocumentRequestState<T> {
	private Request<T> current;
	private boolean closed;

	public synchronized Request<T> begin(T initialRoot) {
		if (closed || current != null || initialRoot == null)
			return null;
		current = new Request<>(initialRoot);
		return current;
	}

	public synchronized Request<T> peek() {
		return current;
	}

	public synchronized Request<T> take() {
		Request<T> request = current;
		current = null;
		return request;
	}

	public synchronized boolean cancel(Request<T> request) {
		if (request == null || current != request)
			return false;
		current = null;
		return true;
	}

	public synchronized boolean isPending() {
		return current != null;
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

	public static final class Request<T> {
		private final T initialRoot;

		private Request(T initialRoot) {
			this.initialRoot = initialRoot;
		}

		public T getInitialRoot() {
			return initialRoot;
		}
	}
}
