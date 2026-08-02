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
 * Owns Scanner scan-policy flags for one generation.
 *
 * Settings apply and scan paths share one synchronized pair so concurrent
 * updates cannot leave a mixed policy. Close freezes the last values for
 * pure late reads.
 */
final class ScannerScanOptionsState {
	private boolean dirScanEnabled = true;
	private boolean hideEmptyDirs = true;
	private boolean closed;

	synchronized void setDirScanEnabled(boolean enabled) {
		if (closed)
			return;
		dirScanEnabled = enabled;
	}

	synchronized boolean isDirScanEnabled() {
		return dirScanEnabled;
	}

	synchronized void setHideEmptyDirs(boolean hide) {
		if (closed)
			return;
		hideEmptyDirs = hide;
	}

	synchronized boolean isHideEmptyDirs() {
		return hideEmptyDirs;
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
