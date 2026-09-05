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
 * Owns the FileBrowser OPDS download task for one browser generation.
 *
 * Navigation, completion callbacks and teardown share one synchronized slot
 * so a late callback cannot cancel a replacement download and a replacement
 * cannot leak its predecessor. Close permanently drops the slot and returns
 * the live task for cancel.
 */
final class BrowserDownloadState {
	private OPDSUtil.DownloadTask task;
	private boolean closed;

	synchronized OPDSUtil.DownloadTask get() {
		return closed ? null : task;
	}

	/**
	 * Installs a new download task. Returns the previous task (possibly null)
	 * for the caller to cancel. When closed the new task is rejected and
	 * returned back so the caller cancels it immediately.
	 */
	synchronized OPDSUtil.DownloadTask install(OPDSUtil.DownloadTask next) {
		if (closed)
			return next;
		OPDSUtil.DownloadTask previous = task;
		task = next;
		return previous;
	}

	/**
	 * Clears the slot only when the given task is the current owner.
	 */
	synchronized boolean clearIf(OPDSUtil.DownloadTask expected) {
		if (closed || expected == null || task != expected)
			return false;
		task = null;
		return true;
	}

	/**
	 * Takes the current task and clears the slot without closing.
	 */
	synchronized OPDSUtil.DownloadTask take() {
		if (closed)
			return null;
		OPDSUtil.DownloadTask previous = task;
		task = null;
		return previous;
	}

	/**
	 * Permanently closes the owner and returns any live task for cancel.
	 */
	synchronized OPDSUtil.DownloadTask close() {
		if (closed)
			return null;
		closed = true;
		OPDSUtil.DownloadTask previous = task;
		task = null;
		return previous;
	}

	synchronized boolean isClosed() {
		return closed;
	}
}
