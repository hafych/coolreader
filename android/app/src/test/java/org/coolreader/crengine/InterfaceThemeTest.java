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
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.coolreader.R;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class InterfaceThemeTest {
	@Test
	public void catalogPreservesLegacyOrderAndLookup() {
		InterfaceThemeCatalog catalog =
				InterfaceThemeCatalog.create(false);
		List<InterfaceTheme> themes = catalog.themes();
		List<String> codes = new ArrayList<>();
		for (InterfaceTheme theme : themes)
			codes.add(theme.getCode());

		assertEquals(
				Arrays.asList(
						"BLACK",
						"WHITE",
						"DARK",
						"LIGHT",
						"GRAY1",
						"GRAY2",
						"HICONTRAST1",
						"HICONTRAST2"),
				codes);
		assertSame(themes.get(2), catalog.findByCode("DARK"));
		assertNull(catalog.findByCode("missing"));
		assertNull(catalog.findByCode(null));
	}

	@Test
	public void catalogCannotBeMutated() {
		InterfaceThemeCatalog catalog =
				InterfaceThemeCatalog.create(false);
		try {
			catalog.themes().clear();
			fail("theme catalog was mutable");
		} catch (UnsupportedOperationException expected) {
			// Expected.
		}
		assertEquals(8, catalog.themes().size());
	}

	@Test
	public void definitionsHaveOnlyFinalPrivateStorage() {
		for (Field field : InterfaceTheme.class.getDeclaredFields()) {
			assertFalse(Modifier.isStatic(field.getModifiers()));
			assertTrue(Modifier.isPrivate(field.getModifiers()));
			assertTrue(Modifier.isFinal(field.getModifiers()));
		}
		for (Class<?> nested : InterfaceTheme.class.getDeclaredClasses()) {
			for (Field field : nested.getDeclaredFields()) {
				assertFalse(Modifier.isStatic(field.getModifiers()));
				assertTrue(Modifier.isPrivate(field.getModifiers()));
				assertTrue(Modifier.isFinal(field.getModifiers()));
			}
		}
		for (Field field :
				InterfaceThemeCatalog.class.getDeclaredFields()) {
			assertFalse(Modifier.isStatic(field.getModifiers()));
			assertTrue(Modifier.isPrivate(field.getModifiers()));
			assertTrue(Modifier.isFinal(field.getModifiers()));
		}
	}

	@Test
	public void visualValuesMatchLegacyDefinitions() {
		InterfaceThemeCatalog catalog =
				InterfaceThemeCatalog.create(false);
		assertVisuals(
				catalog.findByCode("BLACK"),
				R.drawable.divider_black_tiled,
				2,
				R.drawable.ui_status_background_browser_black,
				R.drawable.ui_toolbar_background_browser_black,
				R.drawable.ui_toolbar_background_browser_vertical_black,
				0,
				0xFF000000,
				0x80);
		assertVisuals(
				catalog.findByCode("WHITE"),
				R.drawable.divider_white_tiled,
				2,
				R.drawable.ui_status_background_browser_white,
				R.drawable.ui_toolbar_background_browser_white,
				R.drawable.ui_toolbar_background_browser_vertical_white,
				0,
				0xFFFFFFFF,
				0xE0);
		assertVisuals(
				catalog.findByCode("DARK"),
				R.drawable.divider_dark_tiled,
				16,
				R.drawable.ui_status_background_browser_dark,
				R.drawable.ui_toolbar_background_browser_dark,
				R.drawable.ui_toolbar_background_browser_vertical_dark,
				R.drawable.background_tiled_dark,
				0,
				0x90);
		assertVisuals(
				catalog.findByCode("LIGHT"),
				R.drawable.divider_light_tiled,
				16,
				R.drawable.ui_status_background_browser_light,
				R.drawable.ui_toolbar_background_browser_light,
				R.drawable.ui_toolbar_background_browser_vertical_light,
				R.drawable.background_tiled_light,
				0,
				0xC0);
		assertVisuals(
				catalog.findByCode("GRAY1"),
				R.drawable.divider_black_tiled,
				2,
				R.drawable.ui_status_background_browser_gray1,
				R.drawable.ui_toolbar_background_browser_gray1,
				R.drawable.ui_toolbar_background_browser_vertical_gray1,
				0,
				0xFF555555,
				0x80);
		assertVisuals(
				catalog.findByCode("GRAY2"),
				R.drawable.divider_white_tiled,
				2,
				R.drawable.ui_status_background_browser_gray2,
				R.drawable.ui_toolbar_background_browser_gray2,
				R.drawable.ui_toolbar_background_browser_vertical_gray2,
				0,
				0xFFCCCCCC,
				0xE0);
		assertVisuals(
				catalog.findByCode("HICONTRAST1"),
				R.drawable.divider_white_tiled,
				2,
				R.drawable.ui_status_background_browser_white,
				R.drawable.ui_toolbar_background_browser_white,
				R.drawable.ui_toolbar_background_browser_vertical_white,
				0,
				0xFFFFFFFF,
				0xFF);
		assertVisuals(
				catalog.findByCode("HICONTRAST2"),
				R.drawable.divider_black_tiled,
				2,
				R.drawable.ui_status_background_browser_black,
				R.drawable.ui_toolbar_background_browser_black,
				R.drawable.ui_toolbar_background_browser_vertical_black,
				0,
				0xFF000000,
				0xFF);
	}

	@Test
	public void einkCatalogForcesOpaqueToolbarButtons() {
		for (InterfaceTheme theme :
				InterfaceThemeCatalog.create(true).themes()) {
			assertEquals(0xFF, theme.getToolbarButtonAlpha());
		}
	}

	private static void assertVisuals(
			InterfaceTheme theme,
			int rootDelimiter,
			int rootDelimiterHeight,
			int statusBackground,
			int toolbarBackground,
			int verticalToolbarBackground,
			int popupBackground,
			int popupBackgroundColor,
			int toolbarButtonAlpha) {
		assertEquals(
				rootDelimiter,
				theme.getRootDelimiterResourceId());
		assertEquals(
				rootDelimiterHeight,
				theme.getRootDelimiterHeight());
		assertEquals(
				statusBackground,
				theme.getBrowserStatusBackground());
		assertEquals(
				toolbarBackground,
				theme.getBrowserToolbarBackground(false));
		assertEquals(
				verticalToolbarBackground,
				theme.getBrowserToolbarBackground(true));
		assertEquals(
				popupBackground,
				theme.getPopupToolbarBackground());
		assertEquals(
				popupBackgroundColor,
				theme.getPopupToolbarBackgroundColor());
		assertEquals(
				toolbarButtonAlpha,
				theme.getToolbarButtonAlpha());
	}
}
