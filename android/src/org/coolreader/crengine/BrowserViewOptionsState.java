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
 * Owns FileBrowser view-policy flags for one browser generation.
 *
 * Sort order, simple-list mode, empty-genre hiding and one-shot init share one
 * synchronized owner so settings apply and navigation cannot leave a mixed
 * policy after close.
 */
final class BrowserViewOptionsState {
	private FileInfo.SortOrder sortOrder = FileInfo.DEF_SORT_ORDER;
	private boolean simpleViewMode = true;
	private boolean hideEmptyGenres;
	private boolean initStarted;
	private boolean closed;

	synchronized void setHideEmptyGenres(boolean hide) {
		if (closed)
			return;
		hideEmptyGenres = hide;
	}

	synchronized boolean isHideEmptyGenres() {
		return hideEmptyGenres;
	}

	synchronized FileInfo.SortOrder getSortOrder() {
		return sortOrder;
	}

	/**
	 * Installs a sort order. Null falls back to the default. Returns true when
	 * the published value actually changed while open.
	 */
	synchronized boolean setSortOrder(FileInfo.SortOrder order) {
		if (closed)
			return false;
		FileInfo.SortOrder next =
				order != null ? order : FileInfo.DEF_SORT_ORDER;
		if (sortOrder == next)
			return false;
		sortOrder = next;
		return true;
	}

	synchronized boolean isSimpleViewMode() {
		return simpleViewMode;
	}

	/**
	 * Updates simple-view mode. Returns true when the value changed while open.
	 */
	synchronized boolean setSimpleViewMode(boolean simple) {
		if (closed || simpleViewMode == simple)
			return false;
		simpleViewMode = simple;
		if (simple)
			sortOrder = FileInfo.SortOrder.FILENAME;
		return true;
	}

	/**
	 * Marks init as started exactly once. Returns true on the first claim.
	 */
	synchronized boolean beginInit() {
		if (closed || initStarted)
			return false;
		initStarted = true;
		return true;
	}

	synchronized boolean isInitStarted() {
		return initStarted;
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
