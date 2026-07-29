package org.coolreader.crengine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class FreezableRegistry<T> {
	private final ArrayList<T> builder = new ArrayList<>();
	private List<T> frozen;

	synchronized boolean add(T item) {
		if (item == null)
			throw new IllegalArgumentException("Registry item must not be null");
		if (frozen != null)
			return false;
		builder.add(item);
		return true;
	}

	synchronized List<T> snapshot() {
		return new ArrayList<>(frozen == null ? builder : frozen);
	}

	synchronized List<T> freeze() {
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
}
