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
 * Owns the Activity package version string.
 *
 * Package-info load and UI queries share one synchronized value. Destroy
 * permanently closes the owner.
 */
final class ActivityVersionState {
	private String version;
	private boolean closed;

	ActivityVersionState(String initial) {
		this.version = initial != null ? initial : "0";
	}

	synchronized void set(String version) {
		if (closed)
			return;
		if (version == null || version.length() == 0)
			return;
		this.version = version;
	}

	synchronized String get() {
		return version;
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
