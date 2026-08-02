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
 * Owns the one-shot notice-dialog visibility flag for an Activity.
 *
 * Show attempts while already visible are rejected so overlapping notice
 * dialogs cannot stack from concurrent settings/load paths. Destroy closes
 * the owner permanently.
 */
final class NoticeDialogState {
	private boolean visible;
	private boolean closed;

	/**
	 * Claims visibility for a new notice dialog. Returns false when already
	 * visible or closed.
	 */
	synchronized boolean beginShow() {
		if (closed || visible)
			return false;
		visible = true;
		return true;
	}

	/**
	 * Clears visibility when the dialog is dismissed.
	 */
	synchronized void endShow() {
		if (!closed)
			visible = false;
	}

	synchronized boolean isVisible() {
		return !closed && visible;
	}

	synchronized boolean close() {
		if (closed)
			return false;
		closed = true;
		visible = false;
		return true;
	}

	synchronized boolean isClosed() {
		return closed;
	}
}
