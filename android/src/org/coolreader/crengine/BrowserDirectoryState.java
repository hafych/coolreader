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
 * Owns the FileBrowser current-directory identity for one browser generation.
 *
 * GUI navigation, cover-ready listeners and adapter reads share one
 * synchronized reference so a late callback cannot revive a closed browser or
 * observe a torn-down directory pointer. Close permanently nulls the slot.
 */
final class BrowserDirectoryState {
	private FileInfo directory;
	private boolean closed;

	synchronized FileInfo get() {
		return closed ? null : directory;
	}

	/**
	 * Publishes a new current directory. Returns the previous identity when
	 * the publication was accepted, including {@code null} when the slot was
	 * empty. Returns a sentinel via {@link #isClosed()} when rejected after
	 * close — callers should check {@link #isClosed()} after set when they
	 * need to distinguish rejection. For the common path, {@link #setReturningPrevious}
	 * is preferred.
	 */
	synchronized FileInfo set(FileInfo next) {
		if (closed)
			return directory;
		FileInfo previous = directory;
		directory = next;
		return previous;
	}

	/**
	 * Atomically replaces the current directory and returns both previous and
	 * whether the owner is still open.
	 */
	synchronized Change setReturningPrevious(FileInfo next) {
		if (closed)
			return new Change(false, directory, directory);
		FileInfo previous = directory;
		directory = next;
		return new Change(true, previous, next);
	}

	synchronized boolean close() {
		if (closed)
			return false;
		closed = true;
		directory = null;
		return true;
	}

	synchronized boolean isClosed() {
		return closed;
	}

	static final class Change {
		final boolean accepted;
		final FileInfo previous;
		final FileInfo current;

		Change(boolean accepted, FileInfo previous, FileInfo current) {
			this.accepted = accepted;
			this.previous = previous;
			this.current = current;
		}

		boolean directoryChanged() {
			return accepted && previous != current;
		}
	}
}
