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

/**
 * Owns the Activity screen-backlight wake duration (ms).
 *
 * Settings apply and wake-lock timer share one synchronized value. Destroy
 * permanently closes the owner.
 */
final class ScreenBacklightDurationState {
	private int durationMs;
	private boolean closed;

	ScreenBacklightDurationState(int initialDurationMs) {
		this.durationMs = initialDurationMs;
	}

	synchronized void setDurationMs(int durationMs) {
		if (closed)
			return;
		this.durationMs = durationMs;
	}

	synchronized int getDurationMs() {
		return durationMs;
	}

	synchronized boolean isEnabled() {
		return !closed && durationMs > 0;
	}

	synchronized boolean close() {
		if (closed)
			return false;
		closed = true;
		return true;
	}

	synchronized boolean isClosed() {
		return closed;
	}
}
