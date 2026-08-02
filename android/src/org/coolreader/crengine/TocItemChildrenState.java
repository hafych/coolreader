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
import java.util.Collections;
import java.util.List;

/**
 * Owns TOC tree children for one {@link TOCItem} node.
 *
 * Add/get/indexOf share one synchronized list so concurrent dialog
 * expansion cannot escape a half-published child array. Close permanently
 * clears children.
 */
final class TocItemChildrenState {
	private ArrayList<TOCItem> children;
	private boolean closed;

	synchronized int size() {
		return (!closed && children != null) ? children.size() : 0;
	}

	synchronized TOCItem get(int index) {
		if (closed || children == null || index < 0 || index >= children.size())
			return null;
		return children.get(index);
	}

	synchronized int indexOf(TOCItem item) {
		if (closed || children == null || item == null)
			return -1;
		return children.indexOf(item);
	}

	/**
	 * Appends {@code child} and returns its index, or -1 when closed.
	 */
	synchronized int add(TOCItem child) {
		if (closed || child == null)
			return -1;
		if (children == null)
			children = new ArrayList<>();
		children.add(child);
		return children.size() - 1;
	}

	synchronized List<TOCItem> snapshot() {
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
