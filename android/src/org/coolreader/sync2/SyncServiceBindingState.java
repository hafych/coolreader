/*
 * CoolReader for Android
 * Copyright (C) 2026 CoolReader Next contributors
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 2
 * of the License, or (at your option) any later version.
 */

package org.coolreader.sync2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Owns SyncService bind state: binder, bound/bind-called flags and pending
 * connect callbacks.
 */
final class SyncServiceBindingState {
	private final ArrayList<SyncServiceBinder.Callback> pendingCallbacks =
			new ArrayList<>();
	private SyncServiceBinder binder;
	private boolean serviceBound;
	private boolean bindIsCalled;
	private boolean closed;

	synchronized SyncServiceBinder getBinder() {
		return closed ? null : binder;
	}

	synchronized boolean isServiceBound() {
		return !closed && serviceBound;
	}

	synchronized boolean isBindCalled() {
		return !closed && bindIsCalled;
	}

	synchronized boolean isReady() {
		return !closed && binder != null && serviceBound;
	}

	synchronized void setBinder(SyncServiceBinder binder) {
		if (closed)
			return;
		this.binder = binder;
	}

	synchronized void setServiceBound(boolean bound) {
		if (!closed)
			serviceBound = bound;
	}

	synchronized void setBindCalled(boolean called) {
		if (!closed)
			bindIsCalled = called;
	}

	synchronized void addPending(SyncServiceBinder.Callback callback) {
		if (closed || callback == null)
			return;
		pendingCallbacks.add(callback);
	}

	synchronized List<SyncServiceBinder.Callback> takePending() {
		if (closed) {
			pendingCallbacks.clear();
			return Collections.emptyList();
		}
		List<SyncServiceBinder.Callback> out =
				new ArrayList<>(pendingCallbacks);
		pendingCallbacks.clear();
		return out;
	}

	synchronized void unbind() {
		if (closed)
			return;
		serviceBound = false;
		bindIsCalled = false;
		binder = null;
		pendingCallbacks.clear();
	}

	synchronized void connectionLost() {
		if (closed)
			return;
		binder = null;
		serviceBound = false;
		bindIsCalled = false;
	}

	synchronized boolean close() {
		if (closed)
			return false;
		closed = true;
		unbind();
		return true;
	}

	synchronized boolean isClosed() {
		return closed;
	}
}
