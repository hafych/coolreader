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
 * Immutable result of resolving an app-language setting.
 */
final class AppLocaleSelection {
	private final Locale locale;
	private final String code;

	private AppLocaleSelection(Locale locale, String code) {
		this.locale = locale;
		this.code = code;
	}

	static AppLocaleSelection resolve(
			Settings.Lang language,
			Locale systemLocale) {
		if (language == null || systemLocale == null)
			throw new IllegalArgumentException(
					"language and system locale are required");
		if (language == Settings.Lang.DEFAULT) {
			return new AppLocaleSelection(
					systemLocale,
					Settings.Lang.getCode(systemLocale));
		}
		Locale locale = language.getLocale();
		if (locale == null)
			throw new IllegalArgumentException(
					"unsupported app language: " + language.code);
		return new AppLocaleSelection(locale, language.code);
	}

	Locale locale() {
		return locale;
	}

	String code() {
		return code;
	}
}
