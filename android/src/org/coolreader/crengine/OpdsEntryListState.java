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
 * Owns OPDS feed entry list for one parse generation.
 *
 * Add/size/snapshot share one synchronized list so partial parse UI updates
 * cannot escape a half-published backing array.
 */
final class OpdsEntryListState {
	private final ArrayList<OPDSUtil.EntryInfo> entries = new ArrayList<>();
	private boolean closed;

	synchronized boolean add(OPDSUtil.EntryInfo entry, int maxItems) {
		if (closed || entry == null)
			return false;
		if (entries.size() >= maxItems)
			return false;
		entries.add(entry);
		return true;
	}

	synchronized int size() {
		return closed ? 0 : entries.size();
	}

	synchronized ArrayList<OPDSUtil.EntryInfo> copyAsArrayList() {
		return closed
				? new ArrayList<>()
				: new ArrayList<>(entries);
	}

	synchronized List<OPDSUtil.EntryInfo> snapshot() {
		if (closed)
			return Collections.emptyList();
		return Collections.unmodifiableList(new ArrayList<>(entries));
	}

	synchronized void clear() {
		if (!closed)
			entries.clear();
	}

	synchronized boolean close() {
		if (closed)
			return false;
		closed = true;
		entries.clear();
		return true;
	}

	synchronized boolean isClosed() {
		return closed;
	}
}
