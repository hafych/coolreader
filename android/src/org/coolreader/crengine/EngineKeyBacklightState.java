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
 * Owns the Engine process-key-backlight level used by native bridge calls.
 *
 * Activity and engine paths share one synchronized value. Destroy closes the
 * owner permanently.
 */
final class EngineKeyBacklightState {
	private int level = 1;
	private boolean closed;

	synchronized void setLevel(int level) {
		if (closed)
			return;
		this.level = level;
	}

	synchronized int getLevel() {
		return level;
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
