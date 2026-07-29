/*
 * CoolReader for Android
 * Copyright (C) 2026 CoolReader Next contributors
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 2
 * of the License, or (at your option) any later version.
 */

package com.s_trace.motion_watchdog;

final class MotionWatchdogFadeState {
	private final int originalVolume;
	private int currentVolume;

	MotionWatchdogFadeState(int observedVolume) {
		originalVolume = Math.max(0, observedVolume);
		currentVolume = originalVolume;
	}

	int originalVolume() {
		return originalVolume;
	}

	int currentVolume() {
		return currentVolume;
	}

	boolean isSilent() {
		return currentVolume == 0;
	}

	int step() {
		if (currentVolume > 0)
			currentVolume--;
		return currentVolume;
	}
}
