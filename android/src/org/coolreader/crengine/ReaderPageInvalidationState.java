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
 * Coalesces page-cache invalidation without losing a concurrent request.
 *
 * The Engine thread claims the exact identity currently requested. If another
 * thread invalidates while cache slots are being recycled, it installs a new
 * identity that remains pending for the next preparation pass.
 */
final class ReaderPageInvalidationState {
	private Object requested = new Object();
	private Object applied;
	private boolean closed;

	synchronized void invalidate() {
		if (!closed)
			requested = new Object();
	}

	synchronized boolean claim() {
		if (closed || applied == requested)
			return false;
		applied = requested;
		return true;
	}

	synchronized void close() {
		closed = true;
		applied = requested;
	}
}
