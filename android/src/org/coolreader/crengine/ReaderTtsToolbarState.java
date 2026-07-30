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
 * Owns the exact active TTS toolbar until its asynchronous close completes.
 *
 * Keeping a closing toolbar current prevents a replacement from starting
 * while the old binder/service shutdown is still in flight. Reader teardown
 * closes the owner permanently and releases its retained UI reference.
 */
final class ReaderTtsToolbarState<T> {
	private T current;
	private boolean closed;

	synchronized boolean startIfIdle(T toolbar) {
		if (closed || toolbar == null || current != null)
			return false;
		current = toolbar;
		return true;
	}

	synchronized T current() {
		return closed ? null : current;
	}

	synchronized boolean finish(T toolbar) {
		if (closed || toolbar == null || current != toolbar)
			return false;
		current = null;
		return true;
	}

	synchronized T close() {
		if (closed)
			return null;
		closed = true;
		T stopped = current;
		current = null;
		return stopped;
	}
}
