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
 * Owns one-shot close/cleanup flags and the optional on-close listener
 * for a TTS toolbar dialog generation.
 *
 * Begin-cleanup / begin-finish serialize concurrent document-change and
 * stop-and-close paths so teardown cannot run twice. Close permanently
 * drops the listener.
 */
final class TtsToolbarSessionState {
	private boolean documentCleanedUp;
	private boolean closeFinished;
	private Runnable onCloseListener;
	private boolean closed;

	synchronized void setOnCloseListener(Runnable listener) {
		if (closed || closeFinished)
			return;
		onCloseListener = listener;
	}

	/**
	 * @return true when this caller should perform document cleanup
	 */
	synchronized boolean beginDocumentCleanup() {
		if (closed || documentCleanedUp)
			return false;
		documentCleanedUp = true;
		return true;
	}

	/**
	 * @return true when this caller should finish the close sequence
	 */
	synchronized boolean beginFinishClose() {
		if (closed || closeFinished)
			return false;
		closeFinished = true;
		return true;
	}

	/**
	 * Clears and returns the on-close listener for invocation.
	 */
	synchronized Runnable takeOnCloseListener() {
		if (closed)
			return null;
		Runnable previous = onCloseListener;
		onCloseListener = null;
		return previous;
	}

	synchronized boolean isCloseFinished() {
		return closed || closeFinished;
	}

	synchronized boolean isDocumentCleanedUp() {
		return closed || documentCleanedUp;
	}

	synchronized boolean close() {
		if (closed)
			return false;
		closed = true;
		documentCleanedUp = true;
		closeFinished = true;
		onCloseListener = null;
		return true;
	}

	synchronized boolean isClosed() {
		return closed;
	}
}
