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

final class TapHandlerState<T> {
	private T current;
	private boolean closed;

	synchronized T current() {
		return closed ? null : current;
	}

	synchronized T installIfAbsent(T candidate) {
		if (closed || candidate == null)
			return null;
		if (current == null)
			current = candidate;
		return current;
	}

	synchronized boolean isCurrent(T handler) {
		return !closed
				&& handler != null
				&& current == handler;
	}

	synchronized boolean replace(
			T expected,
			T replacement) {
		if (!isCurrent(expected)
				|| replacement == null
				|| replacement == expected)
			return false;
		current = replacement;
		return true;
	}

	synchronized boolean close() {
		if (closed)
			return false;
		closed = true;
		current = null;
		return true;
	}
}
