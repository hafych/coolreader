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
 * Owns one replaceable font-filter scan and its physical cancellation.
 */
final class FontFilterSession {
	private Request current;
	private boolean closed;

	Request replace(Runnable cancelAction) {
		Request canceled;
		Request replacement;
		synchronized (this) {
			if (closed)
				return null;
			canceled = current;
			replacement = new Request(cancelAction);
			current = replacement;
		}
		cancel(canceled);
		return replacement;
	}

	synchronized boolean isActive(Request request) {
		return !closed && request != null && current == request;
	}

	synchronized boolean complete(Request request) {
		if (!isActive(request))
			return false;
		current = null;
		return true;
	}

	boolean cancel() {
		Request canceled;
		synchronized (this) {
			canceled = current;
			current = null;
		}
		cancel(canceled);
		return canceled != null;
	}

	boolean close() {
		Request canceled;
		synchronized (this) {
			if (closed)
				return false;
			closed = true;
			canceled = current;
			current = null;
		}
		cancel(canceled);
		return true;
	}

	synchronized boolean isClosed() {
		return closed;
	}

	private static void cancel(Request request) {
		if (request != null)
			request.cancelAction.run();
	}

	static final class Request {
		private final Runnable cancelAction;

		private Request(Runnable cancelAction) {
			if (cancelAction == null)
				throw new IllegalArgumentException(
						"cancelAction must not be null");
			this.cancelAction = cancelAction;
		}
	}
}
