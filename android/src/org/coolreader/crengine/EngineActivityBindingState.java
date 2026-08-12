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

import java.lang.ref.WeakReference;

/**
 * Owns the Engine {@link BaseActivity} binding for one service generation.
 *
 * Attach, identity checks and detach share one synchronized weak reference so
 * progress/fatal-error paths cannot observe a half-cleared Activity. Close
 * permanently drops the binding after the matching Activity detaches.
 */
final class EngineActivityBindingState {
	private WeakReference<BaseActivity> activityRef;
	private boolean closed;

	/**
	 * Publishes a live Activity weak reference. Rejected after close or when
	 * activity is null.
	 */
	synchronized boolean attach(BaseActivity activity) {
		if (closed || activity == null)
			return false;
		activityRef = new WeakReference<>(activity);
		return true;
	}

	synchronized BaseActivity get() {
		if (closed || activityRef == null)
			return null;
		return activityRef.get();
	}

	synchronized boolean isAttachedTo(BaseActivity activity) {
		return activity != null && get() == activity;
	}

	/**
	 * Clears the binding only when the given Activity is the current owner.
	 * Returns true when this generation was detached.
	 */
	synchronized boolean clearIf(BaseActivity activity) {
		if (closed || activity == null)
			return false;
		BaseActivity attached = get();
		if (attached != activity)
			return false;
		if (activityRef != null) {
			activityRef.clear();
			activityRef = null;
		}
		return true;
	}

	/**
	 * Permanently closes the binding and drops any remaining weak reference.
	 */
	synchronized boolean close() {
		if (closed)
			return false;
		closed = true;
		if (activityRef != null) {
			activityRef.clear();
			activityRef = null;
		}
		return true;
	}

	synchronized boolean isClosed() {
		return closed;
	}
}
