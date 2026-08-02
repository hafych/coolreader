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
 * Owns one-shot CoverpageManager listener registration for a root view.
 *
 * Begin-register returns true only once while open so theme recreation
 * cannot accumulate listeners. Close permanently blocks further registration.
 */
final class CoverListenerRegistrationState {
	private boolean registered;
	private boolean closed;

	/**
	 * @return true when this caller should perform the platform register
	 */
	synchronized boolean beginRegister() {
		if (closed || registered)
			return false;
		registered = true;
		return true;
	}

	/**
	 * @return true when this caller should perform the platform unregister
	 */
	synchronized boolean beginUnregister() {
		if (closed || !registered)
			return false;
		registered = false;
		return true;
	}

	synchronized boolean isRegistered() {
		return !closed && registered;
	}

	synchronized boolean close() {
		if (closed)
			return false;
		closed = true;
		registered = false;
		return true;
	}

	synchronized boolean isClosed() {
		return closed;
	}
}
