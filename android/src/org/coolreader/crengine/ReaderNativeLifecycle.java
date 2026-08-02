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
 *
 * The DocView identity is attached before native create. Close permanently
 * rejects new work while still allowing a later destroy of a created instance.
 * claimDestroy claims teardown; takeDoc transfers the attached DocView once.
 */
final class ReaderNativeLifecycle {
	private DocView doc;
	private boolean createClaimed;
	private boolean created;
	private boolean initialized;
	private boolean closed;
	private boolean destroyed;

	/**
	 * Attaches the exclusive DocView identity before native create.
	 */
	synchronized boolean attach(DocView candidate) {
		if (closed
				|| destroyed
				|| createClaimed
				|| created
				|| doc != null
				|| candidate == null)
			return false;
		doc = candidate;
		return true;
	}

	/**
	 * Returns the attached DocView while destroy has not taken it.
	 */
	synchronized DocView doc() {
		return doc;
	}

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

	/**
	 * Transfers the attached DocView after a successful claimDestroy.
	 * Returns null when destroy has not been claimed or the DocView was
	 * already taken.
	 */
	synchronized DocView takeDoc() {
		if (!destroyed)
			return null;
		DocView owned = doc;
		doc = null;
		return owned;
	}
}
