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
 * Owns cover thumbnail width/height for one root view generation.
 */
final class CoverSizeState {
	private int width;
	private int height;
	private boolean closed;

	synchronized void set(int width, int height) {
		if (closed)
			return;
		this.width = width;
		this.height = height;
	}

	synchronized int getWidth() {
		return width;
	}

	synchronized int getHeight() {
		return height;
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
