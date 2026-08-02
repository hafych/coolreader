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
 * Tracks whether an Activity has a live BroadcastReceiver registration.
 *
 * Resume registers and pause/destroy unregisters. The owner serializes the
 * registered flag so double-unregister and post-destroy register are no-ops
 * from the caller's perspective (caller still performs the platform call only
 * when this owner says so).
 */
final class BroadcastRegistrationState {
	private boolean registered;
	private boolean closed;

	/**
	 * Records a successful registerReceiver. Returns false when closed.
	 */
	synchronized boolean onRegistered() {
		if (closed)
			return false;
		registered = true;
		return true;
	}

	/**
	 * Returns true exactly once while registered so the caller can unregister.
	 */
	synchronized boolean beginUnregister() {
		if (!registered)
			return false;
		registered = false;
		return true;
	}

	synchronized boolean isRegistered() {
		return registered;
	}

	/**
	 * Permanently closes the owner. If still registered, returns true so the
	 * caller can perform a final unregister.
	 */
	synchronized boolean close() {
		if (closed)
			return false;
		closed = true;
		boolean needsUnregister = registered;
		registered = false;
		return needsUnregister;
	}

	synchronized boolean isClosed() {
		return closed;
	}
}
