/*
 * CoolReader for Android
 * Copyright (C) 2026 CoolReader Next contributors
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 2
 * of the License, or (at your option) any later version.
 */

package org.coolreader;

final class ActivityFrameState<T> {
	private T current;
	private T previous;
	private boolean closed;

	synchronized boolean moveTo(T next) {
		if (closed || next == null || current == next)
			return false;
		previous = current;
		current = next;
		return true;
	}

	synchronized boolean isCurrent(T candidate) {
		return current == candidate;
	}

	synchronized T previous() {
		return previous;
	}

	synchronized boolean isPrevious(T candidate) {
		return previous != null && previous == candidate;
	}

	synchronized boolean close() {
		if (closed)
			return false;
		closed = true;
		current = null;
		previous = null;
		return true;
	}
}
