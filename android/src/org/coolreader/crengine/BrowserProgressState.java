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
 * Owns the FileBrowser progress popup for one browser generation.
 *
 * Creation, show/hide and teardown share one synchronized owner so a late
 * navigation callback cannot revive a closed browser progress indicator.
 * Close permanently drops the popup and returns it for hide.
 */
final class BrowserProgressState {
	private ProgressPopup popup;
	private boolean closed;

	synchronized void set(ProgressPopup next) {
		if (closed)
			return;
		popup = next;
	}

	synchronized ProgressPopup get() {
		return closed ? null : popup;
	}

	synchronized void show() {
		if (closed || popup == null)
			return;
		popup.show();
	}

	synchronized void hide() {
		if (popup == null)
			return;
		popup.hide();
	}

	/**
	 * Permanently closes the owner and returns any live popup for hide.
	 */
	synchronized ProgressPopup close() {
		if (closed)
			return null;
		closed = true;
		ProgressPopup previous = popup;
		popup = null;
		return previous;
	}

	synchronized boolean isClosed() {
		return closed;
	}
}
