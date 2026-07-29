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

final class ViewportResizeState {
	private static final int FALLBACK_SIZE = 80;

	private volatile Size size;
	private Request current;
	private boolean closed;

	ViewportResizeState(int width, int height) {
		size = normalizedSize(width, height);
	}

	synchronized Request request(int width, int height) {
		if (closed)
			return null;
		Size requested = normalizedSize(width, height);
		size = requested;
		current = new Request(requested);
		return current;
	}

	synchronized Request requestCurrent() {
		if (closed)
			return null;
		current = new Request(size);
		return current;
	}

	Size size() {
		return size;
	}

	synchronized boolean isCurrent(Request request) {
		return !closed && request != null && current == request;
	}

	synchronized boolean complete(Request request) {
		if (!isCurrent(request))
			return false;
		current = null;
		return true;
	}

	synchronized boolean close() {
		if (closed)
			return false;
		closed = true;
		current = null;
		return true;
	}

	private static Size normalizedSize(int width, int height) {
		return new Size(
				width > 0 ? width : FALLBACK_SIZE,
				height > 0 ? height : FALLBACK_SIZE);
	}

	static final class Request {
		private final Size size;

		private Request(Size size) {
			this.size = size;
		}

		Size size() {
			return size;
		}
	}

	static final class Size {
		private final int width;
		private final int height;

		private Size(int width, int height) {
			this.width = width;
			this.height = height;
		}

		int width() {
			return width;
		}

		int height() {
			return height;
		}
	}
}
