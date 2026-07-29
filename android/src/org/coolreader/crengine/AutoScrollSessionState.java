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

final class AutoScrollSessionState<T> {
	private T current;
	private boolean ready;
	private boolean initialized;
	private boolean closed;

	synchronized boolean requestStart(T session) {
		if (session == null)
			throw new IllegalArgumentException("session is required");
		if (closed || current != null)
			return false;
		current = session;
		ready = false;
		initialized = false;
		return true;
	}

	synchronized boolean beginInitialization(T session) {
		if (closed || session == null || current != session)
			return false;
		ready = false;
		return true;
	}

	synchronized boolean markReady(T session) {
		if (closed || session == null || current != session)
			return false;
		ready = true;
		initialized = true;
		return true;
	}

	synchronized boolean isCurrent(T session) {
		return !closed && session != null && current == session;
	}

	synchronized boolean isReady(T session) {
		return !closed && ready
				&& session != null && current == session;
	}

	synchronized boolean isInitialized(T session) {
		return !closed && initialized
				&& session != null && current == session;
	}

	synchronized boolean isActive() {
		return !closed && current != null;
	}

	synchronized T currentSession() {
		return closed ? null : current;
	}

	synchronized T readySession() {
		return !closed && ready ? current : null;
	}

	synchronized boolean stop(T session) {
		if (closed || session == null || current != session)
			return false;
		current = null;
		ready = false;
		initialized = false;
		return true;
	}

	synchronized T stopCurrent() {
		if (closed)
			return null;
		T stopped = current;
		current = null;
		ready = false;
		initialized = false;
		return stopped;
	}

	synchronized T close() {
		if (closed)
			return null;
		closed = true;
		T stopped = current;
		current = null;
		ready = false;
		initialized = false;
		return stopped;
	}
}
