/*
 * CoolReader for Android
 * Copyright (C) 2026 CoolReader Next contributors
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 2
 * of the License, or (at your option) any later version.
 */

package org.coolreader.crengine;

final class ReaderInputSettings implements Settings {
	private static final int DEFAULT_BOUNCE_INTERVAL_MS = 150;
	private static final int MIN_BOUNCE_INTERVAL_MS = 50;
	private static final int MAX_BOUNCE_INTERVAL_MS = 250;
	private static final int MAX_PAGE_FLIPS_PER_SWIPE = 20;

	private final ReaderSettingsState.Snapshot settings;
	private final boolean tapZoneHighlightEnabled;
	private final boolean doubleTapSelectionEnabled;
	private final int bounceTapIntervalMs;
	private final int pageFlipsPerFullSwipe;
	private final int secondaryTapActionType;
	private final int backlightControlFlick;
	private final int warmBacklightControlFlick;
	private final boolean coldWarmBacklightControlTogether;
	private final boolean volumeKeysEnabled;
	private final int selectionAction;
	private final int multiSelectionAction;

	private ReaderInputSettings(
			ReaderSettingsState.Snapshot settings) {
		if (settings == null)
			throw new IllegalArgumentException(
					"settings must not be null");
		this.settings = settings;
		tapZoneHighlightEnabled = settings.getBool(
				PROP_APP_TAP_ZONE_HILIGHT, false);
		doubleTapSelectionEnabled = settings.getBool(
				PROP_APP_DOUBLE_TAP_SELECTION, false);
		bounceTapIntervalMs = parseBoundedInt(
				settings.getProperty(
						PROP_APP_BOUNCE_TAP_INTERVAL),
				DEFAULT_BOUNCE_INTERVAL_MS,
				MIN_BOUNCE_INTERVAL_MS,
				MIN_BOUNCE_INTERVAL_MS,
				MAX_BOUNCE_INTERVAL_MS);
		pageFlipsPerFullSwipe = parseBoundedInt(
				settings.getProperty(
						PROP_APP_GESTURE_PAGE_FLIPPING),
				0,
				0,
				0,
				MAX_PAGE_FLIPS_PER_SWIPE);
		secondaryTapActionType =
				settings.getInt(
						PROP_APP_SECONDARY_TAP_ACTION_TYPE,
						TAP_ACTION_TYPE_LONGPRESS)
								== TAP_ACTION_TYPE_DOUBLE
						? TAP_ACTION_TYPE_DOUBLE
						: TAP_ACTION_TYPE_LONGPRESS;
		backlightControlFlick = parseFlick(
				settings.getProperty(
						PROP_APP_FLICK_BACKLIGHT_CONTROL),
				BACKLIGHT_CONTROL_FLICK_LEFT);
		warmBacklightControlFlick = parseFlick(
				settings.getProperty(
						PROP_APP_FLICK_WARMLIGHT_CONTROL),
				BACKLIGHT_CONTROL_FLICK_RIGHT);
		coldWarmBacklightControlTogether = settings.getBool(
				PROP_APP_FLICK_BACKLIGHT_CONTROL_TOGETHER,
				false);
		volumeKeysEnabled = settings.getBool(
				PROP_CONTROLS_ENABLE_VOLUME_KEYS, true);
		selectionAction = settings.getInt(
				PROP_APP_SELECTION_ACTION,
				SELECTION_ACTION_TOOLBAR);
		multiSelectionAction = settings.getInt(
				PROP_APP_MULTI_SELECTION_ACTION,
				SELECTION_ACTION_TOOLBAR);
	}

	static ReaderInputSettings capture(
			ReaderSettingsState.Snapshot settings) {
		return new ReaderInputSettings(settings);
	}

	String tapActionId(int zone, int type) {
		return settings.getProperty(
				ReaderAction.getTapZoneProp(zone, type));
	}

	String keyActionId(int keyCode, int type) {
		return settings.getProperty(
				ReaderAction.getKeyProp(keyCode, type));
	}

	boolean isTapZoneHighlightEnabled() {
		return tapZoneHighlightEnabled;
	}

	boolean isDoubleTapSelectionEnabled() {
		return doubleTapSelectionEnabled;
	}

	int bounceTapIntervalMs() {
		return bounceTapIntervalMs;
	}

	int pageFlipsPerFullSwipe() {
		return pageFlipsPerFullSwipe;
	}

	int secondaryTapActionType() {
		return secondaryTapActionType;
	}

	int backlightControlFlick() {
		return backlightControlFlick;
	}

	int warmBacklightControlFlick() {
		return warmBacklightControlFlick;
	}

	boolean isColdWarmBacklightControlTogether() {
		return coldWarmBacklightControlTogether;
	}

	boolean areVolumeKeysEnabled() {
		return volumeKeysEnabled;
	}

	int selectionAction(boolean multiSelection) {
		return multiSelection
				? multiSelectionAction
				: selectionAction;
	}

	private static int parseFlick(
			String value, int defaultValue) {
		if (value == null)
			return defaultValue;
		if ("1".equals(value))
			return BACKLIGHT_CONTROL_FLICK_LEFT;
		if ("2".equals(value))
			return BACKLIGHT_CONTROL_FLICK_RIGHT;
		return BACKLIGHT_CONTROL_FLICK_NONE;
	}

	private static int parseBoundedInt(
			String value,
			int missingValue,
			int invalidValue,
			int minValue,
			int maxValue) {
		if (value == null)
			return missingValue;
		int parsed;
		try {
			parsed = Integer.parseInt(value);
		} catch (NumberFormatException ignored) {
			parsed = invalidValue;
		}
		if (parsed < minValue)
			return minValue;
		if (parsed > maxValue)
			return maxValue;
		return parsed;
	}
}
