/*
 * CoolReader for Android
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 2
 * of the License, or (at your option) any later version.
 */

package org.coolreader.crengine;

/**
 * Owns one reader surface generation and its delayed focus refresh.
 */
final class ReaderSurfaceState {
	private boolean surfaceCreated;
	private boolean windowVisible;
	private boolean windowFocused;
	private boolean closed;
	private FocusRefresh currentFocusRefresh;

	synchronized boolean markSurfaceCreated() {
		if (closed || surfaceCreated)
			return false;
		surfaceCreated = true;
		if (!windowVisible)
			return false;
		currentFocusRefresh = null;
		return true;
	}

	synchronized void markSurfaceDestroyed() {
		surfaceCreated = false;
		currentFocusRefresh = null;
	}

	synchronized boolean changeVisibility(boolean visible) {
		if (closed || windowVisible == visible)
			return false;
		windowVisible = visible;
		if (!visible) {
			currentFocusRefresh = null;
			return false;
		}
		if (!surfaceCreated)
			return false;
		currentFocusRefresh = null;
		return true;
	}

	synchronized FocusRefresh changeFocus(boolean focused) {
		if (closed || windowFocused == focused)
			return null;
		windowFocused = focused;
		currentFocusRefresh = null;
		if (!focused)
			return null;
		currentFocusRefresh = new FocusRefresh();
		return currentFocusRefresh;
	}

	synchronized boolean claimFocusRefresh(FocusRefresh refresh) {
		if (closed
				|| refresh == null
				|| currentFocusRefresh != refresh)
			return false;
		currentFocusRefresh = null;
		return surfaceCreated && windowVisible && windowFocused;
	}

	synchronized boolean isDrawable() {
		return !closed && surfaceCreated;
	}

	synchronized boolean isClosed() {
		return closed;
	}

	synchronized boolean close() {
		if (closed)
			return false;
		closed = true;
		surfaceCreated = false;
		windowVisible = false;
		windowFocused = false;
		currentFocusRefresh = null;
		return true;
	}

	static final class FocusRefresh {
		private FocusRefresh() {
		}
	}
}
