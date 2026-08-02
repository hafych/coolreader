/*
 * CoolReader for Android
 * Copyright (C) 2026 CoolReader Next contributors
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 2
 * of the License, or (at your option) any later version.
 */

package org.coolreader.tts;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Owns TTS service binding registration, published binder and pending
 * connect callbacks.
 *
 * Bind/unbind/connect share one synchronized owner so a concurrent unbind
 * cannot leave a half-published binder or leak pending callbacks. Close
 * permanently clears all state.
 */
final class TtsBindingSessionState {
	private final ArrayList<TTSControlBinder.Callback> pendingCallbacks =
			new ArrayList<>();
	private TTSControlBinder binder;
	private boolean bindingRegistered;
	private boolean closed;

	synchronized TTSControlBinder getBinder() {
		return closed ? null : binder;
	}

	synchronized boolean isBindingRegistered() {
		return !closed && bindingRegistered;
	}

	/**
	 * Marks binding as registered. Returns false if already registered or
	 * closed.
	 */
	synchronized boolean beginBinding() {
		if (closed || bindingRegistered)
			return false;
		bindingRegistered = true;
		return true;
	}

	synchronized void setBinder(TTSControlBinder binder) {
		if (closed)
			return;
		this.binder = binder;
	}

	synchronized void clearBinder() {
		binder = null;
	}

	synchronized void addPending(TTSControlBinder.Callback callback) {
		if (closed || callback == null)
			return;
		pendingCallbacks.add(callback);
	}

	/**
	 * Clears pending callbacks and returns a snapshot for delivery.
	 */
	synchronized List<TTSControlBinder.Callback> takePending() {
		if (closed) {
			pendingCallbacks.clear();
			return Collections.emptyList();
		}
		List<TTSControlBinder.Callback> out =
				new ArrayList<>(pendingCallbacks);
		pendingCallbacks.clear();
		return out;
	}

	synchronized void clearPending() {
		pendingCallbacks.clear();
	}

	/**
	 * Unbind path: returns whether a platform unbind is required and clears
	 * registration, binder and pending callbacks.
	 */
	synchronized boolean unbind() {
		if (closed)
			return false;
		boolean shouldUnbind = bindingRegistered;
		bindingRegistered = false;
		binder = null;
		pendingCallbacks.clear();
		return shouldUnbind;
	}

	/**
	 * Bind failure: clears registration and pending callbacks.
	 */
	synchronized void bindingFailed() {
		if (closed)
			return;
		bindingRegistered = false;
		pendingCallbacks.clear();
	}

	synchronized boolean close() {
		if (closed)
			return false;
		closed = true;
		bindingRegistered = false;
		binder = null;
		pendingCallbacks.clear();
		return true;
	}

	synchronized boolean isClosed() {
		return closed;
	}
}
