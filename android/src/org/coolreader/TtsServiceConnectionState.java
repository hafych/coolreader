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

import org.coolreader.tts.TTSControlServiceAccessor;

/**
 * Owns the Activity TTS service accessor and selected engine package.
 *
 * The accessor is created lazily and retained until Activity destruction
 * unbinds it. Engine package updates stay synchronized with bind/init so a
 * timeout path cannot clear a replacement engine package, and destroy cannot
 * leave a live accessor after close.
 */
final class TtsServiceConnectionState {
	interface AccessorFactory {
		TTSControlServiceAccessor create();
	}

	private String enginePackage = "";
	private TTSControlServiceAccessor accessor;
	private boolean closed;

	synchronized void setEnginePackage(String value) {
		if (closed)
			return;
		enginePackage = value != null ? value : "";
	}

	synchronized String getEnginePackage() {
		return closed ? "" : enginePackage;
	}

	/**
	 * Returns the existing accessor or creates one while the owner is open.
	 */
	synchronized TTSControlServiceAccessor ensureAccessor(
			AccessorFactory factory) {
		if (closed)
			return null;
		if (factory == null)
			throw new IllegalArgumentException(
					"factory must not be null");
		if (accessor == null)
			accessor = factory.create();
		return accessor;
	}

	synchronized TTSControlServiceAccessor getAccessor() {
		return closed ? null : accessor;
	}

	/**
	 * Permanently closes the owner and returns the accessor for unbind.
	 */
	synchronized TTSControlServiceAccessor close() {
		if (closed)
			return null;
		closed = true;
		TTSControlServiceAccessor previous = accessor;
		accessor = null;
		enginePackage = "";
		return previous;
	}

	synchronized boolean isClosed() {
		return closed;
	}
}
