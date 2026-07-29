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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Immutable interface-theme definitions for one Activity generation.
 */
final class InterfaceThemeCatalog {
	private final List<InterfaceTheme> themes;

	private InterfaceThemeCatalog(List<InterfaceTheme> themes) {
		this.themes = Collections.unmodifiableList(
				new ArrayList<>(themes));
	}

	static InterfaceThemeCatalog create(boolean einkScreen) {
		int blackAlpha = einkScreen ? 0xFF : 0x80;
		int whiteAlpha = einkScreen ? 0xFF : 0xE0;
		return new InterfaceThemeCatalog(Arrays.asList(
				InterfaceTheme.create(
						"BLACK",
						R.style.Theme_Black,
						R.style.Theme_Black_Dialog_Normal,
						R.style.Theme_Black_Dialog_Fullscreen,
						R.string.options_app_ui_theme_black,
						0xFF000000,
						R.drawable.divider_black_tiled,
						2,
						R.drawable.ui_status_background_browser_black,
						R.drawable.ui_toolbar_background_browser_black,
						R.drawable.ui_toolbar_background_browser_vertical_black,
						0,
						0xFF000000,
						blackAlpha),
				InterfaceTheme.create(
						"WHITE",
						R.style.Theme_White,
						R.style.Theme_White_Dialog_Normal,
						R.style.Theme_White_Dialog_Fullscreen,
						R.string.options_app_ui_theme_white,
						0xFFFFFFFF,
						R.drawable.divider_white_tiled,
						2,
						R.drawable.ui_status_background_browser_white,
						R.drawable.ui_toolbar_background_browser_white,
						R.drawable.ui_toolbar_background_browser_vertical_white,
						0,
						0xFFFFFFFF,
						whiteAlpha),
				InterfaceTheme.create(
						"DARK",
						R.style.Theme_Dark,
						R.style.Theme_Dark_Dialog_Normal,
						R.style.Theme_Dark_Dialog_Fullscreen,
						R.string.options_app_ui_theme_dark,
						0xFF000000,
						R.drawable.divider_dark_tiled,
						16,
						R.drawable.ui_status_background_browser_dark,
						R.drawable.ui_toolbar_background_browser_dark,
						R.drawable.ui_toolbar_background_browser_vertical_dark,
						R.drawable.background_tiled_dark,
						0,
						einkScreen ? 0xFF : 0x90),
				InterfaceTheme.create(
						"LIGHT",
						R.style.Theme_Light,
						R.style.Theme_Light_Dialog_Normal,
						R.style.Theme_Light_Dialog_Fullscreen,
						R.string.options_app_ui_theme_light,
						0xFF000000,
						R.drawable.divider_light_tiled,
						16,
						R.drawable.ui_status_background_browser_light,
						R.drawable.ui_toolbar_background_browser_light,
						R.drawable.ui_toolbar_background_browser_vertical_light,
						R.drawable.background_tiled_light,
						0,
						einkScreen ? 0xFF : 0xC0),
				InterfaceTheme.create(
						"GRAY1",
						R.style.Theme_Gray1,
						R.style.Theme_Gray1_Dialog_Normal,
						R.style.Theme_Gray1_Dialog_Fullscreen,
						R.string.options_app_ui_theme_gray1,
						0xFF555555,
						R.drawable.divider_black_tiled,
						2,
						R.drawable.ui_status_background_browser_gray1,
						R.drawable.ui_toolbar_background_browser_gray1,
						R.drawable.ui_toolbar_background_browser_vertical_gray1,
						0,
						0xFF555555,
						blackAlpha),
				InterfaceTheme.create(
						"GRAY2",
						R.style.Theme_Gray2,
						R.style.Theme_Gray2_Dialog_Normal,
						R.style.Theme_Gray2_Dialog_Fullscreen,
						R.string.options_app_ui_theme_gray2,
						0xFFCCCCCC,
						R.drawable.divider_white_tiled,
						2,
						R.drawable.ui_status_background_browser_gray2,
						R.drawable.ui_toolbar_background_browser_gray2,
						R.drawable.ui_toolbar_background_browser_vertical_gray2,
						0,
						0xFFCCCCCC,
						whiteAlpha),
				InterfaceTheme.create(
						"HICONTRAST1",
						R.style.Theme_HiContrast1,
						R.style.Theme_HiContrast1_Dialog_Normal,
						R.style.Theme_HiContrast1_Dialog_Fullscreen,
						R.string.options_app_ui_theme_hicontrast1,
						0xFFFFFFFF,
						R.drawable.divider_white_tiled,
						2,
						R.drawable.ui_status_background_browser_white,
						R.drawable.ui_toolbar_background_browser_white,
						R.drawable.ui_toolbar_background_browser_vertical_white,
						0,
						0xFFFFFFFF,
						0xFF),
				InterfaceTheme.create(
						"HICONTRAST2",
						R.style.Theme_HiContrast2,
						R.style.Theme_HiContrast2_Dialog_Normal,
						R.style.Theme_HiContrast2_Dialog_Fullscreen,
						R.string.options_app_ui_theme_hicontrast2,
						0xFFFFFFFF,
						R.drawable.divider_black_tiled,
						2,
						R.drawable.ui_status_background_browser_black,
						R.drawable.ui_toolbar_background_browser_black,
						R.drawable.ui_toolbar_background_browser_vertical_black,
						0,
						0xFF000000,
						0xFF)));
	}

	List<InterfaceTheme> themes() {
		return themes;
	}

	InterfaceTheme findByCode(String code) {
		if (code == null)
			return null;
		for (InterfaceTheme theme : themes) {
			if (theme.getCode().equals(code))
				return theme;
		}
		return null;
	}
}
