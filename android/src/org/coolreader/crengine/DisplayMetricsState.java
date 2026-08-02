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
 * Owns Activity display metrics used for layout and font bounds.
 *
 * Density, diagonal and font size limits publish through one synchronized
 * owner so concurrent settings/UI paths cannot observe a mixed generation.
 * Destroy permanently closes the owner.
 */
final class DisplayMetricsState {
	private int densityDpi = 160;
	private float diagonalInches = 5f;
	private int preferredItemHeight = 36;
	private int minFontSize = 9;
	private int maxFontSize = 90;
	private boolean closed;

	synchronized void setDensityDpi(int densityDpi) {
		if (closed)
			return;
		if (densityDpi > 0)
			this.densityDpi = densityDpi;
	}

	synchronized int getDensityDpi() {
		return densityDpi;
	}

	synchronized void setDiagonalInches(float diagonalInches) {
		if (closed)
			return;
		if (diagonalInches > 0f)
			this.diagonalInches = diagonalInches;
	}

	synchronized float getDiagonalInches() {
		return diagonalInches;
	}

	synchronized boolean isSmartphone() {
		return diagonalInches <= 6.8f;
	}

	synchronized void setPreferredItemHeight(int preferredItemHeight) {
		if (closed)
			return;
		if (preferredItemHeight > 0)
			this.preferredItemHeight = preferredItemHeight;
	}

	synchronized int getPreferredItemHeight() {
		return preferredItemHeight;
	}

	synchronized void setFontSizeBounds(int minFontSize, int maxFontSize) {
		if (closed)
			return;
		if (minFontSize > 0)
			this.minFontSize = minFontSize;
		if (maxFontSize > 0)
			this.maxFontSize = maxFontSize;
	}

	synchronized int getMinFontSize() {
		return minFontSize;
	}

	synchronized int getMaxFontSize() {
		return maxFontSize;
	}

	synchronized float getDensityFactor() {
		return ((float) densityDpi) / 160f;
	}

	synchronized int getPalmTipPixels() {
		return densityDpi / 3;
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
