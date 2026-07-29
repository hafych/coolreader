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
 * Owns one pending key single/double-click decision by identity.
 */
final class KeyDoubleClickState<T> {
	private Pending<T> current;
	private boolean closed;

	synchronized Pending<T> defer(
			int keyCode,
			long startedAt,
			T singleAction,
			T doubleAction) {
		if (closed
				|| singleAction == null
				|| doubleAction == null)
			return null;
		current = new Pending<>(
				keyCode,
				startedAt,
				singleAction,
				doubleAction);
		return current;
	}

	synchronized PressResult<T> resolvePress(
			int keyCode,
			long now,
			long interval) {
		if (closed || current == null)
			return null;
		Pending<T> pending = current;
		current = null;
		long elapsed = now - pending.startedAt;
		boolean matches =
				keyCode == pending.keyCode
						&& interval > 0
						&& now >= pending.startedAt
						&& elapsed >= 0
						&& elapsed < interval;
		return new PressResult<>(
				matches
						? pending.doubleAction
						: pending.singleAction,
				matches);
	}

	synchronized T claimSingle(Pending<T> pending) {
		if (closed || pending == null || current != pending)
			return null;
		current = null;
		return pending.singleAction;
	}

	synchronized void cancel() {
		current = null;
	}

	synchronized boolean close() {
		if (closed)
			return false;
		closed = true;
		current = null;
		return true;
	}

	static final class Pending<T> {
		private final int keyCode;
		private final long startedAt;
		private final T singleAction;
		private final T doubleAction;

		private Pending(
				int keyCode,
				long startedAt,
				T singleAction,
				T doubleAction) {
			this.keyCode = keyCode;
			this.startedAt = startedAt;
			this.singleAction = singleAction;
			this.doubleAction = doubleAction;
		}
	}

	static final class PressResult<T> {
		private final T action;
		private final boolean consumesPress;

		private PressResult(T action, boolean consumesPress) {
			this.action = action;
			this.consumesPress = consumesPress;
		}

		T action() {
			return action;
		}

		boolean consumesPress() {
			return consumesPress;
		}
	}
}
