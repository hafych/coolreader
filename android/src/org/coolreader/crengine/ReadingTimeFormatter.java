/*
 * CoolReader for Android
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 2
 * of the License, or (at your option) any later version.
 */

package org.coolreader.crengine;

import java.util.Locale;

/**
 * Pure, overflow-safe formatting for persisted reading durations.
 */
final class ReadingTimeFormatter {
	private static final long MILLIS_PER_MINUTE = 60_000L;
	private static final long MILLIS_PER_HOUR =
			60L * MILLIS_PER_MINUTE;

	private ReadingTimeFormatter() {
	}

	static String format(long elapsedMillis) {
		return format(elapsedMillis, Locale.getDefault());
	}

	static String format(long elapsedMillis, Locale locale) {
		if (locale == null)
			throw new IllegalArgumentException("locale is required");
		long duration = Math.max(0L, elapsedMillis);
		long hours = duration / MILLIS_PER_HOUR;
		long minutes =
				duration % MILLIS_PER_HOUR / MILLIS_PER_MINUTE;
		return String.format(locale, "%d:%02d", hours, minutes);
	}
}
