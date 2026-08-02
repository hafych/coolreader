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
 * Owns the Engine progress dialog handle for one generation.
 *
 * Show/hide/replace publish through one synchronized owner so concurrent
 * progress UI transitions cannot leave a half-published dialog. Destroy
 * permanently closes the owner and returns the dialog for dismiss.
 */
final class EngineProgressDialogState {
	private ProgressDialog dialog;
	private boolean closed;

	synchronized void set(ProgressDialog dialog) {
		if (closed)
			return;
		this.dialog = dialog;
	}

	synchronized ProgressDialog get() {
		return closed ? null : dialog;
	}

	/**
	 * Clears the published dialog and returns it for dismiss.
	 */
	synchronized ProgressDialog take() {
		if (closed)
			return null;
		ProgressDialog previous = dialog;
		dialog = null;
		return previous;
	}

	/**
	 * Permanently closes the owner and returns any live dialog for dismiss.
	 */
	synchronized ProgressDialog close() {
		if (closed)
			return null;
		closed = true;
		ProgressDialog previous = dialog;
		dialog = null;
		return previous;
	}

	synchronized boolean isClosed() {
		return closed;
	}
}
