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
 * Owns Activity E-Ink update mode and interval settings.
 *
 * Mode and interval publish through one synchronized owner so concurrent
 * settings apply cannot leave a mixed pair. Destroy permanently closes the
 * owner.
 */
final class EinkUpdateState {
	private EinkScreen.EinkUpdateMode mode = EinkScreen.EinkUpdateMode.Clear;
	private int interval;
	private boolean closed;

	synchronized void setMode(EinkScreen.EinkUpdateMode mode) {
		if (closed || mode == null)
			return;
		this.mode = mode;
	}

	synchronized EinkScreen.EinkUpdateMode getMode() {
		return mode;
	}

	synchronized void setInterval(int interval) {
		if (closed)
			return;
		this.interval = interval;
	}

	synchronized int getInterval() {
		return interval;
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
