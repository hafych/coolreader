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
 * Owns MetadataScanSession finished/complete flags.
 *
 * Concurrent batch callbacks share one synchronized pair so a stop path
 * cannot mark complete after finish, or finish twice.
 */
final class MetadataScanSessionState {
	private boolean finished;
	private boolean complete = true;
	private boolean closed;

	synchronized void markIncomplete() {
		if (!closed)
			complete = false;
	}

	/**
	 * Claims finish once. Returns true when this caller should invoke the
	 * ready callback with the current complete flag.
	 */
	synchronized boolean beginFinish() {
		if (closed || finished)
			return false;
		finished = true;
		return true;
	}

	synchronized boolean isComplete() {
		return complete;
	}

	synchronized boolean isFinished() {
		return finished;
	}

	synchronized boolean close() {
		if (closed)
			return false;
		closed = true;
		finished = true;
		return true;
	}

	synchronized boolean isClosed() {
		return closed;
	}
}
