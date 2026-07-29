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
import static org.junit.Assert.assertTrue;

import android.view.KeyEvent;

import org.junit.Test;

import java.util.Properties;

public class DefaultInputActionsTest {
	@Test
	public void deviceFlagsSelectLegacyNavigationDefaults() {
		Properties regular = apply(
				DefaultInputActions.create(false, false, false), false);
		Properties sony = apply(
				DefaultInputActions.create(true, false, false), false);
		Properties leftRight = apply(
				DefaultInputActions.create(false, true, false), false);

		assertEquals(
				ReaderAction.REPEAT.id,
				key(regular, KeyEvent.KEYCODE_DPAD_UP, ReaderAction.LONG));
		assertEquals(
				ReaderAction.PAGE_UP_10.id,
				key(sony, KeyEvent.KEYCODE_DPAD_UP, ReaderAction.LONG));
		assertEquals(
				ReaderAction.PAGE_UP_10.id,
				key(regular, KeyEvent.KEYCODE_DPAD_LEFT, ReaderAction.NORMAL));
		assertEquals(
				ReaderAction.PAGE_UP.id,
				key(leftRight, KeyEvent.KEYCODE_DPAD_LEFT, ReaderAction.NORMAL));
	}

	@Test
	public void nookDefaultsOverrideOnlyConflictingGeneratedDefaults() {
		DefaultInputActions defaults =
				DefaultInputActions.create(false, false, true);
		Properties generated = apply(defaults, true);

		assertEquals(
				ReaderAction.PAGE_UP.id,
				key(
						generated,
						ReaderView.KEYCODE_PAGE_BOTTOMLEFT,
						ReaderAction.NORMAL));
		assertEquals(
				ReaderAction.PAGE_DOWN.id,
				key(
						generated,
						ReaderView.KEYCODE_PAGE_TOPLEFT,
						ReaderAction.NORMAL));
		assertEquals(
				ReaderAction.PAGE_UP.id,
				key(
						generated,
						ReaderView.NOOK_12_KEY_NEXT_LEFT,
						ReaderAction.NORMAL));

		Properties user = new Properties();
		user.setProperty(
				ReaderAction.getKeyProp(
						ReaderView.KEYCODE_PAGE_BOTTOMLEFT,
						ReaderAction.NORMAL),
				ReaderAction.SEARCH.id);
		defaults.applyTo(user, true, true, false);
		assertEquals(
				ReaderAction.SEARCH.id,
				key(
						user,
						ReaderView.KEYCODE_PAGE_BOTTOMLEFT,
						ReaderAction.NORMAL));
	}

	@Test
	public void inaccessibleMenuForcesOnlyCentralTapFallback() {
		DefaultInputActions defaults =
				DefaultInputActions.create(false, false, false);
		String center = ReaderAction.getTapZoneProp(
				5, ReaderAction.NORMAL);

		Properties inaccessible = new Properties();
		inaccessible.setProperty(center, ReaderAction.SEARCH.id);
		assertTrue(defaults.applyTo(
				inaccessible, false, false, false));
		assertEquals(ReaderAction.READER_MENU.id,
				inaccessible.getProperty(center));

		Properties hardwareMenu = new Properties();
		hardwareMenu.setProperty(center, ReaderAction.SEARCH.id);
		assertFalse(defaults.applyTo(
				hardwareMenu, false, true, false));
		assertEquals(ReaderAction.SEARCH.id,
				hardwareMenu.getProperty(center));

		Properties toolbar = new Properties();
		toolbar.setProperty(center, ReaderAction.SEARCH.id);
		assertFalse(defaults.applyTo(
				toolbar, false, false, true));
		assertEquals(ReaderAction.SEARCH.id,
				toolbar.getProperty(center));
	}

	@Test
	public void existingMenuTapPreventsCentralOverride() {
		DefaultInputActions defaults =
				DefaultInputActions.create(false, false, false);
		Properties properties = new Properties();
		String center = ReaderAction.getTapZoneProp(
				5, ReaderAction.NORMAL);
		properties.setProperty(center, ReaderAction.SEARCH.id);
		properties.setProperty(
				ReaderAction.getTapZoneProp(1, ReaderAction.NORMAL),
				ReaderAction.READER_MENU.id);

		assertFalse(defaults.applyTo(
				properties, false, false, false));
		assertEquals(ReaderAction.SEARCH.id,
				properties.getProperty(center));
		assertTrue(defaults.hasMenuTap(properties));
	}

	private static Properties apply(
			DefaultInputActions defaults, boolean includeNook) {
		Properties properties = new Properties();
		defaults.applyTo(properties, includeNook, true, false);
		return properties;
	}

	private static String key(
			Properties properties, int keyCode, int type) {
		return properties.getProperty(
				ReaderAction.getKeyProp(keyCode, type));
	}
}
