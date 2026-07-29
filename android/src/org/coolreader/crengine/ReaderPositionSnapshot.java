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
 * Owns one immutable copy of a native last-position bookmark.
 */
final class ReaderPositionSnapshot {
	private final Bookmark bookmark;

	private ReaderPositionSnapshot(Bookmark bookmark) {
		this.bookmark = bookmark;
	}

	static ReaderPositionSnapshot capture(
			Bookmark source, long timestamp) {
		if (source == null)
			return null;
		Bookmark bookmark = new Bookmark(source);
		bookmark.setTimeStamp(timestamp);
		bookmark.setType(Bookmark.TYPE_LAST_POSITION);
		return new ReaderPositionSnapshot(bookmark);
	}

	Bookmark copyBookmark() {
		return new Bookmark(bookmark);
	}
}
