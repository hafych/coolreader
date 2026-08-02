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
 * Owns Activity screen backlight brightness (cold/warm) and brightness-hack
 * error flag.
 *
 * GUI and E-Ink paths share one synchronized owner so concurrent updates
 * cannot leave a mixed cold/warm pair. Destroy permanently closes the owner.
 */
final class ScreenBrightnessState {
	private int coldLevel = -1;
	private int warmLevel = -1;
	private boolean brightnessHackError;
	private boolean closed;

	ScreenBrightnessState(boolean initialBrightnessHackError) {
		this.brightnessHackError = initialBrightnessHackError;
	}

	synchronized void setColdLevel(int level) {
		if (closed)
			return;
		coldLevel = level;
	}

	synchronized int getColdLevel() {
		return coldLevel;
	}

	synchronized void setWarmLevel(int level) {
		if (closed)
			return;
		warmLevel = level;
	}

	synchronized int getWarmLevel() {
		return warmLevel;
	}

	synchronized void setBrightnessHackError(boolean error) {
		if (closed)
			return;
		brightnessHackError = error;
	}

	synchronized boolean isBrightnessHackError() {
		return brightnessHackError;
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
