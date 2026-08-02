/*
 * CoolReader for Android
 * Copyright (C) 2026 CoolReader Next contributors
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 2
 * of the License, or (at your option) any later version.
 */

package org.coolreader.plugins;

/**
 * Owns AsyncOperationControl finished/cancelled flags.
 *
 * Cancel and finished transitions are synchronized so concurrent callbacks
 * cannot leave a mixed active state.
 */
final class AsyncOperationState {
	private boolean finished;
	private boolean cancelled;
	private boolean closed;

	synchronized void cancel() {
		if (!closed)
			cancelled = true;
	}

	synchronized boolean isCancelled() {
		return cancelled;
	}

	synchronized void finished() {
		if (!closed)
			finished = true;
	}

	synchronized boolean isFinished() {
		return finished;
	}

	synchronized boolean isActive() {
		return !closed && !finished && !cancelled;
	}

	synchronized boolean close() {
		if (closed)
			return false;
		closed = true;
		finished = true;
		return true;
	}

	synchronized boolean isClosed() {
		return closed;
	}
}
