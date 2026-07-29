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

import android.graphics.Bitmap;

import java.util.List;

interface TtsDocumentHandler {
	interface SelectionHandler {
		void onNewSelection(Selection selection);

		void onFail();
	}

	interface CoverHandler {
		void onCoverReady(Bitmap bitmap);
	}

	boolean isActive();

	void clearSelection();

	void savePosition();

	boolean enterReaderMode();

	void restoreReaderMode(boolean changed);

	void moveSelection(
			ReaderCommand command,
			SelectionHandler selectionHandler);

	void drawCover(Bitmap bitmap, CoverHandler coverHandler);

	List<SentenceInfo> getAllSentences();
}
