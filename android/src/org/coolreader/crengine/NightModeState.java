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
 * Owns the Activity night-mode flag.
 *
 * Settings apply and UI queries share one synchronized value. Destroy closes
 * the owner permanently and forces day mode for late readers.
 */
final class NightModeState {
	private boolean nightMode;
	private boolean closed;

	synchronized void set(boolean nightMode) {
		if (closed)
			return;
		this.nightMode = nightMode;
	}

	synchronized boolean isNightMode() {
		return !closed && nightMode;
	}

	synchronized boolean close() {
		if (closed)
			return false;
		closed = true;
		nightMode = false;
		return true;
	}

	synchronized boolean isClosed() {
		return closed;
	}
}
