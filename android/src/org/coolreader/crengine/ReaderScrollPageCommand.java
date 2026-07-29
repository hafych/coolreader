/*
 * CoolReader for Android
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 2
 * of the License, or (at your option) any later version.
 */

package org.coolreader.crengine;

/**
 * Resolves one viewport-sized scroll without narrowing intermediate values.
 */
final class ReaderScrollPageCommand {
	private static final int STEP_NUMERATOR = 7;
	private static final int STEP_DENOMINATOR = 8;

	private ReaderScrollPageCommand() {
	}

	static Integer destination(
			PositionProperties position,
			int direction) {
		if (position == null || direction == 0)
			return null;
		long pageHeight = Math.max(
				0L, (long) position.pageHeight);
		long step =
				pageHeight * STEP_NUMERATOR
						/ STEP_DENOMINATOR;
		long destination =
				(long) position.y
						+ (direction < 0 ? -step : step);
		long maxScroll = Math.max(
				0L,
				(long) position.fullHeight
						- position.pageHeight);
		destination = Math.max(
				0L, Math.min(destination, maxScroll));
		return (int) Math.min(
				destination, Integer.MAX_VALUE);
	}
}
