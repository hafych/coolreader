/*
 * CoolReader for Android
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 2
 * of the License, or (at your option) any later version.
 */

package org.coolreader.crengine;

import java.util.Map;

/**
 * Immutable baseline for optimistic native-to-GUI settings readback.
 */
final class ReaderSettingsSyncSnapshot {
	private final Properties baseline;

	private ReaderSettingsSyncSnapshot(
			java.util.Properties baseline) {
		this.baseline = new Properties(baseline);
	}

	static ReaderSettingsSyncSnapshot capture(
			java.util.Properties baseline) {
		if (baseline == null)
			return null;
		return new ReaderSettingsSyncSnapshot(baseline);
	}

	Properties merge(
			java.util.Properties current,
			java.util.Properties nativeSettings) {
		if (current == null || nativeSettings == null)
			return null;
		Properties merged = new Properties(current);
		Properties nativeSnapshot =
				new Properties(nativeSettings);
		for (Map.Entry<Object, Object> entry :
				nativeSnapshot.entrySet()) {
			Object key = entry.getKey();
			boolean baselineContains =
					baseline.containsKey(key);
			boolean currentContains =
					merged.containsKey(key);
			if (baselineContains != currentContains
					|| !Properties.eq(
							baseline.get(key),
							merged.get(key)))
				continue;
			merged.setProperty(
					(String) key,
					(String) entry.getValue());
		}
		return merged;
	}
}
