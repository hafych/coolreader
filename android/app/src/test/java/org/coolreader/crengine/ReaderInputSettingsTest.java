/*
 * CoolReader for Android
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 2
 * of the License, or (at your option) any later version.
 */

package org.coolreader.crengine;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ReaderInputSettingsTest {
	@Test
	public void emptySettingsPreserveReaderDefaults() {
		ReaderInputSettings settings = capture(
				new Properties());

		assertFalse(settings.isTapZoneHighlightEnabled());
		assertFalse(settings.isDoubleTapSelectionEnabled());
		assertEquals(150, settings.bounceTapIntervalMs());
		assertEquals(0, settings.pageFlipsPerFullSwipe());
		assertEquals(
				Settings.TAP_ACTION_TYPE_LONGPRESS,
				settings.secondaryTapActionType());
		assertEquals(
				Settings.BACKLIGHT_CONTROL_FLICK_LEFT,
				settings.backlightControlFlick());
		assertEquals(
				Settings.BACKLIGHT_CONTROL_FLICK_RIGHT,
				settings.warmBacklightControlFlick());
		assertFalse(
				settings.isColdWarmBacklightControlTogether());
		assertTrue(settings.areVolumeKeysEnabled());
		assertEquals(
				Settings.SELECTION_ACTION_TOOLBAR,
				settings.selectionAction(false));
		assertEquals(
				Settings.SELECTION_ACTION_TOOLBAR,
				settings.selectionAction(true));
		assertNull(settings.tapActionId(
				5, ReaderAction.NORMAL));
	}

	@Test
	public void configuredSettingsAreCapturedTogether() {
		Properties values = new Properties();
		values.setBool(
				Settings.PROP_APP_TAP_ZONE_HILIGHT, true);
		values.setBool(
				Settings.PROP_APP_DOUBLE_TAP_SELECTION, true);
		values.setInt(
				Settings.PROP_APP_BOUNCE_TAP_INTERVAL, 200);
		values.setInt(
				Settings.PROP_APP_GESTURE_PAGE_FLIPPING, 12);
		values.setInt(
				Settings.PROP_APP_SECONDARY_TAP_ACTION_TYPE,
				Settings.TAP_ACTION_TYPE_DOUBLE);
		values.setInt(
				Settings.PROP_APP_FLICK_BACKLIGHT_CONTROL,
				Settings.BACKLIGHT_CONTROL_FLICK_RIGHT);
		values.setInt(
				Settings.PROP_APP_FLICK_WARMLIGHT_CONTROL,
				Settings.BACKLIGHT_CONTROL_FLICK_LEFT);
		values.setBool(
				Settings.PROP_APP_FLICK_BACKLIGHT_CONTROL_TOGETHER,
				true);
		values.setBool(
				Settings.PROP_CONTROLS_ENABLE_VOLUME_KEYS, false);
		values.setInt(
				Settings.PROP_APP_SELECTION_ACTION, 3);
		values.setInt(
				Settings.PROP_APP_MULTI_SELECTION_ACTION, 4);
		values.setProperty(
				ReaderAction.getTapZoneProp(
						5, ReaderAction.NORMAL),
				ReaderAction.PAGE_DOWN.id);
		values.setProperty(
				ReaderAction.getKeyProp(
						42, ReaderAction.LONG),
				ReaderAction.READER_MENU.id);

		ReaderInputSettings settings = capture(values);

		assertTrue(settings.isTapZoneHighlightEnabled());
		assertTrue(settings.isDoubleTapSelectionEnabled());
		assertEquals(200, settings.bounceTapIntervalMs());
		assertEquals(12, settings.pageFlipsPerFullSwipe());
		assertEquals(
				Settings.TAP_ACTION_TYPE_DOUBLE,
				settings.secondaryTapActionType());
		assertEquals(
				Settings.BACKLIGHT_CONTROL_FLICK_RIGHT,
				settings.backlightControlFlick());
		assertEquals(
				Settings.BACKLIGHT_CONTROL_FLICK_LEFT,
				settings.warmBacklightControlFlick());
		assertTrue(
				settings.isColdWarmBacklightControlTogether());
		assertFalse(settings.areVolumeKeysEnabled());
		assertEquals(3, settings.selectionAction(false));
		assertEquals(4, settings.selectionAction(true));
		assertEquals(
				ReaderAction.PAGE_DOWN.id,
				settings.tapActionId(
						5, ReaderAction.NORMAL));
		assertEquals(
				ReaderAction.READER_MENU.id,
				settings.keyActionId(
						42, ReaderAction.LONG));
	}

	@Test
	public void numericInputIsNormalizedToSupportedRanges() {
		Properties values = new Properties();
		values.setProperty(
				Settings.PROP_APP_BOUNCE_TAP_INTERVAL,
				"-1");
		values.setProperty(
				Settings.PROP_APP_GESTURE_PAGE_FLIPPING,
				"999");
		values.setProperty(
				Settings.PROP_APP_SECONDARY_TAP_ACTION_TYPE,
				"7");
		values.setProperty(
				Settings.PROP_APP_FLICK_BACKLIGHT_CONTROL,
				"7");
		ReaderInputSettings upper = capture(values);

		assertEquals(50, upper.bounceTapIntervalMs());
		assertEquals(20, upper.pageFlipsPerFullSwipe());
		assertEquals(
				Settings.TAP_ACTION_TYPE_LONGPRESS,
				upper.secondaryTapActionType());
		assertEquals(
				Settings.BACKLIGHT_CONTROL_FLICK_NONE,
				upper.backlightControlFlick());

		values.setProperty(
				Settings.PROP_APP_BOUNCE_TAP_INTERVAL,
				"invalid");
		values.setProperty(
				Settings.PROP_APP_GESTURE_PAGE_FLIPPING,
				"-9");
		ReaderInputSettings lower = capture(values);

		assertEquals(50, lower.bounceTapIntervalMs());
		assertEquals(0, lower.pageFlipsPerFullSwipe());
	}

	@Test
	public void capturedGenerationDoesNotFollowReplacement() {
		Properties oldValues = new Properties();
		oldValues.setBool(
				Settings.PROP_CONTROLS_ENABLE_VOLUME_KEYS, true);
		oldValues.setProperty(
				ReaderAction.getKeyProp(
						42, ReaderAction.NORMAL),
				ReaderAction.PAGE_UP.id);
		ReaderSettingsState state =
				new ReaderSettingsState(oldValues);
		ReaderInputSettings captured =
				ReaderInputSettings.capture(state.snapshot());

		Properties newValues = new Properties();
		newValues.setBool(
				Settings.PROP_CONTROLS_ENABLE_VOLUME_KEYS, false);
		newValues.setProperty(
				ReaderAction.getKeyProp(
						42, ReaderAction.NORMAL),
				ReaderAction.PAGE_DOWN.id);
		state.replace(newValues);

		assertTrue(captured.areVolumeKeysEnabled());
		assertEquals(
				ReaderAction.PAGE_UP.id,
				captured.keyActionId(
						42, ReaderAction.NORMAL));
		ReaderInputSettings replacement =
				ReaderInputSettings.capture(state.snapshot());
		assertFalse(replacement.areVolumeKeysEnabled());
		assertEquals(
				ReaderAction.PAGE_DOWN.id,
				replacement.keyActionId(
						42, ReaderAction.NORMAL));
	}

	@Test(expected = IllegalArgumentException.class)
	public void nullSnapshotIsRejected() {
		ReaderInputSettings.capture(null);
	}

	private static ReaderInputSettings capture(
			Properties values) {
		return ReaderInputSettings.capture(
				new ReaderSettingsState(values).snapshot());
	}
}
