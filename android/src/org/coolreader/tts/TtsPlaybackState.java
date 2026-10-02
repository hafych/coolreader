/*
 * CoolReader for Android
 * Copyright (C) 2026 CoolReader Next contributors
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 2
 * of the License, or (at your option) any later version.
 */

package org.coolreader.tts;

/**
 * Owns the TTS service playback status and continuous-error counter for one
 * service generation.
 *
 * PLAYING/PAUSED/STOPPED transitions and the bounded error counter share one
 * synchronized owner so media-session callbacks, utterance callbacks and
 * audio-focus paths cannot leave a mixed generation. The owner is pure JVM
 * (no Android types); the service maps {@link Status} to its platform
 * {@code State} at the boundary. Close permanently drops the slot.
 */
final class TtsPlaybackState {
	enum Status {
		STOPPED,
		PLAYING,
		PAUSED
	}

	private Status status = Status.STOPPED;
	private Status previous = Status.STOPPED;
	private int continuousErrors;
	private boolean closed;

	synchronized Status status() {
		return closed ? Status.STOPPED : status;
	}

	synchronized boolean isPlaying() {
		return !closed && status == Status.PLAYING;
	}

	synchronized boolean isPaused() {
		return !closed && status == Status.PAUSED;
	}

	/**
	 * Transitions to the given status. Returns true when the published
	 * status actually changed while open (caller notifies the listener).
	 */
	synchronized boolean transitionTo(Status next) {
		if (closed || next == null)
			return false;
		if (status == next)
			return false;
		status = next;
		if (status != previous) {
			previous = status;
			return true;
		}
		return false;
	}

	/**
	 * Parks the current status without touching previous and without
	 * reporting a change. Preserves the legacy prepare path which parks
	 * PAUSED silently; the next real transition still notifies exactly once.
	 */
	synchronized void parkSilent(Status next) {
		if (closed || next == null)
			return;
		status = next;
	}

	/**
	 * Forces both current and previous to STOPPED without reporting a change.
	 * Used for initial create/prepare paths that must not emit a transition.
	 */
	synchronized void resetSilent() {
		if (closed)
			return;
		status = Status.STOPPED;
		previous = Status.STOPPED;
	}

	synchronized int errorCount() {
		return closed ? 0 : continuousErrors;
	}

	synchronized void recordSuccess() {
		if (!closed)
			continuousErrors = 0;
	}

	/**
	 * Records one utterance error. Returns the new counter value.
	 */
	synchronized int recordError() {
		if (closed)
			return 0;
		if (continuousErrors < Integer.MAX_VALUE)
			continuousErrors++;
		return continuousErrors;
	}

	synchronized boolean close() {
		if (closed)
			return false;
		closed = true;
		status = Status.STOPPED;
		previous = Status.STOPPED;
		continuousErrors = 0;
		return true;
	}

	synchronized boolean isClosed() {
		return closed;
	}
}
