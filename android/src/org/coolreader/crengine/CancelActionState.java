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
 * Owns a replaceable cancel Runnable for navigation/dialog sessions.
 *
 * Set/take share one synchronized slot so concurrent cancel cannot run a
 * stale predecessor. Close permanently drops the action.
 */
final class CancelActionState {
	private Runnable cancelAction;
	private boolean closed;

	synchronized void set(Runnable action) {
		if (closed)
			return;
		cancelAction = action;
	}

	synchronized Runnable get() {
		return closed ? null : cancelAction;
	}

	/**
	 * Clears and returns the current cancel action for invocation.
	 */
	synchronized Runnable take() {
		if (closed)
			return null;
		Runnable previous = cancelAction;
		cancelAction = null;
		return previous;
	}

	synchronized void clear() {
		if (!closed)
			cancelAction = null;
	}

	synchronized boolean close() {
		if (closed)
			return false;
		closed = true;
		cancelAction = null;
		return true;
	}

	synchronized boolean isClosed() {
		return closed;
	}
}
