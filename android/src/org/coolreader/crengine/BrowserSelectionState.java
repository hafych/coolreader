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
 * Owns the FileBrowser context-menu selection target for one browser
 * generation.
 *
 * Long-press selection and menu actions share one synchronized identity so a
 * late menu event cannot act on a closed browser. Close permanently clears
 * the slot.
 */
final class BrowserSelectionState {
	private FileInfo selected;
	private boolean closed;

	synchronized void set(FileInfo item) {
		if (closed)
			return;
		selected = item;
	}

	synchronized FileInfo get() {
		return closed ? null : selected;
	}

	synchronized boolean close() {
		if (closed)
			return false;
		closed = true;
		selected = null;
		return true;
	}

	synchronized boolean isClosed() {
		return closed;
	}
}
