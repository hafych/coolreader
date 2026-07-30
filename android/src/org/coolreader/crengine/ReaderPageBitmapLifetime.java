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

import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Prevents page bitmaps from returning to the reuse pool during a Canvas read.
 */
final class ReaderPageBitmapLifetime<T> {
	interface Releaser<T> {
		void release(T resource);
	}

	private final Releaser<T> releaser;
	private final IdentityHashMap<Read, Boolean> readers =
			new IdentityHashMap<>();
	private final IdentityHashMap<T, Boolean> deferred =
			new IdentityHashMap<>();
	private boolean closed;

	ReaderPageBitmapLifetime(Releaser<T> releaser) {
		if (releaser == null)
			throw new IllegalArgumentException(
					"releaser must not be null");
		this.releaser = releaser;
	}

	synchronized Read beginRead() {
		if (closed)
			return null;
		Read read = new Read();
		readers.put(read, Boolean.TRUE);
		return read;
	}

	synchronized boolean finishRead(Read read) {
		if (read == null || readers.remove(read) == null)
			return false;
		if (readers.isEmpty())
			releaseDeferred();
		return true;
	}

	synchronized boolean retire(T resource) {
		if (resource == null)
			return false;
		if (!readers.isEmpty()) {
			if (deferred.containsKey(resource))
				return false;
			deferred.put(resource, Boolean.TRUE);
			return false;
		}
		releaser.release(resource);
		return true;
	}

	synchronized boolean close() {
		if (closed)
			return false;
		closed = true;
		if (readers.isEmpty())
			releaseDeferred();
		return true;
	}

	private void releaseDeferred() {
		Iterator<Map.Entry<T, Boolean>> iterator =
				deferred.entrySet().iterator();
		while (iterator.hasNext()) {
			T resource = iterator.next().getKey();
			iterator.remove();
			releaser.release(resource);
		}
	}

	static final class Read {
		private Read() {
		}
	}
}
