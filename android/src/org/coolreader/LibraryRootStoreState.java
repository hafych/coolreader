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

import org.coolreader.crengine.LibraryRootStore;

/**
 * Owns the Activity-scoped SAF library root store.
 *
 * The store is installed once during onCreate. Readers and mutators go through
 * this owner so destroy can permanently close access and prevent late root
 * updates into a torn-down Activity.
 */
final class LibraryRootStoreState {
	private LibraryRootStore store;
	private boolean closed;

	synchronized boolean install(LibraryRootStore candidate) {
		if (closed || candidate == null || store != null)
			return false;
		store = candidate;
		return true;
	}

	synchronized LibraryRootStore get() {
		return closed ? null : store;
	}

	synchronized LibraryRootStore close() {
		if (closed)
			return null;
		closed = true;
		LibraryRootStore previous = store;
		store = null;
		return previous;
	}

	synchronized boolean isClosed() {
		return closed;
	}
}
