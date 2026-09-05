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
 * Owns the FileBrowser list adapter for one browser generation.
 *
 * Adapter creation, ListView attach and teardown share one synchronized
 * owner so a late cover-ready or DB callback cannot publish into a closed
 * browser. Close permanently drops the adapter.
 */
final class BrowserListAdapterState {
	private BaseListAdapter adapter;
	private boolean closed;

	/**
	 * Installs a new adapter. Returns the previous adapter (possibly null).
	 * When closed the new adapter is rejected and null is returned; the
	 * caller retains ownership of the rejected instance.
	 */
	synchronized BaseListAdapter install(BaseListAdapter next) {
		if (closed)
			return null;
		BaseListAdapter previous = adapter;
		adapter = next;
		return previous;
	}

	synchronized BaseListAdapter get() {
		return closed ? null : adapter;
	}

	/**
	 * Permanently closes the owner and returns any live adapter.
	 */
	synchronized BaseListAdapter close() {
		if (closed)
			return null;
		closed = true;
		BaseListAdapter previous = adapter;
		adapter = null;
		return previous;
	}

	synchronized boolean isClosed() {
		return closed;
	}
}
