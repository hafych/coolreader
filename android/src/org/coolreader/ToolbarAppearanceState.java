/*
 * CoolReader for Android
 * Copyright (C) 2026 CoolReader Next contributors
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 2
 * of the License, or (at your option) any later version.
 */

package org.coolreader;

/**
 * Owns the Activity toolbar appearance id.
 *
 * Settings apply and UI queries share one synchronized value so a concurrent
 * settings update cannot leave a half-published appearance id. Destroy closes
 * the owner permanently.
 */
final class ToolbarAppearanceState {
	private String appearanceId = "0";
	private boolean closed;

	synchronized void set(String id) {
		if (closed)
			return;
		if (id == null || id.length() == 0)
			id = "0";
		appearanceId = id;
	}

	synchronized String get() {
		return closed ? "0" : appearanceId;
	}

	synchronized boolean close() {
		if (closed)
			return false;
		closed = true;
		return true;
	}

	synchronized boolean isClosed() {
		return closed;
	}
}
