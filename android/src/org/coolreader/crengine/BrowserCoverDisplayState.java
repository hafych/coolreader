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
 * Owns FileBrowser cover-thumbnail display options for one browser generation.
 *
 * Screen size, size option and enabled flag share one synchronized snapshot so
 * resize and settings apply cannot leave mixed width/height. Size math is pure
 * and JVM-tested.
 */
final class BrowserCoverDisplayState {
	static final int SIZE_SMALL = 0;
	static final int SIZE_MEDIUM = 1;
	static final int SIZE_BIG = 2;

	private boolean enabled = true;
	private int sizeOption = SIZE_MEDIUM;
	private int screenWidth = 480;
	private int screenHeight = 320;
	private int coverWidth = 90;
	private int coverHeight = 120;
	private boolean closed;

	/**
	 * Pure cover dimensions from screen size and option (0=small, 1=medium,
	 * 2=big). Matches the historical FileBrowser thumbnail policy.
	 */
	static Dimensions computeDimensions(
			int screenWidth, int screenHeight, int sizeOption) {
		int minScreenSize = Math.min(screenWidth, screenHeight);
		if (minScreenSize < 0)
			minScreenSize = 0;
		int minh = 80;
		int maxh = minScreenSize / 3;
		if (maxh < minh)
			maxh = minh;
		int avgh = (minh + maxh) / 2;
		int h = avgh;
		if (sizeOption == SIZE_BIG)
			h = maxh;
		else if (sizeOption == SIZE_SMALL)
			h = minh;
		int w = h * 3 / 4;
		if (w < 1)
			w = 1;
		if (h < 1)
			h = 1;
		return new Dimensions(w, h);
	}

	synchronized boolean isEnabled() {
		return enabled;
	}

	/**
	 * Updates the enabled flag. Returns true when the published value changed.
	 */
	synchronized boolean setEnabled(boolean coverPagesEnabled) {
		if (closed || enabled == coverPagesEnabled)
			return false;
		enabled = coverPagesEnabled;
		return true;
	}

	synchronized int getSizeOption() {
		return sizeOption;
	}

	/**
	 * Updates size option and recomputes cover width/height from the current
	 * screen. Returns the new dimensions when accepted, or null when closed or
	 * the option is unchanged.
	 */
	synchronized Dimensions setSizeOption(int option) {
		if (closed || sizeOption == option)
			return null;
		sizeOption = option;
		return recomputeLocked();
	}

	/**
	 * Updates screen size and recomputes cover dimensions. Returns new
	 * dimensions when the published cover size changed, or null when closed or
	 * unchanged.
	 */
	synchronized Dimensions setScreenSize(int width, int height) {
		if (closed)
			return null;
		screenWidth = width;
		screenHeight = height;
		Dimensions next = computeDimensions(width, height, sizeOption);
		if (coverWidth == next.width && coverHeight == next.height)
			return null;
		coverWidth = next.width;
		coverHeight = next.height;
		return next;
	}

	synchronized int getCoverWidth() {
		return coverWidth;
	}

	synchronized int getCoverHeight() {
		return coverHeight;
	}

	synchronized int getScreenWidth() {
		return screenWidth;
	}

	synchronized int getScreenHeight() {
		return screenHeight;
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

	private Dimensions recomputeLocked() {
		Dimensions next =
				computeDimensions(screenWidth, screenHeight, sizeOption);
		coverWidth = next.width;
		coverHeight = next.height;
		return next;
	}

	static final class Dimensions {
		final int width;
		final int height;

		Dimensions(int width, int height) {
			this.width = width;
			this.height = height;
		}
	}
}
