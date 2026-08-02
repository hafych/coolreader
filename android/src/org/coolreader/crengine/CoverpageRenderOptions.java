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
 * Owns CoverpageManager render size and font-face options.
 *
 * Setters return whether the published value changed so callers can clear
 * caches only on real transitions. Close freezes the last snapshot.
 */
final class CoverpageRenderOptions {
	private int maxWidth = 110;
	private int maxHeight = 140;
	private String fontFace = "Droid Sans";
	private boolean closed;

	/**
	 * @return true when size actually changed
	 */
	synchronized boolean setSize(int width, int height) {
		if (closed)
			return false;
		if (maxWidth == width && maxHeight == height)
			return false;
		maxWidth = width;
		maxHeight = height;
		return true;
	}

	/**
	 * @return true when face actually changed
	 */
	synchronized boolean setFontFace(String face) {
		if (closed)
			return false;
		if (face == null)
			face = "Droid Sans";
		if (fontFace.equals(face))
			return false;
		fontFace = face;
		return true;
	}

	synchronized int getMaxWidth() {
		return maxWidth;
	}

	synchronized int getMaxHeight() {
		return maxHeight;
	}

	synchronized String getFontFace() {
		return fontFace;
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
