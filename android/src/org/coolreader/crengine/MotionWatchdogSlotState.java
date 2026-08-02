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

import com.s_trace.motion_watchdog.MotionWatchdogHandler;

/**
 * Owns the replaceable MotionWatchdogHandler slot for a TTS toolbar.
 *
 * Install/take share one synchronized slot so concurrent start/stop cannot
 * leave two live watchdogs or stop a replacement. Close permanently drops
 * the slot.
 */
final class MotionWatchdogSlotState {
	private MotionWatchdogHandler watchdog;
	private boolean closed;

	/**
	 * Installs {@code next} and returns the previous handler for stop.
	 * When closed, does not retain {@code next} and returns {@code next}
	 * so the caller can close the unused handler.
	 */
	synchronized MotionWatchdogHandler install(
			MotionWatchdogHandler next) {
		if (closed)
			return next;
		MotionWatchdogHandler previous = watchdog;
		watchdog = next;
		return previous;
	}

	/**
	 * Clears and returns the current handler for stop.
	 */
	synchronized MotionWatchdogHandler take() {
		if (closed)
			return null;
		MotionWatchdogHandler previous = watchdog;
		watchdog = null;
		return previous;
	}

	synchronized MotionWatchdogHandler get() {
		return closed ? null : watchdog;
	}

	synchronized boolean close() {
		if (closed)
			return false;
		closed = true;
		watchdog = null;
		return true;
	}

	synchronized boolean isClosed() {
		return closed;
	}
}
