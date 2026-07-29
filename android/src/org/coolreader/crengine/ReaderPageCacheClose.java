/*
 * CoolReader for Android
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 2
 * of the License, or (at your option) any later version.
 */

package org.coolreader.crengine;

/**
 * Owns the page-cache resources detached by one asynchronous document close.
 */
final class ReaderPageCacheClose<T> {
	private final T initialCurrent;
	private final T initialNext;
	private T serializedCurrent;
	private T serializedNext;
	private boolean serializedPublished;
	private boolean finished;

	private ReaderPageCacheClose(T current, T next) {
		initialCurrent = current;
		initialNext = next;
	}

	static <T> ReaderPageCacheClose<T> begin(
			T current, T next) {
		return new ReaderPageCacheClose<>(current, next);
	}

	synchronized boolean publishSerialized(
			T current, T next) {
		if (finished || serializedPublished)
			return false;
		serializedCurrent = current;
		serializedNext = next;
		serializedPublished = true;
		return true;
	}

	synchronized Resources<T> finish() {
		if (finished)
			return null;
		finished = true;
		return new Resources<>(
				initialCurrent,
				initialNext,
				serializedCurrent,
				serializedNext);
	}

	static final class Resources<T> {
		private final T initialCurrent;
		private final T initialNext;
		private final T serializedCurrent;
		private final T serializedNext;

		private Resources(
				T initialCurrent,
				T initialNext,
				T serializedCurrent,
				T serializedNext) {
			this.initialCurrent = initialCurrent;
			this.initialNext = initialNext;
			this.serializedCurrent = serializedCurrent;
			this.serializedNext = serializedNext;
		}

		T initialCurrent() {
			return initialCurrent;
		}

		T initialNext() {
			return initialNext;
		}

		T serializedCurrent() {
			return serializedCurrent;
		}

		T serializedNext() {
			return serializedNext;
		}
	}
}
