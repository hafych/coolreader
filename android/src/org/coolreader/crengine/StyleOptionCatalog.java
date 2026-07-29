/*
 * CoolReader for Android
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 2
 * of the License, or (at your option) any later version.
 */

package org.coolreader.crengine;

import org.coolreader.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Immutable, typed catalog of configurable document styles.
 */
final class StyleOptionCatalog {
	private final List<Entry> entries;

	StyleOptionCatalog(List<Entry> entries) {
		this.entries = Collections.unmodifiableList(
				new ArrayList<>(entries));
	}

	static StyleOptionCatalog legacy() {
		List<Entry> entries = new ArrayList<>();
		entries.add(new Entry("def", R.string.options_css_def));
		entries.add(new Entry("title", R.string.options_css_title));
		entries.add(new Entry("subtitle", R.string.options_css_subtitle));
		entries.add(new Entry("pre", R.string.options_css_pre));
		entries.add(new Entry("link", R.string.options_css_link));
		entries.add(new Entry("cite", R.string.options_css_cite));
		entries.add(new Entry("epigraph", R.string.options_css_epigraph));
		entries.add(new Entry("poem", R.string.options_css_poem));
		entries.add(new Entry(
				"text-author", R.string.options_css_textauthor));
		entries.add(new Entry("footnote", R.string.options_css_footnote));
		entries.add(new Entry(
				"footnote-link", R.string.options_css_footnotelink));
		entries.add(new Entry(
				"footnote-title", R.string.options_css_footnotetitle));
		entries.add(new Entry(
				"annotation", R.string.options_css_annotation));
		return new StyleOptionCatalog(entries);
	}

	List<Entry> entries() {
		return entries;
	}

	static final class Entry {
		private final String code;
		private final int titleId;

		Entry(String code, int titleId) {
			this.code = code;
			this.titleId = titleId;
		}

		String code() {
			return code;
		}

		int titleId() {
			return titleId;
		}
	}
}
