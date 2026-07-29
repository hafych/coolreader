/*
 * CoolReader for Android
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 2
 * of the License, or (at your option) any later version.
 */

package org.coolreader.crengine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class ReaderSettingsSyncSnapshotTest {
	@Test
	public void nativeChangesMergeIntoUnchangedBaseline() {
		ReaderSettingsSyncSnapshot snapshot =
				ReaderSettingsSyncSnapshot.capture(
						properties(
								"font.size", "20",
								"font.face", "Old"));

		Properties merged = snapshot.merge(
				properties(
						"font.size", "20",
						"font.face", "Old",
						"theme", "night"),
				properties(
						"font.size", "22",
						"font.face", "New"));

		assertEquals("22", merged.getProperty("font.size"));
		assertEquals("New", merged.getProperty("font.face"));
		assertEquals("night", merged.getProperty("theme"));
	}

	@Test
	public void newerSameKeyGuiValuesWinOverNativeReadback() {
		ReaderSettingsSyncSnapshot snapshot =
				ReaderSettingsSyncSnapshot.capture(
						properties("font.size", "20"));

		Properties merged = snapshot.merge(
				properties("font.size", "30"),
				properties("font.size", "22"));

		assertEquals("30", merged.getProperty("font.size"));
	}

	@Test
	public void newNativeKeyRequiresNoNewerGuiOwner() {
		ReaderSettingsSyncSnapshot snapshot =
				ReaderSettingsSyncSnapshot.capture(
						properties());

		Properties merged = snapshot.merge(
				properties(
						"font.size", "30",
						"theme", "night"),
				properties(
						"font.size", "22",
						"hyphenation", "1"));

		assertEquals("30", merged.getProperty("font.size"));
		assertEquals("night", merged.getProperty("theme"));
		assertEquals(
				"1", merged.getProperty("hyphenation"));
	}

	@Test
	public void captureAndMergeOwnTheirSnapshots() {
		Properties baseline =
				properties("font.size", "20");
		ReaderSettingsSyncSnapshot snapshot =
				ReaderSettingsSyncSnapshot.capture(baseline);
		baseline.setProperty("font.size", "99");
		Properties current =
				properties("font.size", "20");
		Properties nativeSettings =
				properties("font.size", "22");

		Properties merged =
				snapshot.merge(current, nativeSettings);
		current.setProperty("font.size", "88");
		nativeSettings.setProperty("font.size", "77");

		assertEquals("22", merged.getProperty("font.size"));
		assertNotSame(current, merged);
		assertNotSame(nativeSettings, merged);
	}

	@Test
	public void missingBoundariesAreRejected() {
		assertNull(ReaderSettingsSyncSnapshot.capture(null));
		ReaderSettingsSyncSnapshot snapshot =
				ReaderSettingsSyncSnapshot.capture(
						properties());

		assertNull(snapshot.merge(
				null, properties()));
		assertNull(snapshot.merge(
				properties(), null));
	}

	private static Properties properties(String... pairs) {
		Properties properties = new Properties();
		for (int i = 0; i < pairs.length; i += 2)
			properties.setProperty(
					pairs[i], pairs[i + 1]);
		return properties;
	}
}
