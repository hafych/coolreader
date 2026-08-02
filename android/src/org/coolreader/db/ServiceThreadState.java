/*
 * CoolReader for Android
 * Copyright (C) 2026 CoolReader Next contributors
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 2
 * of the License, or (at your option) any later version.
 */

package org.coolreader.db;

import android.os.Handler;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Owns ServiceThread deferred queue, Handler publication and stopped flag.
 *
 * Post paths and looper startup share one synchronized owner so concurrent
 * posts before/after start cannot leave a mixed queue/handler generation.
 */
final class ServiceThreadState {
	private final LinkedList<Runnable> queue = new LinkedList<>();
	private Handler handler;
	private boolean stopped = true;
	private boolean closed;

	/**
	 * Queues a task when not running; otherwise returns false so the caller
	 * can post through the live handler after draining.
	 */
	synchronized boolean enqueueIfStopped(Runnable task) {
		if (closed)
			return true; // swallow
		if (handler == null || stopped) {
			queue.addLast(task);
			return true;
		}
		return false;
	}

	synchronized Handler getHandler() {
		return closed ? null : handler;
	}

	synchronized void setHandler(Handler handler) {
		if (closed)
			return;
		this.handler = handler;
	}

	synchronized void clearHandler() {
		handler = null;
	}

	synchronized boolean isStopped() {
		return closed || stopped;
	}

	synchronized void setStopped(boolean stopped) {
		if (!closed)
			this.stopped = stopped;
	}

	/**
	 * Drains pre-start queue for posting after handler creation.
	 */
	synchronized List<Runnable> drainQueue() {
		ArrayList<Runnable> drained = new ArrayList<>(queue);
		queue.clear();
		return drained;
	}

	synchronized int queueSize() {
		return queue.size();
	}

	synchronized boolean close() {
		if (closed)
			return false;
		closed = true;
		stopped = true;
		queue.clear();
		handler = null;
		return true;
	}

	synchronized boolean isClosed() {
		return closed;
	}
}
