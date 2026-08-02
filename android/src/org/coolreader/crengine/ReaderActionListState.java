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
 * Owns a mutable {@link ReaderAction} list for toolbar/layout generation.
 *
 * Install, add and snapshot share one synchronized container so concurrent
 * layout calc cannot leave a half-published action set. Close permanently
 * clears the list.
 */
final class ReaderActionListState {
	private final ArrayList<ReaderAction> actions = new ArrayList<>();
	private boolean closed;

	/**
	 * Replaces contents with a copy of {@code source}.
	 */
	synchronized void replaceAll(ArrayList<ReaderAction> source) {
		if (closed)
			return;
		actions.clear();
		if (source != null)
			actions.addAll(source);
	}

	synchronized void add(ReaderAction action) {
		if (closed || action == null)
			return;
		actions.add(action);
	}

	synchronized void clear() {
		if (!closed)
			actions.clear();
	}

	synchronized int size() {
		return closed ? 0 : actions.size();
	}

	synchronized ReaderAction get(int index) {
		if (closed || index < 0 || index >= actions.size())
			return null;
		return actions.get(index);
	}

	synchronized List<ReaderAction> snapshot() {
		if (closed)
			return Collections.emptyList();
		return Collections.unmodifiableList(new ArrayList<>(actions));
	}

	synchronized ArrayList<ReaderAction> copyAsArrayList() {
		return closed ? new ArrayList<>() : new ArrayList<>(actions);
	}

	synchronized boolean close() {
		if (closed)
			return false;
		closed = true;
		actions.clear();
		return true;
	}

	synchronized boolean isClosed() {
		return closed;
	}
}
