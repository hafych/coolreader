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
 * Owns the Activity settings-profile number.
 *
 * Lazy load, clamp, and switch publish one synchronized value so concurrent
 * profile queries cannot observe a half-updated id. Destroy permanently
 * closes the owner.
 */
final class ActivityProfileState {
	static final int UNSET = 0;

	private int profile = UNSET;
	private boolean closed;

	/**
	 * Returns the current profile, loading from {@code loader} when still
	 * unset. Clamps to {@code [1, maxProfiles]}.
	 */
	synchronized int getOrLoad(int maxProfiles, IntLoader loader) {
		if (closed)
			return 1;
		if (profile == UNSET) {
			if (loader == null)
				throw new IllegalArgumentException(
						"loader must not be null");
			int loaded = loader.load();
			if (loaded < 1 || loaded > maxProfiles)
				loaded = 1;
			profile = loaded;
		}
		return profile;
	}

	synchronized int get() {
		return closed ? UNSET : profile;
	}

	/**
	 * Publishes a new profile when open and the value is in range.
	 */
	synchronized boolean set(int next, int maxProfiles) {
		if (closed)
			return false;
		if (next < 1 || next > maxProfiles)
			return false;
		profile = next;
		return true;
	}

	synchronized boolean close() {
		if (closed)
			return false;
		closed = true;
		profile = UNSET;
		return true;
	}

	synchronized boolean isClosed() {
		return closed;
	}

	interface IntLoader {
		int load();
	}
}
