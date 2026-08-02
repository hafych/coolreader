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

import java.util.ArrayList;

/**
 * Owns CoverpageManager work queues and latest scheduled task tokens.
 *
 * Check-cache, scan-file and ready queues plus their GUI task owners share
 * one synchronized state so concurrent schedule/notify paths cannot leave a
 * mixed generation. Close permanently clears queues and tasks.
 */
final class CoverpageWorkState {
	private final CoverpageImageQueue checkCacheQueue =
			new CoverpageImageQueue();
	private final CoverpageImageQueue scanFileQueue =
			new CoverpageImageQueue();
	private final CoverpageImageQueue readyQueue =
			new CoverpageImageQueue();
	private Runnable lastCheckCacheTask;
	private Runnable lastScanFileTask;
	private Runnable lastReadyNotifyTask;
	private long firstReadyTimestamp;
	private boolean closed;

	synchronized void removeFromAll(CoverpageManager.ImageItem file) {
		if (closed || file == null)
			return;
		checkCacheQueue.remove(file);
		scanFileQueue.remove(file);
		readyQueue.remove(file);
	}

	synchronized void clearQueues() {
		if (closed)
			return;
		checkCacheQueue.clear();
		scanFileQueue.clear();
		readyQueue.clear();
	}

	synchronized boolean addCheckCacheOnTop(CoverpageManager.ImageItem file) {
		return !closed && checkCacheQueue.addOnTop(file);
	}

	synchronized boolean addScanFileOnTop(CoverpageManager.ImageItem file) {
		return !closed && scanFileQueue.addOnTop(file);
	}

	synchronized void addReady(CoverpageManager.ImageItem file) {
		if (closed || file == null)
			return;
		if (readyQueue.empty())
			firstReadyTimestamp = Utils.timeStamp();
		readyQueue.add(file);
	}

	/**
	 * Claims the next check-cache item only when {@code task} is still the
	 * current owner.
	 */
	synchronized CoverpageManager.ImageItem nextCheckCacheIfCurrent(
			Runnable task) {
		if (closed || lastCheckCacheTask != task)
			return null;
		return checkCacheQueue.next();
	}

	synchronized CoverpageManager.ImageItem nextScanFileIfCurrent(
			Runnable task) {
		if (closed || lastScanFileTask != task)
			return null;
		return scanFileQueue.next();
	}

	synchronized ArrayList<CoverpageManager.ImageItem> drainReady() {
		if (closed)
			return new ArrayList<>();
		return readyQueue.drain();
	}

	synchronized void setLastCheckCacheTask(Runnable task) {
		if (!closed)
			lastCheckCacheTask = task;
	}

	synchronized Runnable getLastCheckCacheTask() {
		return closed ? null : lastCheckCacheTask;
	}

	synchronized void setLastScanFileTask(Runnable task) {
		if (!closed)
			lastScanFileTask = task;
	}

	synchronized Runnable getLastScanFileTask() {
		return closed ? null : lastScanFileTask;
	}

	synchronized void setLastReadyNotifyTask(Runnable task) {
		if (!closed)
			lastReadyNotifyTask = task;
	}

	synchronized Runnable getLastReadyNotifyTask() {
		return closed ? null : lastReadyNotifyTask;
	}

	synchronized long getFirstReadyTimestamp() {
		return firstReadyTimestamp;
	}

	synchronized void markReadyNotified() {
		if (!closed)
			firstReadyTimestamp = Utils.timeStamp();
	}

	synchronized boolean close() {
		if (closed)
			return false;
		closed = true;
		checkCacheQueue.clear();
		scanFileQueue.clear();
		readyQueue.clear();
		lastCheckCacheTask = null;
		lastScanFileTask = null;
		lastReadyNotifyTask = null;
		firstReadyTimestamp = 0;
		return true;
	}

	synchronized boolean isClosed() {
		return closed;
	}
}
