/*
 * CoolReader for Android
 * Copyright (C) 2026 CoolReader Next contributors
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 2
 * of the License, or (at your option) any later version.
 */

package org.coolreader;

import android.content.SharedPreferences;

/**
 * Owns the Activity SharedPreferences handle used for last-location and
 * notification bookmarks.
 *
 * Preferences are created lazily through a factory so unit tests can supply a
 * fake store. Destroy permanently closes the owner so late UI paths cannot
 * install a new handle into a torn-down Activity.
 */
final class ActivityPreferencesState {
	interface PreferencesFactory {
		SharedPreferences create();
	}

	private SharedPreferences preferences;
	private boolean closed;

	/**
	 * Returns the existing preferences or creates one while the owner is open.
	 */
	synchronized SharedPreferences ensure(PreferencesFactory factory) {
		if (closed)
			return null;
		if (factory == null)
			throw new IllegalArgumentException(
					"factory must not be null");
		if (preferences == null)
			preferences = factory.create();
		return preferences;
	}

	synchronized SharedPreferences get() {
		return closed ? null : preferences;
	}

	/**
	 * Permanently closes the owner and drops the preferences reference.
	 */
	synchronized boolean close() {
		if (closed)
			return false;
		closed = true;
		preferences = null;
		return true;
	}

	synchronized boolean isClosed() {
		return closed;
	}
}
