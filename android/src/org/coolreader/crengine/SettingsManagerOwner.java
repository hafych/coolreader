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
 * Owns the BaseActivity settings manager for one generation.
 *
 * Installed once during service startup and permanently closed on destroy so
 * late settings load/save cannot revive a torn-down manager through a
 * parallel raw field.
 */
final class SettingsManagerOwner {
	private BaseActivity.SettingsManager manager;
	private boolean closed;

	synchronized boolean install(BaseActivity.SettingsManager next) {
		if (closed || next == null || manager != null)
			return false;
		manager = next;
		return true;
	}

	synchronized BaseActivity.SettingsManager get() {
		return closed ? null : manager;
	}

	/**
	 * Returns the manager or throws when services have not been started or
	 * the owner is closed.
	 */
	synchronized BaseActivity.SettingsManager require() {
		BaseActivity.SettingsManager current = get();
		if (current == null)
			throw new IllegalStateException(
					"Settings manager is not available");
		return current;
	}

	synchronized boolean close() {
		if (closed)
			return false;
		closed = true;
		if (manager != null)
			manager.close();
		manager = null;
		return true;
	}

	synchronized boolean isClosed() {
		return closed;
	}
}
