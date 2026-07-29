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
 * Owns one exact logcat export and its immutable time boundary.
 */
public final class LogcatExportSession {
	private Request current;
	private boolean closed;

	public synchronized Request begin(
			String displayName,
			long sinceMillis,
			long completedThroughMillis) {
		if (closed
				|| current != null
				|| displayName == null
				|| displayName.isEmpty()
				|| sinceMillis < 0
				|| completedThroughMillis < sinceMillis)
			return null;
		current = new Request(
				displayName,
				sinceMillis,
				completedThroughMillis);
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
		private final String displayName;
		private final long sinceMillis;
		private final long completedThroughMillis;

		private Request(
				String displayName,
				long sinceMillis,
				long completedThroughMillis) {
			this.displayName = displayName;
			this.sinceMillis = sinceMillis;
			this.completedThroughMillis =
					completedThroughMillis;
		}

		public String getDisplayName() {
			return displayName;
		}

		public long getSinceMillis() {
			return sinceMillis;
		}

		public long getCompletedThroughMillis() {
			return completedThroughMillis;
		}
	}
}
