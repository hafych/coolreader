package org.coolreader.crengine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class FreezableRegistry<T> {
	private final ArrayList<T> builder = new ArrayList<>();
	private List<T> frozen;
	private boolean closed;

	synchronized boolean add(T item) {
		if (item == null)
			throw new IllegalArgumentException("Registry item must not be null");
		if (closed || frozen != null)
			return false;
		builder.add(item);
		return true;
	}

	synchronized List<T> snapshot() {
		if (closed)
			return frozen != null
					? new ArrayList<>(frozen)
					: Collections.emptyList();
		return new ArrayList<>(frozen == null ? builder : frozen);
	}

	synchronized List<T> freeze() {
		if (closed)
			return frozen != null ? frozen : Collections.emptyList();
		if (frozen == null) {
			frozen = Collections.unmodifiableList(
					new ArrayList<>(builder));
			builder.clear();
		}
		return frozen;
	}

	synchronized boolean isFrozen() {
		return frozen != null;
	}

	/**
	 * Permanently freezes (if not already) and blocks further adds.
	 */
	synchronized boolean close() {
		if (closed)
			return false;
		closed = true;
		if (frozen == null) {
			frozen = Collections.unmodifiableList(
					new ArrayList<>(builder));
			builder.clear();
		}
		return true;
	}

	synchronized boolean isClosed() {
		return closed;
	}
}
