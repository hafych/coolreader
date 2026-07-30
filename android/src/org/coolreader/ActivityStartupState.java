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

final class ActivityStartupState {
	private boolean initialStartPending = true;
	private boolean interfaceReady;
	private boolean closed;

	synchronized boolean takeInitialStart() {
		if (closed || !initialStartPending)
			return false;
		initialStartPending = false;
		return true;
	}

	synchronized boolean markInterfaceReady() {
		if (closed || interfaceReady)
			return false;
		interfaceReady = true;
		return true;
	}

	synchronized boolean isInterfaceReady() {
		return !closed && interfaceReady;
	}

	synchronized boolean shouldValidateSettings() {
		return !closed
				&& !initialStartPending
				&& interfaceReady;
	}

	synchronized boolean close() {
		if (closed)
			return false;
		closed = true;
		initialStartPending = false;
		interfaceReady = false;
		return true;
	}
}
