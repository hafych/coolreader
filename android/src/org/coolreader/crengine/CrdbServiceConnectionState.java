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

import org.coolreader.db.CRDBServiceAccessor;

/**
 * Owns the Activity CRDB service accessor.
 *
 * The accessor is created lazily and retained until Activity destruction
 * unbinds it. Destroy permanently closes the owner so late bind requests
 * cannot install a new accessor into a torn-down Activity.
 */
final class CrdbServiceConnectionState {
	interface AccessorFactory {
		CRDBServiceAccessor create();
	}

	private CRDBServiceAccessor accessor;
	private boolean closed;

	/**
	 * Returns the existing accessor or creates one while the owner is open.
	 */
	synchronized CRDBServiceAccessor ensure(AccessorFactory factory) {
		if (closed)
			return null;
		if (factory == null)
			throw new IllegalArgumentException(
					"factory must not be null");
		if (accessor == null)
			accessor = factory.create();
		return accessor;
	}

	synchronized CRDBServiceAccessor get() {
		return closed ? null : accessor;
	}

	/**
	 * Permanently closes the owner and returns the accessor for unbind.
	 */
	synchronized CRDBServiceAccessor close() {
		if (closed)
			return null;
		closed = true;
		CRDBServiceAccessor previous = accessor;
		accessor = null;
		return previous;
	}

	synchronized boolean isClosed() {
		return closed;
	}
}
