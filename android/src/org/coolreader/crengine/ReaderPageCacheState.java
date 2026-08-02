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

/**
 * Owns the two-slot reader page cache (current and next).
 *
 * Publication, promotion, invalidation and document-close detach all go through
 * one synchronized owner so a replacement or teardown cannot leave parallel
 * raw slot fields half-updated. Callers remain responsible for recycling any
 * identity returned as releasable.
 */
final class ReaderPageCacheState<T> {
	static final class Snapshot<T> {
		private final T current;
		private final T next;

		private Snapshot(T current, T next) {
			this.current = current;
			this.next = next;
		}

		T current() {
			return current;
		}

		T next() {
			return next;
		}
	}

	static final class Publication<T> {
		private final boolean accepted;
		private final T published;
		private final T releasable;

		private Publication(
				boolean accepted,
				T published,
				T releasable) {
			this.accepted = accepted;
			this.published = published;
			this.releasable = releasable;
		}

		boolean isAccepted() {
			return accepted;
		}

		T published() {
			return published;
		}

		T releasable() {
			return releasable;
		}
	}

	private T current;
	private T next;
	private boolean closed;

	synchronized Snapshot<T> snapshot() {
		return new Snapshot<>(current, next);
	}

	synchronized T current() {
		return current;
	}

	synchronized T next() {
		return next;
	}

	/**
	 * If {@code page} is currently in the next slot, swaps it into current.
	 * Returns the current slot when it matches {@code page}, otherwise null.
	 */
	synchronized T makeCurrent(T page) {
		if (closed || page == null)
			return null;
		if (next == page) {
			T temporary = next;
			next = current;
			current = temporary;
		}
		return current == page ? current : null;
	}

	/**
	 * Installs {@code candidate} as the current page.
	 * When accepted, {@link Publication#releasable()} is the previous current
	 * page if it is no longer referenced by either slot. When rejected because
	 * the owner is closed, releasable is the candidate itself.
	 */
	synchronized Publication<T> publishCurrent(T candidate) {
		if (candidate == null)
			return new Publication<>(false, null, null);
		if (closed)
			return new Publication<>(false, null, candidate);
		T previous = current;
		current = candidate;
		T releasable = previous != null
				&& previous != candidate
				&& previous != next
						? previous
						: null;
		return new Publication<>(true, candidate, releasable);
	}

	/**
	 * Installs {@code candidate} as the next page.
	 */
	synchronized Publication<T> publishNext(T candidate) {
		if (candidate == null)
			return new Publication<>(false, null, null);
		if (closed)
			return new Publication<>(false, null, candidate);
		T previous = next;
		next = candidate;
		T releasable = previous != null
				&& previous != candidate
				&& previous != current
						? previous
						: null;
		return new Publication<>(true, candidate, releasable);
	}

	/**
	 * Detaches both slots and returns the previous identities for recycle.
	 */
	synchronized Snapshot<T> clear() {
		Snapshot<T> previous = new Snapshot<>(current, next);
		current = null;
		next = null;
		return previous;
	}

	/**
	 * Begins an asynchronous document-close capture of the current slots.
	 */
	synchronized ReaderPageCacheClose<T> beginClose() {
		return ReaderPageCacheClose.begin(current, next);
	}

	/**
	 * Publishes the live slots into a close request and detaches them when the
	 * request still owns serialization.
	 */
	synchronized boolean publishSerializedClose(
			ReaderPageCacheClose<T> close) {
		if (close == null)
			return false;
		if (!close.publishSerialized(current, next))
			return false;
		current = null;
		next = null;
		return true;
	}

	synchronized void close() {
		if (closed)
			return;
		closed = true;
		current = null;
		next = null;
	}

	synchronized boolean isClosed() {
		return closed;
	}
}
