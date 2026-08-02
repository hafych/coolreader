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

import android.content.pm.ActivityInfo;

/**
 * Owns the Activity requested screen orientation.
 *
 * Sensor and settings updates publish one synchronized value so concurrent
 * apply paths cannot leave a mixed orientation. Destroy permanently closes
 * the owner.
 */
final class ScreenOrientationState {
	private int orientation = ActivityInfo.SCREEN_ORIENTATION_USER;
	private boolean closed;

	synchronized void set(int orientation) {
		if (closed)
			return;
		this.orientation = orientation;
	}

	synchronized int get() {
		return closed
				? ActivityInfo.SCREEN_ORIENTATION_USER
				: orientation;
	}

	synchronized boolean close() {
		if (closed)
			return false;
		closed = true;
		orientation = ActivityInfo.SCREEN_ORIENTATION_USER;
		return true;
	}

	synchronized boolean isClosed() {
		return closed;
	}
}
