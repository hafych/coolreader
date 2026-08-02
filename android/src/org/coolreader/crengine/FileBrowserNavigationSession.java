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
 * Owns the latest FileBrowser navigation and its physical cancellation.
 */
final class FileBrowserNavigationSession {
	private Request current;
	private boolean closed;

	Request replace() {
		Request canceled;
		Request replacement;
		synchronized (this) {
			if (closed)
				return null;
			canceled = current;
			replacement = new Request();
			current = replacement;
		}
		cancel(canceled);
		return replacement;
	}

	boolean attachCancellation(
			Request request, Runnable cancelAction) {
		if (cancelAction == null)
			throw new IllegalArgumentException(
					"cancelAction must not be null");
		synchronized (this) {
			if (isActive(request)) {
				request.cancelAction.set(cancelAction);
				return true;
			}
			if (request != null && request.completed)
				return false;
		}
		cancelAction.run();
		return false;
	}

	synchronized boolean isActive(Request request) {
		return !closed && request != null && current == request;
	}

	synchronized boolean complete(Request request) {
		if (!isActive(request))
			return false;
		current = null;
		request.completed = true;
		request.cancelAction.clear();
		return true;
	}

	void cancel() {
		Request canceled;
		synchronized (this) {
			if (closed)
				return;
			canceled = current;
			current = null;
		}
		cancel(canceled);
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
		if (request == null)
			return;
		Runnable cancelAction = request.cancelAction.take();
		if (cancelAction != null)
			cancelAction.run();
	}

	static final class Request {
		private final CancelActionState cancelAction =
				new CancelActionState();
		private boolean completed;

		private Request() {
		}
	}
}
