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
 * Owns BaseActivity start/pause visibility flags.
 *
 * Lifecycle transitions publish both flags through one synchronized owner
 * so concurrent UI work cannot observe a mixed started/paused pair from
 * parallel mutable fields. Destroy permanently closes the owner.
 */
final class ActivityRunState {
	private boolean started;
	private boolean paused;
	private boolean closed;

	synchronized void onStart() {
		if (closed)
			return;
		started = true;
		paused = false;
	}

	synchronized void onStop() {
		if (closed)
			return;
		started = false;
	}

	synchronized void onPause() {
		if (closed)
			return;
		started = false;
		paused = true;
	}

	synchronized void onResume() {
		if (closed)
			return;
		paused = false;
		started = true;
	}

	synchronized boolean isStarted() {
		return !closed && started;
	}

	synchronized boolean isPaused() {
		return !closed && paused;
	}

	synchronized boolean close() {
		if (closed)
			return false;
		closed = true;
		started = false;
		paused = false;
		return true;
	}

	synchronized boolean isClosed() {
		return closed;
	}
}
