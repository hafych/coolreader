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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ProfileSettingsFilterTest {
	@Test
	public void legacyPatternsPreserveOrderAndValues() {
		assertEquals(
				Arrays.asList(
						"background.*",
						"crengine.night.mode",
						"font.*",
						"crengine.page.*",
						"crengine.font.size",
						"crengine.font.fallback.faces",
						"crengine.interline.space",
						"window.status.line",
						"crengine.footnotes",
						"window.status.*",
						"crengine.style.floating.punctuation.enabled",
						"window.landscape.pages",
						"crengine.hyphenation.directory",
						"crengine.image.*",
						"crengine.style.space.condensing.percent",
						"app.fullscreen",
						"app.screen.*",
						"app.dictionary.current",
						"app.selection.action",
						"app.selection.persist",
						"crengine.highlight.bookmarks*",
						"crengine.highlight.selection.color*",
						"crengine.highlight.bookmarks.color.comment*",
						"crengine.highlight.bookmarks.color.correction*",
						"viewer.*",
						"app.view.autoscroll.speed",
						"app.view.autoscroll.type",
						"app.key.*",
						"app.tapzone.*",
						"app.controls.doubletap.selection",
						"app.touch.*",
						"app.ui.theme*"),
				ProfileSettingsFilter.legacy().patterns());
	}

	@Test
	public void exactAndPrefixMatchingPreserveLegacyCaseRules() {
		ProfileSettingsFilter filter =
				ProfileSettingsFilter.legacy();

		assertTrue(filter.includes("CRENGINE.NIGHT.MODE"));
		assertTrue(filter.includes("background.texture"));
		assertFalse(filter.includes("Background.texture"));
		assertTrue(filter.includes("styles.footnote"));
		assertFalse(filter.includes("Styles.footnote"));
		assertFalse(filter.includes("app.locale"));
		assertFalse(filter.includes(null));
	}

	@Test
	public void filteringCopiesOnlyProfileSettings() {
		Properties source = new Properties();
		source.setProperty("font.face.default", "Noto Serif");
		source.setProperty("styles.title.color", "#000000");
		source.setProperty("app.locale", "pl");
		source.setProperty(Settings.PROP_PROFILE_NUMBER, "4");

		Properties result =
				ProfileSettingsFilter.legacy().filter(source);

		assertEquals("Noto Serif", result.getProperty("font.face.default"));
		assertEquals("#000000", result.getProperty("styles.title.color"));
		assertNull(result.getProperty("app.locale"));
		assertNull(result.getProperty(Settings.PROP_PROFILE_NUMBER));
		result.setProperty("font.face.default", "Changed");
		assertEquals(
				"Noto Serif",
				source.getProperty("font.face.default"));
	}

	@Test
	public void patternStorageIsCopiedAndUnmodifiable() {
		List<String> source =
				new ArrayList<>(Arrays.asList("first", "prefix.*"));
		ProfileSettingsFilter filter =
				new ProfileSettingsFilter(source);
		source.set(0, "changed");

		assertEquals(
				Arrays.asList("first", "prefix.*"),
				filter.patterns());
		try {
			filter.patterns().clear();
			fail("profile patterns were mutable");
		} catch (UnsupportedOperationException expected) {
			// Expected.
		}
	}

	@Test
	public void invalidDefinitionsAndInputAreRejected() {
		assertRejected(null);
		assertRejected(new ArrayList<String>());
		assertRejected(Arrays.asList("valid", ""));
		try {
			ProfileSettingsFilter.legacy().filter(null);
			fail("missing settings were accepted");
		} catch (IllegalArgumentException expected) {
			// Expected.
		}
	}

	private static void assertRejected(List<String> patterns) {
		try {
			new ProfileSettingsFilter(patterns);
			fail("invalid profile patterns were accepted");
		} catch (IllegalArgumentException expected) {
			// Expected.
		}
	}
}
