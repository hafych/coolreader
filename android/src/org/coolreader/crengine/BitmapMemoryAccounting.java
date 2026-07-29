/*
 * CoolReader for Android
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 2
 * of the License, or (at your option) any later version.
 */

package org.coolreader.crengine;

final class BitmapMemoryAccounting {
	private static final int SURFACE_BYTES_PER_PIXEL = 2;

	private BitmapMemoryAccounting() {
	}

	static long bitmapBytes(int rowBytes, int height) {
		return checkedProduct(rowBytes, height);
	}

	static long surfaceBytes(int width, int height) {
		return checkedProduct(width, height) * SURFACE_BYTES_PER_PIXEL;
	}

	private static long checkedProduct(int first, int second) {
		if (first < 0 || second < 0)
			throw new IllegalArgumentException("dimensions must be non-negative");
		return (long) first * second;
	}
}
