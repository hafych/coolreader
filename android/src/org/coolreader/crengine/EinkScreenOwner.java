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
 * Owns the Activity E-Ink screen controller for one generation.
 *
 * The controller is installed once during service startup and cleared on
 * destroy so late backlight or refresh work cannot revive a torn-down
 * Activity through a parallel raw field.
 */
final class EinkScreenOwner {
	private EinkScreen screen;
	private boolean closed;

	synchronized boolean install(EinkScreen next) {
		if (closed || next == null || screen != null)
			return false;
		screen = next;
		return true;
	}

	synchronized EinkScreen get() {
		return closed ? null : screen;
	}

	synchronized boolean close() {
		if (closed)
			return false;
		closed = true;
		screen = null;
		return true;
	}

	synchronized boolean isClosed() {
		return closed;
	}
}
