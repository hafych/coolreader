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
 * Owns the lazy Handler cache of one DelayedExecutor.
 *
 * Ensure/get share one synchronized slot so concurrent posts cannot install
 * two handlers. Close permanently drops the handle.
 */
final class DelayedExecutorHandlerState {
	private Handler handler;
	private boolean closed;

	/**
	 * Returns the existing handler or installs {@code created} once.
	 */
	synchronized Handler ensure(Handler created) {
		if (closed)
			return null;
		if (handler == null) {
			if (created == null)
				throw new IllegalArgumentException(
						"handler must not be null");
			handler = created;
		}
		return handler;
	}

	synchronized Handler get() {
		return closed ? null : handler;
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
