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

final class SelectionModeState {
	private boolean active;
	private boolean closed;

	synchronized boolean toggle() {
		if (closed)
			return false;
		active = !active;
		return active;
	}

	synchronized boolean isActive() {
		return !closed && active;
	}

	synchronized boolean consume() {
		if (closed || !active)
			return false;
		active = false;
		return true;
	}

	synchronized boolean close() {
		if (closed)
			return false;
		closed = true;
		active = false;
		return true;
	}
}
