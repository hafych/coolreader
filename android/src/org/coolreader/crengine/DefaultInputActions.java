/*
 * CoolReader for Android
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 2
 * of the License, or (at your option) any later version.
 */

package org.coolreader.crengine;

import android.view.KeyEvent;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Immutable, device-specific input defaults for one SettingsManager.
 */
final class DefaultInputActions {
	private final List<KeyAction> keyActions;
	private final List<KeyAction> nookKeyActions;
	private final List<TapAction> tapActions;

	private DefaultInputActions(
			List<KeyAction> keyActions,
			List<KeyAction> nookKeyActions,
			List<TapAction> tapActions) {
		this.keyActions = Collections.unmodifiableList(keyActions);
		this.nookKeyActions = Collections.unmodifiableList(nookKeyActions);
		this.tapActions = Collections.unmodifiableList(tapActions);
	}

	static DefaultInputActions create(
			boolean einkSony,
			boolean navigateLeftRight,
			boolean einkNook) {
		return new DefaultInputActions(
				Arrays.asList(
						key(KeyEvent.KEYCODE_BACK, ReaderAction.NORMAL, ReaderAction.GO_BACK),
						key(KeyEvent.KEYCODE_BACK, ReaderAction.LONG, ReaderAction.EXIT),
						key(KeyEvent.KEYCODE_BACK, ReaderAction.DOUBLE, ReaderAction.EXIT),
						key(KeyEvent.KEYCODE_DPAD_CENTER, ReaderAction.NORMAL, ReaderAction.RECENT_BOOKS),
						key(KeyEvent.KEYCODE_DPAD_CENTER, ReaderAction.LONG, ReaderAction.BOOKMARKS),
						key(KeyEvent.KEYCODE_DPAD_UP, ReaderAction.NORMAL, ReaderAction.PAGE_UP),
						key(KeyEvent.KEYCODE_DPAD_DOWN, ReaderAction.NORMAL, ReaderAction.PAGE_DOWN),
						key(
								KeyEvent.KEYCODE_DPAD_UP,
								ReaderAction.LONG,
								einkSony ? ReaderAction.PAGE_UP_10 : ReaderAction.REPEAT),
						key(
								KeyEvent.KEYCODE_DPAD_DOWN,
								ReaderAction.LONG,
								einkSony ? ReaderAction.PAGE_DOWN_10 : ReaderAction.REPEAT),
						key(
								KeyEvent.KEYCODE_DPAD_LEFT,
								ReaderAction.NORMAL,
								navigateLeftRight
										? ReaderAction.PAGE_UP
										: ReaderAction.PAGE_UP_10),
						key(
								KeyEvent.KEYCODE_DPAD_RIGHT,
								ReaderAction.NORMAL,
								navigateLeftRight
										? ReaderAction.PAGE_DOWN
										: ReaderAction.PAGE_DOWN_10),
						key(KeyEvent.KEYCODE_DPAD_LEFT, ReaderAction.LONG, ReaderAction.REPEAT),
						key(KeyEvent.KEYCODE_DPAD_RIGHT, ReaderAction.LONG, ReaderAction.REPEAT),
						key(KeyEvent.KEYCODE_VOLUME_UP, ReaderAction.NORMAL, ReaderAction.PAGE_UP),
						key(KeyEvent.KEYCODE_VOLUME_DOWN, ReaderAction.NORMAL, ReaderAction.PAGE_DOWN),
						key(KeyEvent.KEYCODE_VOLUME_UP, ReaderAction.LONG, ReaderAction.REPEAT),
						key(KeyEvent.KEYCODE_VOLUME_DOWN, ReaderAction.LONG, ReaderAction.REPEAT),
						key(KeyEvent.KEYCODE_MENU, ReaderAction.NORMAL, ReaderAction.READER_MENU),
						key(KeyEvent.KEYCODE_MENU, ReaderAction.LONG, ReaderAction.OPTIONS),
						key(KeyEvent.KEYCODE_CAMERA, ReaderAction.NORMAL, ReaderAction.NONE),
						key(KeyEvent.KEYCODE_CAMERA, ReaderAction.LONG, ReaderAction.NONE),
						key(KeyEvent.KEYCODE_SEARCH, ReaderAction.NORMAL, ReaderAction.SEARCH),
						key(KeyEvent.KEYCODE_SEARCH, ReaderAction.LONG, ReaderAction.TOGGLE_SELECTION_MODE),
						key(KeyEvent.KEYCODE_PAGE_UP, ReaderAction.NORMAL, ReaderAction.PAGE_UP),
						key(KeyEvent.KEYCODE_PAGE_UP, ReaderAction.LONG, ReaderAction.NONE),
						key(KeyEvent.KEYCODE_PAGE_UP, ReaderAction.DOUBLE, ReaderAction.NONE),
						key(KeyEvent.KEYCODE_PAGE_DOWN, ReaderAction.NORMAL, ReaderAction.PAGE_DOWN),
						key(KeyEvent.KEYCODE_PAGE_DOWN, ReaderAction.LONG, ReaderAction.NONE),
						key(KeyEvent.KEYCODE_PAGE_DOWN, ReaderAction.DOUBLE, ReaderAction.NONE),
						key(ReaderView.SONY_DPAD_DOWN_SCANCODE, ReaderAction.NORMAL, ReaderAction.PAGE_DOWN),
						key(ReaderView.SONY_DPAD_UP_SCANCODE, ReaderAction.NORMAL, ReaderAction.PAGE_UP),
						key(ReaderView.SONY_DPAD_DOWN_SCANCODE, ReaderAction.LONG, ReaderAction.PAGE_DOWN_10),
						key(ReaderView.SONY_DPAD_UP_SCANCODE, ReaderAction.LONG, ReaderAction.PAGE_UP_10),
						key(KeyEvent.KEYCODE_8, ReaderAction.NORMAL, ReaderAction.PAGE_DOWN),
						key(KeyEvent.KEYCODE_2, ReaderAction.NORMAL, ReaderAction.PAGE_UP),
						key(KeyEvent.KEYCODE_8, ReaderAction.LONG, ReaderAction.PAGE_DOWN_10),
						key(KeyEvent.KEYCODE_2, ReaderAction.LONG, ReaderAction.PAGE_UP_10),
						key(ReaderView.KEYCODE_ESCAPE, ReaderAction.NORMAL, ReaderAction.PAGE_DOWN),
						key(ReaderView.KEYCODE_ESCAPE, ReaderAction.LONG, ReaderAction.REPEAT)),
				Arrays.asList(
						key(ReaderView.NOOK_KEY_NEXT_RIGHT, ReaderAction.NORMAL, ReaderAction.PAGE_DOWN),
						key(ReaderView.NOOK_KEY_SHIFT_DOWN, ReaderAction.NORMAL, ReaderAction.PAGE_DOWN),
						key(ReaderView.NOOK_KEY_PREV_LEFT, ReaderAction.NORMAL, ReaderAction.PAGE_UP),
						key(ReaderView.NOOK_KEY_PREV_RIGHT, ReaderAction.NORMAL, ReaderAction.PAGE_UP),
						key(ReaderView.NOOK_KEY_SHIFT_UP, ReaderAction.NORMAL, ReaderAction.PAGE_UP),
						key(
								ReaderView.NOOK_12_KEY_NEXT_LEFT,
								ReaderAction.NORMAL,
								einkNook ? ReaderAction.PAGE_UP : ReaderAction.PAGE_DOWN),
						key(
								ReaderView.NOOK_12_KEY_NEXT_LEFT,
								ReaderAction.LONG,
								einkNook ? ReaderAction.PAGE_UP_10 : ReaderAction.PAGE_DOWN_10),
						key(ReaderView.KEYCODE_PAGE_BOTTOMLEFT, ReaderAction.NORMAL, ReaderAction.PAGE_UP),
						key(ReaderView.KEYCODE_PAGE_TOPLEFT, ReaderAction.NORMAL, ReaderAction.PAGE_DOWN),
						key(ReaderView.KEYCODE_PAGE_TOPRIGHT, ReaderAction.NORMAL, ReaderAction.PAGE_DOWN),
						key(ReaderView.KEYCODE_PAGE_BOTTOMLEFT, ReaderAction.LONG, ReaderAction.PAGE_UP_10),
						key(ReaderView.KEYCODE_PAGE_TOPLEFT, ReaderAction.LONG, ReaderAction.PAGE_DOWN_10),
						key(ReaderView.KEYCODE_PAGE_TOPRIGHT, ReaderAction.LONG, ReaderAction.PAGE_DOWN_10)),
				Arrays.asList(
						tap(1, false, ReaderAction.PAGE_UP),
						tap(2, false, ReaderAction.PAGE_UP),
						tap(4, false, ReaderAction.PAGE_UP),
						tap(1, true, ReaderAction.GO_BACK),
						tap(2, true, ReaderAction.TOGGLE_DAY_NIGHT),
						tap(4, true, ReaderAction.PAGE_UP_10),
						tap(3, false, ReaderAction.PAGE_DOWN),
						tap(6, false, ReaderAction.PAGE_DOWN),
						tap(7, false, ReaderAction.PAGE_DOWN),
						tap(8, false, ReaderAction.PAGE_DOWN),
						tap(9, false, ReaderAction.PAGE_DOWN),
						tap(3, true, ReaderAction.TOGGLE_AUTOSCROLL),
						tap(6, true, ReaderAction.PAGE_DOWN_10),
						tap(7, true, ReaderAction.PAGE_DOWN_10),
						tap(8, true, ReaderAction.PAGE_DOWN_10),
						tap(9, true, ReaderAction.PAGE_DOWN_10),
						tap(5, false, ReaderAction.READER_MENU),
						tap(5, true, ReaderAction.OPTIONS)));
	}

	boolean applyTo(
			java.util.Properties properties,
			boolean includeNookKeys,
			boolean hasHardwareMenuKey,
			boolean toolbarEnabled) {
		Set<String> userNookMappings = new HashSet<>();
		if (includeNookKeys) {
			for (KeyAction action : nookKeyActions) {
				if (properties.getProperty(action.propertyName) != null)
					userNookMappings.add(action.propertyName);
			}
		}
		for (KeyAction action : keyActions)
			applyDefault(properties, action.propertyName, action.actionId);
		if (includeNookKeys) {
			for (KeyAction action : nookKeyActions) {
				if (!userNookMappings.contains(action.propertyName))
					properties.setProperty(
							action.propertyName, action.actionId);
			}
		}

		boolean menuTapFound = hasMenuTap(properties);
		boolean menuKeyFound = hasMenuKeyMapping();
		boolean forcedCenterMenu = false;
		for (TapAction action : tapActions) {
			boolean force = action.zone == 5
					&& !action.longPress
					&& !menuTapFound
					&& !(hasHardwareMenuKey && menuKeyFound)
					&& !toolbarEnabled;
			if (force) {
				properties.setProperty(action.propertyName, action.actionId);
				forcedCenterMenu = true;
			} else {
				applyDefault(
						properties, action.propertyName, action.actionId);
			}
		}
		return forcedCenterMenu;
	}

	boolean hasAvailableMenuKey(boolean hasHardwareMenuKey) {
		for (KeyAction action : keyActions) {
			if (ReaderAction.READER_MENU.id.equals(action.actionId)
					&& (action.keyCode != KeyEvent.KEYCODE_MENU
							|| hasHardwareMenuKey))
				return true;
		}
		return false;
	}

	boolean hasMenuTap(java.util.Properties properties) {
		for (TapAction action : tapActions) {
			if (ReaderAction.READER_MENU.id.equals(
					properties.getProperty(action.propertyName)))
				return true;
		}
		return false;
	}

	private boolean hasMenuKeyMapping() {
		for (KeyAction action : keyActions) {
			if (ReaderAction.READER_MENU.id.equals(action.actionId))
				return true;
		}
		return false;
	}

	private static void applyDefault(
			java.util.Properties properties,
			String name,
			String value) {
		if (properties.getProperty(name) == null)
			properties.setProperty(name, value);
	}

	private static KeyAction key(
			int keyCode, int type, ReaderAction action) {
		return new KeyAction(
				keyCode,
				ReaderAction.getKeyProp(keyCode, type),
				action.id);
	}

	private static TapAction tap(
			int zone, boolean longPress, ReaderAction action) {
		int type = longPress ? ReaderAction.LONG : ReaderAction.NORMAL;
		return new TapAction(
				zone,
				longPress,
				ReaderAction.getTapZoneProp(zone, type),
				action.id);
	}

	private static final class KeyAction {
		private final int keyCode;
		private final String propertyName;
		private final String actionId;

		private KeyAction(
				int keyCode, String propertyName, String actionId) {
			this.keyCode = keyCode;
			this.propertyName = propertyName;
			this.actionId = actionId;
		}
	}

	private static final class TapAction {
		private final int zone;
		private final boolean longPress;
		private final String propertyName;
		private final String actionId;

		private TapAction(
				int zone,
				boolean longPress,
				String propertyName,
				String actionId) {
			this.zone = zone;
			this.longPress = longPress;
			this.propertyName = propertyName;
			this.actionId = actionId;
		}
	}
}
