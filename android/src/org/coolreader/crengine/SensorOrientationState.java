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
 * Owns the latest sensor-derived orientation (0 portrait / 1 landscape).
 *
 * Configuration callbacks publish through one synchronized owner. Destroy
 * permanently closes the owner.
 */
final class SensorOrientationState {
	private int orientationFromSensor;
	private boolean closed;

	synchronized void set(int orientationFromSensor) {
		if (closed)
			return;
		this.orientationFromSensor = orientationFromSensor;
	}

	synchronized int get() {
		return orientationFromSensor;
	}

	synchronized boolean close() {
		if (closed)
			return false;
		closed = true;
		orientationFromSensor = 0;
		return true;
	}

	synchronized boolean isClosed() {
		return closed;
	}
}
