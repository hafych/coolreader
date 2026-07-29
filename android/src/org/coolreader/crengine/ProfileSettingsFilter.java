/*
 * CoolReader for Android
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 2
 * of the License, or (at your option) any later version.
 */

package org.coolreader.crengine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Immutable rule set for settings stored in numbered reader profiles.
 */
final class ProfileSettingsFilter {
	private final List<String> patterns;

	ProfileSettingsFilter(List<String> patterns) {
		if (patterns == null || patterns.isEmpty())
			throw new IllegalArgumentException(
					"profile patterns must not be empty");
		List<String> copy = new ArrayList<>(patterns.size());
		for (String pattern : patterns) {
			if (pattern == null || pattern.length() == 0)
				throw new IllegalArgumentException(
						"profile pattern must not be empty");
			copy.add(pattern);
		}
		this.patterns = Collections.unmodifiableList(copy);
	}

	static ProfileSettingsFilter legacy() {
		return new ProfileSettingsFilter(Arrays.asList(
				"background.*",
				Settings.PROP_NIGHT_MODE,
				"font.*",
				"crengine.page.*",
				Settings.PROP_FONT_SIZE,
				Settings.PROP_FALLBACK_FONT_FACES,
				Settings.PROP_INTERLINE_SPACE,
				Settings.PROP_STATUS_LINE,
				Settings.PROP_FOOTNOTES,
				"window.status.*",
				Settings.PROP_FLOATING_PUNCTUATION,
				Settings.PROP_LANDSCAPE_PAGES,
				Settings.PROP_HYPHENATION_DICT,
				"crengine.image.*",
				Settings.PROP_FORMAT_MIN_SPACE_CONDENSING_PERCENT,
				Settings.PROP_APP_FULLSCREEN,
				"app.screen.*",
				Settings.PROP_APP_DICTIONARY,
				Settings.PROP_APP_SELECTION_ACTION,
				Settings.PROP_APP_SELECTION_PERSIST,
				Settings.PROP_APP_HIGHLIGHT_BOOKMARKS + "*",
				Settings.PROP_HIGHLIGHT_SELECTION_COLOR + "*",
				Settings.PROP_HIGHLIGHT_BOOKMARK_COLOR_COMMENT + "*",
				Settings.PROP_HIGHLIGHT_BOOKMARK_COLOR_CORRECTION + "*",
				"viewer.*",
				Settings.PROP_APP_VIEW_AUTOSCROLL_SPEED,
				Settings.PROP_APP_VIEW_AUTOSCROLL_TYPE,
				"app.key.*",
				"app.tapzone.*",
				Settings.PROP_APP_DOUBLE_TAP_SELECTION,
				"app.touch.*",
				"app.ui.theme*"));
	}

	List<String> patterns() {
		return patterns;
	}

	boolean includes(String key) {
		if (key == null)
			return false;
		if (key.startsWith("styles."))
			return true;
		for (String pattern : patterns) {
			if (pattern.endsWith("*")) {
				String prefix =
						pattern.substring(0, pattern.length() - 1);
				if (key.startsWith(prefix))
					return true;
			} else if (pattern.equalsIgnoreCase(key)) {
				return true;
			}
		}
		return false;
	}

	Properties filter(Properties settings) {
		if (settings == null)
			throw new IllegalArgumentException("settings are required");
		Properties result = new Properties();
		synchronized (settings) {
			for (Object rawKey : settings.keySet()) {
				String key = (String) rawKey;
				if (includes(key)) {
					result.setProperty(
							key,
							settings.getProperty(key));
				}
			}
		}
		return result;
	}
}
