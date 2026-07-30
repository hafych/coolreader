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

final class AutoScrollSpeedState {
	static final int DEFAULT_SPEED = 1500;
	static final int MIN_SPEED = 200;
	static final int MAX_SPEED = 10000;

	private volatile int speed = DEFAULT_SPEED;

	int speed() {
		return speed;
	}

	synchronized int configure(int requestedSpeed) {
		speed = clamp(requestedSpeed);
		return speed;
	}

	synchronized int change(int delta) {
		long adjustedDelta =
				(long) delta * multiplier(speed);
		speed = clamp((long) speed + adjustedDelta);
		return speed;
	}

	private static int multiplier(int speed) {
		if (speed < 300)
			return 10;
		if (speed < 500)
			return 20;
		if (speed < 1000)
			return 40;
		if (speed < 2000)
			return 80;
		if (speed < 5000)
			return 200;
		return 300;
	}

	private static int clamp(long value) {
		if (value < MIN_SPEED)
			return MIN_SPEED;
		if (value > MAX_SPEED)
			return MAX_SPEED;
		return (int) value;
	}
}
