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

import android.os.Handler;

/**
 * Owns one BackgroundThread Handler slot (background looper or GUI).
 *
 * Get/set/clear share one synchronized slot so concurrent quit and post
 * paths cannot observe a half-cleared handler. Close permanently drops
 * the slot.
 */
final class BackgroundThreadHandlerState {
	private Handler handler;
	private boolean closed;

	synchronized void set(Handler value) {
		if (closed)
			return;
		handler = value;
	}

	synchronized Handler get() {
		return closed ? null : handler;
	}

	/**
	 * Clears and returns the current handler.
	 */
	synchronized Handler take() {
		if (closed)
			return null;
		Handler previous = handler;
		handler = null;
		return previous;
	}

	synchronized void clear() {
		if (!closed)
			handler = null;
	}

	synchronized boolean close() {
		if (closed)
			return false;
		closed = true;
		handler = null;
		return true;
	}

	synchronized boolean isClosed() {
		return closed;
	}
}
