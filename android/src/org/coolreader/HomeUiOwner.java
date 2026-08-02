/*
 * CoolReader for Android
 * Copyright (C) 2026 CoolReader Next contributors
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 2
 * of the License, or (at your option) any later version.
 */

package org.coolreader;

import org.coolreader.crengine.CRRootView;

/**
 * Owns the Activity home root view.
 */
final class HomeUiOwner {
	private CRRootView homeFrame;
	private boolean closed;

	synchronized boolean install(CRRootView frame) {
		if (closed || frame == null || homeFrame != null)
			return false;
		homeFrame = frame;
		return true;
	}

	synchronized CRRootView frame() {
		return closed ? null : homeFrame;
	}

	synchronized boolean isPresent() {
		return !closed && homeFrame != null;
	}

	synchronized CRRootView close() {
		if (closed)
			return null;
		closed = true;
		CRRootView previous = homeFrame;
		homeFrame = null;
		return previous;
	}

	synchronized boolean isClosed() {
		return closed;
	}
}
