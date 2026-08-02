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
 * Owns the Activity fullscreen flag.
 *
 * Settings apply and UI layout share one synchronized value. Destroy closes
 * the owner permanently.
 */
final class FullscreenState {
	private boolean fullscreen;
	private boolean closed;

	synchronized void set(boolean fullscreen) {
		if (closed)
			return;
		this.fullscreen = fullscreen;
	}

	synchronized boolean isFullscreen() {
		return !closed && fullscreen;
	}

	synchronized boolean close() {
		if (closed)
			return false;
		closed = true;
		fullscreen = false;
		return true;
	}

	synchronized boolean isClosed() {
		return closed;
	}
}
