/*
 * CoolReader for Android
 * Copyright (C) 2026 CoolReader Next contributors
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 2
 * of the License, or (at your option) any later version.
 */

package org.coolreader.crengine;

final class TapBounceState {
	private static final long NO_TAP_UPTIME = -1L;

	private long lastTapUptime = NO_TAP_UPTIME;
	private boolean closed;

	synchronized boolean shouldReject(
			long nowUptimeMillis,
			long bounceIntervalMillis) {
		if (closed
				|| lastTapUptime == NO_TAP_UPTIME
				|| nowUptimeMillis < lastTapUptime
				|| bounceIntervalMillis <= 0)
			return false;
		return nowUptimeMillis - lastTapUptime
				< bounceIntervalMillis;
	}

	synchronized boolean recordTap(long uptimeMillis) {
		if (closed || uptimeMillis < 0)
			return false;
		lastTapUptime = uptimeMillis;
		return true;
	}

	synchronized boolean close() {
		if (closed)
			return false;
		closed = true;
		lastTapUptime = NO_TAP_UPTIME;
		return true;
	}
}
