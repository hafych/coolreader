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
 * Owns the reader touch-screen lock flag.
 *
 * Toggle is serialized so command handling and touch delivery always observe
 * one coherent enabled state. Destroy closes the owner permanently; a closed
 * owner reports disabled so late motion events are swallowed.
 */
final class TouchScreenLockState {
	private boolean enabled = true;
	private boolean closed;

	synchronized boolean toggle() {
		if (closed)
			return false;
		enabled = !enabled;
		return enabled;
	}

	synchronized boolean isEnabled() {
		return !closed && enabled;
	}

	synchronized boolean close() {
		if (closed)
			return false;
		closed = true;
		enabled = false;
		return true;
	}

	synchronized boolean isClosed() {
		return closed;
	}
}
