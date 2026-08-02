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
 * Owns the Scanner virtual root {@link FileInfo} for one generation.
 *
 * Install once at construction; get while open; close permanently drops the
 * root so late scan paths cannot revive a torn-down tree.
 */
final class ScannerRootState {
	private FileInfo root;
	private boolean closed;

	synchronized boolean install(FileInfo root) {
		if (closed || root == null || this.root != null)
			return false;
		this.root = root;
		return true;
	}

	synchronized FileInfo get() {
		return closed ? null : root;
	}

	/**
	 * Non-null require for internal Scanner code that historically assumed
	 * a live root after construction.
	 */
	synchronized FileInfo require() {
		FileInfo current = get();
		if (current == null)
			throw new IllegalStateException("Scanner root is not available");
		return current;
	}

	synchronized boolean close() {
		if (closed)
			return false;
		closed = true;
		root = null;
		return true;
	}

	synchronized boolean isClosed() {
		return closed;
	}
}
