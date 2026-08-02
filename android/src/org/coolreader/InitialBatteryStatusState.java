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

import org.coolreader.crengine.BatteryStatus;

/**
 * Owns the Activity-held battery snapshot used before ReaderView exists.
 *
 * Broadcasts may publish an immutable status while the reader is still
 * creating. Reader setup reads one stable snapshot, and destroy closes the
 * owner so late broadcasts cannot publish into a torn-down Activity.
 */
final class InitialBatteryStatusState {
	private volatile BatteryStatus status =
			BatteryStatus.unavailable();
	private boolean closed;

	BatteryStatus get() {
		return status;
	}

	synchronized boolean set(BatteryStatus next) {
		if (closed)
			return false;
		if (next == null)
			throw new IllegalArgumentException(
					"status must not be null");
		status = next;
		return true;
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
