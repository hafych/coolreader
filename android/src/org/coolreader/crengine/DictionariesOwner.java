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

import org.coolreader.Dictionaries;

/**
 * Owns the Activity dictionary helper for one generation.
 *
 * Installed once during Activity create and permanently closed on destroy so
 * late Dictan result callbacks cannot revive a torn-down helper through a
 * parallel raw field.
 */
final class DictionariesOwner {
	private Dictionaries dictionaries;
	private boolean closed;

	synchronized boolean install(Dictionaries next) {
		if (closed || next == null || dictionaries != null)
			return false;
		dictionaries = next;
		return true;
	}

	synchronized Dictionaries get() {
		return closed ? null : dictionaries;
	}

	synchronized boolean close() {
		if (closed)
			return false;
		closed = true;
		dictionaries = null;
		return true;
	}

	synchronized boolean isClosed() {
		return closed;
	}
}
