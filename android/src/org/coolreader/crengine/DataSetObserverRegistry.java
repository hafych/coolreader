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

import android.database.DataSetObserver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Owns DataSetObserver registration for list adapters.
 *
 * Register/unregister and notify use one synchronized owner so concurrent
 * detach cannot leave a half-updated observer list. Close permanently drops
 * all observers.
 */
final class DataSetObserverRegistry {
	private final ArrayList<DataSetObserver> observers = new ArrayList<>();
	private boolean closed;

	synchronized void register(DataSetObserver observer) {
		if (closed || observer == null)
			return;
		if (!observers.contains(observer))
			observers.add(observer);
	}

	synchronized void unregister(DataSetObserver observer) {
		if (closed || observer == null)
			return;
		observers.remove(observer);
	}

	synchronized List<DataSetObserver> snapshot() {
		if (closed || observers.isEmpty())
			return Collections.emptyList();
		return Collections.unmodifiableList(new ArrayList<>(observers));
	}

	synchronized void notifyChanged() {
		for (DataSetObserver observer : snapshot())
			observer.onChanged();
	}

	synchronized boolean close() {
		if (closed)
			return false;
		closed = true;
		observers.clear();
		return true;
	}

	synchronized boolean isClosed() {
		return closed;
	}
}
