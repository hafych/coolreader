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
 * Owns the lazy-resolved hardware-menu-key presence flag.
 *
 * Detection is published once and reused; destroy permanently closes the
 * owner so late UI paths cannot rewrite the cache.
 */
final class HardwareMenuKeyState {
	private Boolean hasHardwareMenuKey;
	private boolean closed;

	/**
	 * Returns the cached value, or null when still unset / closed.
	 */
	synchronized Boolean get() {
		return closed ? null : hasHardwareMenuKey;
	}

	/**
	 * Publishes the detection result while open. No-op after close.
	 */
	synchronized void set(boolean value) {
		if (closed)
			return;
		hasHardwareMenuKey = value;
	}

	synchronized boolean isResolved() {
		return !closed && hasHardwareMenuKey != null;
	}

	synchronized boolean close() {
		if (closed)
			return false;
		closed = true;
		hasHardwareMenuKey = null;
		return true;
	}

	synchronized boolean isClosed() {
		return closed;
	}
}
