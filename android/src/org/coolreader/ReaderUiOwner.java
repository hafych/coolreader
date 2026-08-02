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

import org.coolreader.crengine.ReaderView;
import org.coolreader.crengine.ReaderViewLayout;

/**
 * Owns the Activity-scoped reader view and its frame layout.
 *
 * Install publishes a fully built pair. Clear releases both references on
 * teardown so late UI work cannot resurrect a destroyed reader.
 */
final class ReaderUiOwner {
	private ReaderView readerView;
	private ReaderViewLayout readerFrame;
	private boolean closed;

	synchronized boolean install(
			ReaderView view,
			ReaderViewLayout frame) {
		if (closed
				|| view == null
				|| frame == null
				|| readerView != null)
			return false;
		readerView = view;
		readerFrame = frame;
		return true;
	}

	synchronized ReaderView view() {
		return closed ? null : readerView;
	}

	synchronized ReaderViewLayout frame() {
		return closed ? null : readerFrame;
	}

	synchronized boolean isPresent() {
		return !closed && readerView != null;
	}

	/**
	 * Permanently closes ownership and returns the prior reader for teardown.
	 */
	synchronized ReaderView close() {
		if (closed)
			return null;
		closed = true;
		ReaderView previous = readerView;
		readerView = null;
		readerFrame = null;
		return previous;
	}

	synchronized boolean isClosed() {
		return closed;
	}
}
