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

import java.io.File;

/**
 * Owns the TTS service audiobook file, start time and enabled flag for one
 * service generation.
 *
 * Set and reads share one synchronized snapshot so a sentence probe cannot
 * observe a new file with the previous start time. Close permanently drops
 * the slot. The owner does not touch MediaPlayer; the service stops playback
 * when the published file identity changes.
 */
final class TtsAudiobookState {
	private File audioFile;
	private double startTime;
	private boolean enabled;
	private boolean closed;

	synchronized boolean isEnabled() {
		return !closed && enabled;
	}

	synchronized File audioFile() {
		return closed ? null : audioFile;
	}

	synchronized double startTime() {
		return closed ? 0 : startTime;
	}

	/**
	 * Publishes a new audiobook. Null file disables the slot. Returns true
	 * when the previous file identity differs and the caller must stop the
	 * current player before applying the new file.
	 */
	synchronized boolean set(File nextFile, double nextStartTime) {
		if (closed)
			return false;
		boolean fileChanged = audioFile != null && !audioFile.equals(nextFile);
		audioFile = nextFile;
		startTime = nextStartTime;
		enabled = nextFile != null;
		return fileChanged;
	}

	synchronized boolean close() {
		if (closed)
			return false;
		closed = true;
		audioFile = null;
		startTime = 0;
		enabled = false;
		return true;
	}

	synchronized boolean isClosed() {
		return closed;
	}
}
