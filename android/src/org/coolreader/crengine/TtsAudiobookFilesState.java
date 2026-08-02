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

import java.io.File;

/**
 * Owns audiobook word-timing related File paths for one TTS toolbar
 * generation.
 *
 * Set/get/clear share one synchronized triple so concurrent init and stop
 * cannot observe half-cleared path slots. Close permanently drops paths.
 */
final class TtsAudiobookFilesState {
	private File wordTimingFile;
	private File sentenceInfoFile;
	private File sentenceTimingCacheFile;
	private boolean closed;

	synchronized void set(
			File wordTiming, File sentenceInfo, File timingCache) {
		if (closed)
			return;
		wordTimingFile = wordTiming;
		sentenceInfoFile = sentenceInfo;
		sentenceTimingCacheFile = timingCache;
	}

	synchronized void clear() {
		if (closed)
			return;
		wordTimingFile = null;
		sentenceInfoFile = null;
		sentenceTimingCacheFile = null;
	}

	synchronized File getWordTimingFile() {
		return closed ? null : wordTimingFile;
	}

	synchronized File getSentenceInfoFile() {
		return closed ? null : sentenceInfoFile;
	}

	synchronized File getSentenceTimingCacheFile() {
		return closed ? null : sentenceTimingCacheFile;
	}

	/**
	 * Snapshot for background work that must not hold the owner lock.
	 */
	synchronized Snapshot snapshot() {
		if (closed)
			return new Snapshot(null, null, null);
		return new Snapshot(
				wordTimingFile, sentenceInfoFile, sentenceTimingCacheFile);
	}

	synchronized boolean close() {
		if (closed)
			return false;
		closed = true;
		wordTimingFile = null;
		sentenceInfoFile = null;
		sentenceTimingCacheFile = null;
		return true;
	}

	synchronized boolean isClosed() {
		return closed;
	}

	static final class Snapshot {
		final File wordTimingFile;
		final File sentenceInfoFile;
		final File sentenceTimingCacheFile;

		Snapshot(File wordTiming, File sentenceInfo, File timingCache) {
			this.wordTimingFile = wordTiming;
			this.sentenceInfoFile = sentenceInfo;
			this.sentenceTimingCacheFile = timingCache;
		}
	}
}
