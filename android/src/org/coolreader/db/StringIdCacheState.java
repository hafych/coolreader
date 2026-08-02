/*
 * CoolReader for Android
 * Copyright (C) 2026 CoolReader Next contributors
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 2
 * of the License, or (at your option) any later version.
 */

package org.coolreader.db;

import java.util.HashMap;

/**
 * Owns a string→id lookup cache for MainDB series/folder/author tables.
 *
 * Get/put share one synchronized map so concurrent metadata inserts cannot
 * publish a half-updated cache view. Close permanently clears the map.
 */
final class StringIdCacheState {
	private final HashMap<String, Long> cache = new HashMap<>();
	private boolean closed;

	synchronized Long get(String key) {
		if (closed || key == null)
			return null;
		return cache.get(key);
	}

	synchronized void put(String key, Long id) {
		if (closed || key == null || id == null)
			return;
		cache.put(key, id);
	}

	synchronized int size() {
		return closed ? 0 : cache.size();
	}

	synchronized void clear() {
		if (!closed)
			cache.clear();
	}

	synchronized boolean close() {
		if (closed)
			return false;
		closed = true;
		cache.clear();
		return true;
	}

	synchronized boolean isClosed() {
		return closed;
	}
}
