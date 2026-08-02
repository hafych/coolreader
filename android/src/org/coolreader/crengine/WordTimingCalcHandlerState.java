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

import android.os.Handler;
import android.os.HandlerThread;

/**
 * Owns the lazy word-timing HandlerThread/Handler pair for TTS audiobook
 * work.
 *
 * Ensure/get share one synchronized slot so concurrent sentence posts cannot
 * install two worker threads. Take-running clears the slot for stop paths
 * that still need to quit the previous thread. Close permanently blocks
 * reinstall.
 */
final class WordTimingCalcHandlerState {
	private HandlerThread thread;
	private Handler handler;
	private boolean closed;

	/**
	 * Returns the existing handler or installs the created pair once.
	 * Callers that lose the install race must quit their unused
	 * {@code createdThread} when the returned handler is not
	 * {@code createdHandler}.
	 */
	synchronized Handler ensure(
			HandlerThread createdThread, Handler createdHandler) {
		if (closed)
			return null;
		if (handler == null) {
			if (createdThread == null || createdHandler == null)
				throw new IllegalArgumentException(
						"word-timing handler pair must not be null");
			thread = createdThread;
			handler = createdHandler;
		}
		return handler;
	}

	synchronized Handler get() {
		return closed ? null : handler;
	}

	/**
	 * Clears the slot and returns the previous pair for callback drain/quit.
	 */
	synchronized Running takeRunning() {
		if (closed)
			return null;
		Running previous = (handler == null && thread == null)
				? null
				: new Running(thread, handler);
		thread = null;
		handler = null;
		return previous;
	}

	synchronized boolean close() {
		if (closed)
			return false;
		closed = true;
		thread = null;
		handler = null;
		return true;
	}

	synchronized boolean isClosed() {
		return closed;
	}

	static final class Running {
		final HandlerThread thread;
		final Handler handler;

		Running(HandlerThread thread, Handler handler) {
			this.thread = thread;
			this.handler = handler;
		}
	}
}
