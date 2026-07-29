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
import static org.junit.Assert.fail;

import org.coolreader.R;
import org.junit.Test;

public class StyleOptionCatalogTest {
	@Test
	public void legacyCatalogPreservesAllTypedPairsInOrder() {
		StyleOptionCatalog catalog = StyleOptionCatalog.legacy();

		assertEquals(13, catalog.entries().size());
		assertEntry(catalog, 0, "def", R.string.options_css_def);
		assertEntry(catalog, 1, "title", R.string.options_css_title);
		assertEntry(
				catalog,
				2,
				"subtitle",
				R.string.options_css_subtitle);
		assertEntry(catalog, 3, "pre", R.string.options_css_pre);
		assertEntry(catalog, 4, "link", R.string.options_css_link);
		assertEntry(catalog, 5, "cite", R.string.options_css_cite);
		assertEntry(
				catalog,
				6,
				"epigraph",
				R.string.options_css_epigraph);
		assertEntry(catalog, 7, "poem", R.string.options_css_poem);
		assertEntry(
				catalog,
				8,
				"text-author",
				R.string.options_css_textauthor);
		assertEntry(
				catalog,
				9,
				"footnote",
				R.string.options_css_footnote);
		assertEntry(
				catalog,
				10,
				"footnote-link",
				R.string.options_css_footnotelink);
		assertEntry(
				catalog,
				11,
				"footnote-title",
				R.string.options_css_footnotetitle);
		assertEntry(
				catalog,
				12,
				"annotation",
				R.string.options_css_annotation);
	}

	@Test
	public void entryListCannotBeMutated() {
		try {
			StyleOptionCatalog.legacy().entries().clear();
			fail("style catalog entries were mutable");
		} catch (UnsupportedOperationException expected) {
			// Expected.
		}
	}

	private static void assertEntry(
			StyleOptionCatalog catalog,
			int index,
			String code,
			int titleId) {
		StyleOptionCatalog.Entry entry = catalog.entries().get(index);
		assertEquals(code, entry.code());
		assertEquals(titleId, entry.titleId());
	}
}
