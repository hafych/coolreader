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
 * Owns symlink entries used by mount-path normalization.
 *
 * Add/snapshot share one synchronized list so concurrent path correction
 * cannot observe a half-published link set. Close permanently clears links.
 *
 * @param <T> link record type (package-private LinkInfo)
 */
final class MountPathLinkListState<T> {
	private final ArrayList<T> links = new ArrayList<>(4);
	private boolean closed;

	synchronized void add(T item) {
		if (closed || item == null)
			return;
		links.add(item);
	}

	synchronized int size() {
		return closed ? 0 : links.size();
	}

	synchronized List<T> snapshot() {
		if (closed)
			return Collections.emptyList();
		return Collections.unmodifiableList(new ArrayList<>(links));
	}

	synchronized boolean close() {
		if (closed)
			return false;
		closed = true;
		links.clear();
		return true;
	}

	synchronized boolean isClosed() {
		return closed;
	}
}
