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
 * Owns the Activity UI language code.
 *
 * Settings apply and locale resolution share one synchronized value. Destroy
 * permanently closes the owner.
 */
final class ActivityLanguageState {
	private String language;
	private boolean closed;

	synchronized void set(String language) {
		if (closed)
			return;
		this.language = language;
	}

	synchronized String get() {
		return closed ? null : language;
	}

	synchronized boolean close() {
		if (closed)
			return false;
		closed = true;
		language = null;
		return true;
	}

	synchronized boolean isClosed() {
		return closed;
	}
}
