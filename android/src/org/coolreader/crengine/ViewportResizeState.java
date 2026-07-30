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

final class ViewportResizeState {
	private static final int FALLBACK_SIZE = 80;
	private static final int NORMAL_DELAY_MILLIS = 300;
	private static final int POST_RESUME_DELAY_MILLIS = 1000;
	private static final long POST_RESUME_WINDOW_MILLIS = 1000L;
	private static final long NO_RESUME_UPTIME = -1L;

	private volatile Size requestedSize;
	private volatile Size appliedSize;
	private long lastResumeUptime = NO_RESUME_UPTIME;
	private Request current;
	private Request applying;
	private boolean appliedNeedsCompletion;
	private boolean closed;

	ViewportResizeState(int width, int height) {
		requestedSize = normalizedSize(width, height);
	}

	synchronized boolean recordResume(long uptimeMillis) {
		if (closed || uptimeMillis < 0)
			return false;
		lastResumeUptime = uptimeMillis;
		return true;
	}

	synchronized int resizeDelayMillis(long nowUptimeMillis) {
		if (lastResumeUptime == NO_RESUME_UPTIME
				|| nowUptimeMillis < 0)
			return NORMAL_DELAY_MILLIS;
		if (nowUptimeMillis < lastResumeUptime)
			return POST_RESUME_DELAY_MILLIS;
		return nowUptimeMillis - lastResumeUptime
				< POST_RESUME_WINDOW_MILLIS
						? POST_RESUME_DELAY_MILLIS
						: NORMAL_DELAY_MILLIS;
	}

	synchronized Request request(int width, int height) {
		if (closed)
			return null;
		Size requested = normalizedSize(width, height);
		requestedSize = requested;
		current = new Request(requested);
		return current;
	}

	synchronized Request requestCurrent() {
		if (closed)
			return null;
		current = new Request(requestedSize);
		return current;
	}

	Size requestedSize() {
		return requestedSize;
	}

	Size appliedSize() {
		return appliedSize;
	}

	Size appliedOrRequestedSize() {
		Size applied = appliedSize;
		return applied != null ? applied : requestedSize;
	}

	synchronized boolean requestedIsApplied() {
		return applying == null
				&& !appliedNeedsCompletion
				&& sameSize(requestedSize, appliedSize);
	}

	synchronized boolean isCurrent(Request request) {
		return !closed && request != null && current == request;
	}

	synchronized boolean completeIfApplied(Request request) {
		if (!isCurrent(request)
				|| applying != null
				|| appliedNeedsCompletion
				|| !sameSize(request.size, appliedSize))
			return false;
		current = null;
		return true;
	}

	synchronized boolean beginApply(Request request) {
		if (!isCurrent(request) || applying != null)
			return false;
		applying = request;
		return true;
	}

	synchronized boolean finishApply(Request request) {
		if (closed || request == null || applying != request)
			return false;
		appliedSize = request.size;
		applying = null;
		appliedNeedsCompletion = true;
		return true;
	}

	synchronized boolean cancelApply(Request request) {
		if (request == null || applying != request)
			return false;
		applying = null;
		return true;
	}

	synchronized boolean publishApplied(Size size) {
		if (closed || size == null || applying != null)
			return false;
		appliedSize = size;
		return true;
	}

	synchronized boolean completeCurrentApplied() {
		if (closed
				|| applying != null
				|| !appliedNeedsCompletion
				|| current == null
				|| !sameSize(current.size, appliedSize))
			return false;
		current = null;
		appliedNeedsCompletion = false;
		return true;
	}

	synchronized boolean complete(Request request) {
		if (!isCurrent(request))
			return false;
		current = null;
		return true;
	}

	synchronized boolean close() {
		if (closed)
			return false;
		closed = true;
		current = null;
		applying = null;
		appliedNeedsCompletion = false;
		return true;
	}

	private static boolean sameSize(Size first, Size second) {
		return first != null
				&& second != null
				&& first.width == second.width
				&& first.height == second.height;
	}

	private static Size normalizedSize(int width, int height) {
		return new Size(
				width > 0 ? width : FALLBACK_SIZE,
				height > 0 ? height : FALLBACK_SIZE);
	}

	static final class Request {
		private final Size size;

		private Request(Size size) {
			this.size = size;
		}

		Size size() {
			return size;
		}
	}

	static final class Size {
		private final int width;
		private final int height;

		private Size(int width, int height) {
			this.width = width;
			this.height = height;
		}

		int width() {
			return width;
		}

		int height() {
			return height;
		}
	}
}
