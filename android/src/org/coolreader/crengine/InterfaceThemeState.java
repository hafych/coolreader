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

/**
 * Owns the Activity interface theme and themed action-icon snapshot.
 *
 * Theme selection and icon rebuild share one synchronized owner so a
 * concurrent settings apply cannot leave a half-published theme/icon pair.
 * Destroy permanently closes the owner.
 */
final class InterfaceThemeState {
	private InterfaceTheme theme;
	private ActionIconSet actionIcons = ActionIconSet.empty();
	private boolean closed;

	synchronized void setTheme(InterfaceTheme next) {
		if (closed)
			return;
		theme = next;
	}

	synchronized InterfaceTheme getTheme() {
		return closed ? null : theme;
	}

	synchronized void setActionIcons(ActionIconSet icons) {
		if (closed)
			return;
		actionIcons = icons != null ? icons : ActionIconSet.empty();
	}

	synchronized ActionIconSet getActionIcons() {
		return closed ? ActionIconSet.empty() : actionIcons;
	}

	synchronized int iconFor(ReaderAction action) {
		return getActionIcons().iconFor(action);
	}

	synchronized boolean close() {
		if (closed)
			return false;
		closed = true;
		theme = null;
		actionIcons = ActionIconSet.empty();
		return true;
	}

	synchronized boolean isClosed() {
		return closed;
	}
}
