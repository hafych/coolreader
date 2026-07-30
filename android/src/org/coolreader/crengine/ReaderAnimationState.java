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
 * Owns the exact active reader animation and its coalesced pointer update.
 *
 * A pointer update may arrive before the background thread finishes creating
 * the animation. Keeping both references in one owner lets that update wait
 * for publication while ensuring that finishing or resetting an animation
 * also invalidates work queued for the old session.
 */
final class ReaderAnimationState<A, U> {
	private A current;
	private U pendingUpdate;
	private boolean closed;

	synchronized A current() {
		return closed ? null : current;
	}

	synchronized boolean installIfIdle(A candidate) {
		if (closed || candidate == null || current != null)
			return false;
		current = candidate;
		return true;
	}

	synchronized boolean isCurrent(A animation) {
		return !closed
				&& animation != null
				&& current == animation;
	}

	synchronized boolean installPendingUpdate(U update) {
		if (closed || update == null || pendingUpdate != null)
			return false;
		pendingUpdate = update;
		return true;
	}

	synchronized U pendingUpdate() {
		return closed ? null : pendingUpdate;
	}

	synchronized boolean isPendingUpdate(U update) {
		return !closed
				&& update != null
				&& pendingUpdate == update;
	}

	synchronized boolean clearPendingUpdate(U update) {
		if (!isPendingUpdate(update))
			return false;
		pendingUpdate = null;
		return true;
	}

	synchronized boolean finish(A animation) {
		if (!isCurrent(animation))
			return false;
		current = null;
		pendingUpdate = null;
		return true;
	}

	synchronized A reset() {
		if (closed)
			return null;
		A stopped = current;
		current = null;
		pendingUpdate = null;
		return stopped;
	}

	synchronized A close() {
		if (closed)
			return null;
		closed = true;
		A stopped = current;
		current = null;
		pendingUpdate = null;
		return stopped;
	}
}
