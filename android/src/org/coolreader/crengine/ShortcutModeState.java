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
 * Owns a dialog/list shortcut-mode flag.
 *
 * UI mode switches publish through one synchronized value. Close freezes
 * the last mode for pure late reads.
 */
final class ShortcutModeState {
	private boolean shortcutMode;
	private boolean closed;

	synchronized void set(boolean shortcutMode) {
		if (closed)
			return;
		this.shortcutMode = shortcutMode;
	}

	synchronized boolean isShortcutMode() {
		return !closed && shortcutMode;
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
