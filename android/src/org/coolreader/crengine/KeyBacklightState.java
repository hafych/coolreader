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
 * Owns Activity key-backlight level and disabled flag.
 *
 * User activity and settings updates share one synchronized owner. Destroy
 * permanently closes the owner.
 */
final class KeyBacklightState {
	private int level = 1;
	private boolean disabled = true;
	private boolean closed;

	synchronized void setLevel(int level) {
		if (closed)
			return;
		this.level = level;
	}

	synchronized int getLevel() {
		return level;
	}

	synchronized void setDisabled(boolean disabled) {
		if (closed)
			return;
		this.disabled = disabled;
	}

	synchronized boolean isDisabled() {
		return !closed && disabled;
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
