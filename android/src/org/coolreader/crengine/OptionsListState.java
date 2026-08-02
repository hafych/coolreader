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
 * Owns the option rows of one OptionsDialog.OptionsListView.
 *
 * Add/remove/clear and adapter reads share one synchronized list so concurrent
 * theme rebuild cannot leave a half-published option set. Close permanently
 * clears the list.
 */
final class OptionsListState {
	private final ArrayList<OptionsDialog.OptionBase> options =
			new ArrayList<>();
	private boolean closed;

	synchronized void add(OptionsDialog.OptionBase option) {
		if (closed || option == null)
			return;
		options.add(option);
	}

	synchronized boolean remove(int index) {
		if (closed || index < 0 || index >= options.size())
			return false;
		options.remove(index);
		return true;
	}

	synchronized boolean remove(OptionsDialog.OptionBase option) {
		return !closed && options.remove(option);
	}

	synchronized void clear() {
		if (!closed)
			options.clear();
	}

	synchronized int size() {
		return closed ? 0 : options.size();
	}

	synchronized OptionsDialog.OptionBase get(int index) {
		if (closed || index < 0 || index >= options.size())
			return null;
		return options.get(index);
	}

	synchronized List<OptionsDialog.OptionBase> snapshot() {
		if (closed)
			return Collections.emptyList();
		return Collections.unmodifiableList(new ArrayList<>(options));
	}

	synchronized boolean close() {
		if (closed)
			return false;
		closed = true;
		options.clear();
		return true;
	}

	synchronized boolean isClosed() {
		return closed;
	}
}
