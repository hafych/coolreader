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
 * Owns the latest asynchronous non-reader options-dialog request.
 */
public final class OptionsDialogRequestSession<T> {
	private Request<T> current;
	private boolean closed;

	public synchronized Request<T> replace(T mode) {
		if (closed || mode == null)
			return null;
		current = new Request<>(mode);
		return current;
	}

	public synchronized boolean isActive(Request<T> request) {
		return !closed
				&& request != null
				&& current == request;
	}

	public synchronized boolean complete(Request<T> request) {
		if (!isActive(request))
			return false;
		current = null;
		return true;
	}

	public synchronized void cancel() {
		current = null;
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
		private final T mode;

		private Request(T mode) {
			this.mode = mode;
		}

		public T getMode() {
			return mode;
		}
	}
}
