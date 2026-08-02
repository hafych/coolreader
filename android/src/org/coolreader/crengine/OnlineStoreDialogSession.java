/*
 * CoolReader for Android
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 2
 * of the License, or (at your option) any later version.
 */

package org.coolreader.crengine;

import java.util.ArrayList;
import java.util.List;

/**
 * Owns independent latest-only online-store requests and their cancellation.
 */
final class OnlineStoreDialogSession {
	enum Channel {
		BROWSER,
		BOOK_INFO,
		COVER,
		DOWNLOAD,
		AUTHENTICATION,
	}

	private final Request[] current =
			new Request[Channel.values().length];
	private boolean closed;

	Request replace(Channel channel) {
		Request canceled;
		Request replacement;
		synchronized (this) {
			if (closed || channel == null)
				return null;
			canceled = current[channel.ordinal()];
			replacement = new Request(channel);
			current[channel.ordinal()] = replacement;
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
		return !closed && request != null
				&& current[request.channel.ordinal()] == request;
	}

	synchronized boolean complete(Request request) {
		if (!isActive(request))
			return false;
		current[request.channel.ordinal()] = null;
		request.completed = true;
		request.cancelAction.clear();
		return true;
	}

	void cancel(Channel channel) {
		Request canceled;
		synchronized (this) {
			if (closed || channel == null)
				return;
			canceled = current[channel.ordinal()];
			current[channel.ordinal()] = null;
		}
		cancel(canceled);
	}

	boolean close() {
		List<Request> canceled = new ArrayList<>();
		synchronized (this) {
			if (closed)
				return false;
			closed = true;
			for (int i = 0; i < current.length; i++) {
				if (current[i] != null)
					canceled.add(current[i]);
				current[i] = null;
			}
		}
		for (Request request : canceled)
			cancel(request);
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
		private final Channel channel;
		private final CancelActionState cancelAction =
				new CancelActionState();
		private boolean completed;

		private Request(Channel channel) {
			this.channel = channel;
		}
	}
}
