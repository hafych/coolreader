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
 * Owns independent latest-only refreshes for one CRRootView generation.
 */
final class RootViewRefreshSession {
	enum Channel {
		RECENT_BOOKS,
		ONLINE_CATALOGS,
		FILESYSTEM,
		LIBRARY,
	}

	private final Request[] current =
			new Request[Channel.values().length];
	private boolean closed;

	synchronized Request replace(Channel channel) {
		if (closed || channel == null)
			return null;
		Request request = new Request(channel);
		current[channel.ordinal()] = request;
		return request;
	}

	synchronized boolean isActive(Request request) {
		return !closed && request != null
				&& current[request.channel.ordinal()] == request;
	}

	synchronized boolean complete(Request request) {
		if (!isActive(request))
			return false;
		current[request.channel.ordinal()] = null;
		return true;
	}

	synchronized void replaceView() {
		if (closed)
			return;
		for (int i = 0; i < current.length; i++)
			current[i] = null;
	}

	synchronized boolean close() {
		if (closed)
			return false;
		closed = true;
		for (int i = 0; i < current.length; i++)
			current[i] = null;
		return true;
	}

	synchronized boolean isClosed() {
		return closed;
	}

	static final class Request {
		private final Channel channel;

		private Request(Channel channel) {
			this.channel = channel;
		}
	}
}
