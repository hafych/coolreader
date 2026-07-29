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
 * Owns creation, initialization and destruction of one native DocView.
 */
final class ReaderNativeLifecycle {
	private boolean createClaimed;
	private boolean created;
	private boolean initialized;
	private boolean closed;
	private boolean destroyed;

	synchronized boolean claimCreate() {
		if (closed || createClaimed || created || destroyed)
			return false;
		createClaimed = true;
		return true;
	}

	synchronized boolean markCreated() {
		if (!createClaimed || created || destroyed)
			return false;
		created = true;
		return !closed;
	}

	synchronized boolean isActive() {
		return created && !closed && !destroyed;
	}

	synchronized boolean markInitialized() {
		if (!isActive() || initialized)
			return false;
		initialized = true;
		return true;
	}

	synchronized boolean isInitialized() {
		return initialized && !closed && !destroyed;
	}

	synchronized boolean isClosed() {
		return closed;
	}

	synchronized boolean close() {
		if (closed)
			return false;
		closed = true;
		initialized = false;
		return true;
	}

	synchronized boolean claimDestroy() {
		if (!closed || !created || destroyed)
			return false;
		destroyed = true;
		return true;
	}
}
