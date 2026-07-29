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

interface SelectionToolbarHandler {
	interface SelectionUpdateHandler {
		void onNewSelection(Selection selection);

		void onFail();
	}

	boolean isActive();

	ReaderViewModeState.Lease enterAdjustmentMode();

	void restoreAdjustmentMode(ReaderViewModeState.Lease lease);

	void moveSelectionBound(
			boolean start,
			int delta,
			SelectionUpdateHandler updateHandler);

	void clearSelection();

	void copyToClipboard(String text);

	boolean shouldPersistSelection();

	void showNewBookmark(Selection selection);

	void showBookmarks();

	void sendQuotation(Selection selection);

	void showSearch(String initialText);

	void scrollBy(int delta);
}
