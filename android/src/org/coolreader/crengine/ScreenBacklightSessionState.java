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

import android.os.PowerManager;

/**
 * Owns the Activity screen-backlight wake-lock session fields.
 *
 * WakeLock identity, delayed timer task and timestamps publish through one
 * synchronized owner so concurrent user-activity and timer paths cannot
 * observe a mixed generation. Destroy permanently closes the owner.
 */
final class ScreenBacklightSessionState {
	private PowerManager.WakeLock wakeLock;
	private Runnable timerTask;
	private long lastUserActivityTime;
	private long lastUpdateTimeStamp;
	private boolean closed;

	synchronized void setLastUserActivityTime(long value) {
		if (closed)
			return;
		lastUserActivityTime = value;
	}

	synchronized long getLastUserActivityTime() {
		return lastUserActivityTime;
	}

	synchronized void setLastUpdateTimeStamp(long value) {
		if (closed)
			return;
		lastUpdateTimeStamp = value;
	}

	synchronized long getLastUpdateTimeStamp() {
		return lastUpdateTimeStamp;
	}

	synchronized PowerManager.WakeLock getWakeLock() {
		return closed ? null : wakeLock;
	}

	synchronized void setWakeLock(PowerManager.WakeLock wakeLock) {
		if (closed)
			return;
		this.wakeLock = wakeLock;
	}

	synchronized Runnable getTimerTask() {
		return closed ? null : timerTask;
	}

	synchronized void setTimerTask(Runnable timerTask) {
		if (closed)
			return;
		this.timerTask = timerTask;
	}

	/**
	 * Clears timer and update stamp for release(). Returns the wake lock so
	 * the caller can release it. Does not permanently close the owner.
	 */
	synchronized PowerManager.WakeLock clearForRelease() {
		timerTask = null;
		lastUpdateTimeStamp = 0;
		return wakeLock;
	}

	synchronized boolean close() {
		if (closed)
			return false;
		closed = true;
		timerTask = null;
		lastUpdateTimeStamp = 0;
		lastUserActivityTime = 0;
		wakeLock = null;
		return true;
	}

	synchronized boolean isClosed() {
		return closed;
	}
}
