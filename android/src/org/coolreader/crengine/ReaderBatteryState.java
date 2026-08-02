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
 * Owns one immutable battery snapshot for the reader.
 *
 * Updates publish a complete {@link BatteryStatus} so the native renderer
 * never observes mixed generation of state/connection/level. Destroy closes
 * the owner permanently so late broadcasts cannot publish into a torn-down
 * reader.
 */
final class ReaderBatteryState {
	static final class Change {
		private final BatteryStatus previous;
		private final BatteryStatus current;

		private Change(
				BatteryStatus previous,
				BatteryStatus current) {
			this.previous = previous;
			this.current = current;
		}

		BatteryStatus previous() {
			return previous;
		}

		BatteryStatus current() {
			return current;
		}

		boolean stateChanged() {
			return previous.getState() != current.getState();
		}

		boolean connectionChanged() {
			return previous.getChargingConnection()
					!= current.getChargingConnection();
		}

		boolean levelChanged() {
			return previous.getChargeLevel()
					!= current.getChargeLevel();
		}
	}

	private volatile BatteryStatus status;
	private boolean closed;

	ReaderBatteryState(BatteryStatus initial) {
		if (initial == null)
			throw new IllegalArgumentException(
					"status must not be null");
		status = initial;
	}

	BatteryStatus snapshot() {
		return status;
	}

	/**
	 * Publishes a new snapshot when it differs from the current one.
	 * Returns null when closed, when {@code next} equals the current
	 * snapshot, or when {@code next} is null (rejected before mutation).
	 */
	synchronized Change update(BatteryStatus next) {
		if (closed)
			return null;
		if (next == null)
			throw new IllegalArgumentException(
					"status must not be null");
		BatteryStatus previous = status;
		if (next.equals(previous))
			return null;
		status = next;
		return new Change(previous, next);
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
