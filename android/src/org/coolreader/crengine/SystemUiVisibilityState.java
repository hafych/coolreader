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
 * Owns Activity system-UI visibility cache and listener-install flag.
 *
 * Apply and platform callbacks share one synchronized owner so concurrent
 * fullscreen transitions cannot observe a mixed generation. Destroy
 * permanently closes the owner.
 */
final class SystemUiVisibilityState {
	private int lastVisibility = -1;
	private boolean listenerIsSet;
	private boolean closed;

	synchronized void setLastVisibility(int visibility) {
		if (closed)
			return;
		lastVisibility = visibility;
	}

	synchronized int getLastVisibility() {
		return lastVisibility;
	}

	synchronized boolean markListenerSet() {
		if (closed || listenerIsSet)
			return false;
		listenerIsSet = true;
		return true;
	}

	synchronized boolean isListenerSet() {
		return !closed && listenerIsSet;
	}

	/**
	 * Resets cached visibility so the next apply re-enforces flags
	 * (legacy fullscreen path used lastSystemUiVisibility = -1).
	 */
	synchronized void invalidateCache() {
		if (closed)
			return;
		lastVisibility = -1;
	}

	synchronized boolean close() {
		if (closed)
			return false;
		closed = true;
		lastVisibility = -1;
		listenerIsSet = false;
		return true;
	}

	synchronized boolean isClosed() {
		return closed;
	}
}
