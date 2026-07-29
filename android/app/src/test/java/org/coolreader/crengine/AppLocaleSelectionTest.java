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
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.fail;

import org.junit.Test;

import java.util.Locale;

public class AppLocaleSelectionTest {
	@Test
	public void systemSettingUsesCurrentActivitySnapshot() {
		AppLocaleSelection first = AppLocaleSelection.resolve(
				Settings.Lang.DEFAULT,
				new Locale("pt", "BR"));
		AppLocaleSelection second = AppLocaleSelection.resolve(
				Settings.Lang.DEFAULT,
				new Locale("pl", "PL"));

		assertEquals(new Locale("pt", "BR"), first.locale());
		assertEquals("pt_BR", first.code());
		assertEquals(new Locale("pl", "PL"), second.locale());
		assertEquals("pl_PL", second.code());
		assertNotEquals(first.code(), second.code());
	}

	@Test
	public void explicitSettingIgnoresSystemLocale() {
		AppLocaleSelection selection = AppLocaleSelection.resolve(
				Settings.Lang.UK,
				Locale.JAPAN);

		assertEquals(new Locale("uk"), selection.locale());
		assertEquals("uk", selection.code());
	}

	@Test
	public void missingInputsAreRejected() {
		assertRejected(null, Locale.ENGLISH);
		assertRejected(Settings.Lang.DEFAULT, null);
	}

	private static void assertRejected(
			Settings.Lang language,
			Locale systemLocale) {
		try {
			AppLocaleSelection.resolve(language, systemLocale);
			fail("invalid locale selection was accepted");
		} catch (IllegalArgumentException expected) {
			// Expected.
		}
	}
}
