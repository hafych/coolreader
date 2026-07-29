/*
 * CoolReader for Android
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 2
 * of the License, or (at your option) any later version.
 */

package org.coolreader.crengine;

/**
 * Owns one replaceable task generation until its component is closed.
 */
final class CloseableTaskGate {
	private Token current;
	private boolean closed;

	synchronized Token replace() {
		if (closed)
			return null;
		current = new Token();
		return current;
	}

	synchronized void cancel() {
		current = null;
	}

	synchronized boolean complete(Token token) {
		if (!isActive(token))
			return false;
		current = null;
		return true;
	}

	synchronized boolean close() {
		if (closed)
			return false;
		closed = true;
		current = null;
		return true;
	}

	synchronized boolean isActive(Token token) {
		return !closed && token != null && current == token;
	}

	synchronized boolean isClosed() {
		return closed;
	}

	static final class Token {
		private Token() {
		}
	}
}
