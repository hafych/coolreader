/*
 * CoolReader for Android
 * Copyright (C) 2026 CoolReader Next contributors
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 2
 * of the License, or (at your option) any later version.
 */

package org.coolreader.plugins.litres;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Owns LitresGenre child list for one genre node.
 */
final class LitresGenreChildrenState {
	private ArrayList<LitresConnection.LitresGenre> children;
	private boolean closed;

	synchronized void add(LitresConnection.LitresGenre child) {
		if (closed || child == null)
			return;
		if (children == null)
			children = new ArrayList<>();
		children.add(child);
	}

	synchronized int size() {
		return (!closed && children != null) ? children.size() : 0;
	}

	synchronized LitresConnection.LitresGenre get(int index) {
		if (closed || children == null || index < 0 || index >= children.size())
			return null;
		return children.get(index);
	}

	synchronized List<LitresConnection.LitresGenre> snapshot() {
		if (closed || children == null)
			return Collections.emptyList();
		return Collections.unmodifiableList(new ArrayList<>(children));
	}

	synchronized boolean close() {
		if (closed)
			return false;
		closed = true;
		children = null;
		return true;
	}

	synchronized boolean isClosed() {
		return closed;
	}
}
