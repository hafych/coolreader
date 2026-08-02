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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Owns CoverpageManager ready-listener registration for one generation.
 *
 * Add/remove and notification snapshots share one synchronized owner so a
 * concurrent unregister cannot leave a half-updated listener list. Close
 * permanently drops all listeners.
 */
final class CoverpageListenerRegistry {
	private final ArrayList<CoverpageManager.CoverpageReadyListener> listeners =
			new ArrayList<>();
	private boolean closed;

	synchronized void add(CoverpageManager.CoverpageReadyListener listener) {
		if (closed || listener == null)
			return;
		if (!listeners.contains(listener))
			listeners.add(listener);
	}

	synchronized void remove(CoverpageManager.CoverpageReadyListener listener) {
		if (closed || listener == null)
			return;
		listeners.remove(listener);
	}

	/**
	 * Snapshot for notification so delivery does not hold the registry lock.
	 */
	synchronized List<CoverpageManager.CoverpageReadyListener> snapshot() {
		if (closed || listeners.isEmpty())
			return Collections.emptyList();
		return Collections.unmodifiableList(new ArrayList<>(listeners));
	}

	synchronized void clear() {
		if (!closed)
			listeners.clear();
	}

	synchronized boolean close() {
		if (closed)
			return false;
		closed = true;
		listeners.clear();
		return true;
	}

	synchronized boolean isClosed() {
		return closed;
	}
}
