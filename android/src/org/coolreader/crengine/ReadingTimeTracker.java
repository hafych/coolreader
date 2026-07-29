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
 * Reader-owned monotonic reading-session time accounting.
 */
final class ReadingTimeTracker {
	private static final long STOPPED = -1L;

	private long startedAt = STOPPED;
	private long accumulated;

	synchronized boolean start(long timestamp) {
		if (timestamp < 0)
			throw new IllegalArgumentException(
					"reading timestamp must be non-negative");
		if (startedAt != STOPPED)
			return false;
		startedAt = timestamp;
		return true;
	}

	synchronized boolean stop(long timestamp) {
		if (startedAt == STOPPED)
			return false;
		accumulated = addSaturated(
				accumulated,
				elapsedSinceStart(timestamp));
		startedAt = STOPPED;
		return true;
	}

	synchronized long elapsed(long timestamp) {
		if (startedAt == STOPPED)
			return accumulated;
		return addSaturated(
				accumulated,
				elapsedSinceStart(timestamp));
	}

	synchronized void setElapsed(long elapsed) {
		accumulated = Math.max(0L, elapsed);
	}

	synchronized boolean isRunning() {
		return startedAt != STOPPED;
	}

	private long elapsedSinceStart(long timestamp) {
		return timestamp <= startedAt ? 0L : timestamp - startedAt;
	}

	private static long addSaturated(long first, long second) {
		if (Long.MAX_VALUE - first < second)
			return Long.MAX_VALUE;
		return first + second;
	}
}
