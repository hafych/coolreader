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

import java.text.NumberFormat;
import java.util.Locale;

final class ProgressDisplayState {
	private ProgressDisplayState() {
	}

	static Snapshot format(
			int progress, int max, Locale locale) {
		if (locale == null)
			throw new IllegalArgumentException(
					"locale must not be null");
		int boundedMax = Math.max(0, max);
		int boundedProgress =
				Math.max(0, Math.min(progress, boundedMax));
		double fraction = boundedMax == 0
				? 0
				: (double) boundedProgress / boundedMax;
		NumberFormat percentFormat =
				NumberFormat.getPercentInstance(locale);
		percentFormat.setMaximumFractionDigits(0);
		return new Snapshot(
				boundedProgress + "/" + boundedMax,
				percentFormat.format(fraction));
	}

	static final class Snapshot {
		private final String number;
		private final String percent;

		private Snapshot(String number, String percent) {
			this.number = number;
			this.percent = percent;
		}

		String number() {
			return number;
		}

		String percent() {
			return percent;
		}
	}
}
