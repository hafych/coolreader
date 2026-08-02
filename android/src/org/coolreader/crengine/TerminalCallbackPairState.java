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
 * Owns a success/failure callback pair for one terminal session.
 *
 * Set/take/clear share one synchronized slot so concurrent complete and
 * cancel cannot deliver both outcomes or a stale predecessor. Close
 * permanently drops both callbacks.
 */
final class TerminalCallbackPairState {
	private Runnable successCallback;
	private Runnable failureCallback;
	private boolean closed;

	synchronized void set(Runnable success, Runnable failure) {
		if (closed)
			return;
		successCallback = success;
		failureCallback = failure;
	}

	/**
	 * Snapshot both callbacks and clear the slot.
	 */
	synchronized Snapshot takeBoth() {
		if (closed)
			return new Snapshot(null, null);
		Snapshot snapshot = new Snapshot(successCallback, failureCallback);
		successCallback = null;
		failureCallback = null;
		return snapshot;
	}

	/**
	 * Clears both and returns only the failure callback (cancel path).
	 */
	synchronized Runnable takeFailure() {
		if (closed)
			return null;
		Runnable failure = failureCallback;
		successCallback = null;
		failureCallback = null;
		return failure;
	}

	synchronized void clear() {
		if (closed)
			return;
		successCallback = null;
		failureCallback = null;
	}

	synchronized boolean close() {
		if (closed)
			return false;
		closed = true;
		successCallback = null;
		failureCallback = null;
		return true;
	}

	synchronized boolean isClosed() {
		return closed;
	}

	static final class Snapshot {
		final Runnable success;
		final Runnable failure;

		Snapshot(Runnable success, Runnable failure) {
			this.success = success;
			this.failure = failure;
		}
	}
}
