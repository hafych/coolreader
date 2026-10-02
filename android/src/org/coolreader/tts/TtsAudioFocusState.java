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
 * Owns the TTS service audio-focus flags for one service generation.
 *
 * Authorized/delayed/resume-on-gain share one synchronized owner so focus
 * callbacks and request/abandon paths cannot leave a mixed triple. The owner
 * is pure JVM (no Android types); the service interprets the snapshot to
 * decide play/pause/resume. Close permanently drops the slot.
 */
final class TtsAudioFocusState {
	private boolean authorized;
	private boolean delayed;
	private boolean resumeOnGain;
	private boolean closed;

	synchronized boolean isAuthorized() {
		return !closed && authorized;
	}

	synchronized Snapshot snapshot() {
		if (closed)
			return new Snapshot(false, false, false);
		return new Snapshot(authorized, delayed, resumeOnGain);
	}

	/**
	 * Audio-focus gain: authorizes playback and consumes any pending
	 * delayed/resume request. Returns true when the caller should resume
	 * playback (a delayed or resume request was pending).
	 */
	synchronized boolean onGain() {
		if (closed)
			return false;
		authorized = true;
		boolean shouldResume = delayed || resumeOnGain;
		delayed = false;
		resumeOnGain = false;
		return shouldResume;
	}

	/** Permanent loss: deauthorizes and drops all pending requests. */
	synchronized void onLoss() {
		if (closed)
			return;
		authorized = false;
		resumeOnGain = false;
		delayed = false;
	}

	/**
	 * Transient loss: deauthorizes and drops pending requests. Returns the
	 * caller's current playing flag so the caller can arm resume-on-gain.
	 */
	synchronized void onTransientLoss() {
		if (closed)
			return;
		authorized = false;
		resumeOnGain = false;
		delayed = false;
	}

	synchronized void armResumeOnGain() {
		if (!closed)
			resumeOnGain = true;
	}

	synchronized void onGranted() {
		if (closed)
			return;
		authorized = true;
		delayed = false;
		resumeOnGain = false;
	}

	synchronized void onDelayed() {
		if (closed)
			return;
		authorized = false;
		delayed = true;
		resumeOnGain = false;
	}

	synchronized void onFailed() {
		if (closed)
			return;
		authorized = false;
		delayed = false;
		resumeOnGain = false;
	}

	synchronized void onGrantedWithResult(boolean granted) {
		if (closed)
			return;
		authorized = granted;
		delayed = false;
		resumeOnGain = false;
	}

	synchronized void forceAuthorized() {
		if (!closed)
			authorized = true;
	}

	synchronized boolean close() {
		if (closed)
			return false;
		closed = true;
		authorized = false;
		delayed = false;
		resumeOnGain = false;
		return true;
	}

	synchronized boolean isClosed() {
		return closed;
	}

	static final class Snapshot {
		final boolean authorized;
		final boolean delayed;
		final boolean resumeOnGain;

		Snapshot(boolean authorized, boolean delayed, boolean resumeOnGain) {
			this.authorized = authorized;
			this.delayed = delayed;
			this.resumeOnGain = resumeOnGain;
		}

		boolean shouldResume() {
			return delayed || resumeOnGain;
		}
	}
}
