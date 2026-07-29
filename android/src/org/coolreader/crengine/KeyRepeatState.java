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
 * Owns one hardware-key press and its in-flight repeat action by identity.
 */
final class KeyRepeatState<T> {
	private Press<T> current;
	private boolean repeatInFlight;
	private boolean closed;

	synchronized Press<T> begin(
			int keyCode, long downTime, T repeatAction) {
		if (closed)
			return null;
		current = new Press<>(
				keyCode, downTime, repeatAction);
		repeatInFlight = false;
		return current;
	}

	synchronized Repeat<T> startRepeat(Press<T> press) {
		if (closed
				|| press == null
				|| current != press
				|| press.repeatAction == null
				|| repeatInFlight)
			return null;
		repeatInFlight = true;
		return new Repeat<>(press, press.repeatAction);
	}

	synchronized RepeatEvent<T> repeat(
			int keyCode,
			long downTime,
			long eventTime,
			long downTimeTolerance,
			long longPressTime) {
		if (closed || !matches(
				current,
				keyCode,
				downTime,
				downTimeTolerance)) {
			clear();
			return new RepeatEvent<>(
					false, false, false, null);
		}
		boolean longPress = elapsedAtLeast(
				current.downTime,
				eventTime,
				longPressTime);
		boolean hasRepeatAction =
				current.repeatAction != null;
		Repeat<T> repeat = null;
		if (longPress
				&& hasRepeatAction
				&& !repeatInFlight) {
			repeatInFlight = true;
			repeat = new Repeat<>(
					current,
					current.repeatAction);
		}
		return new RepeatEvent<>(
				true,
				longPress,
				hasRepeatAction,
				repeat);
	}

	synchronized boolean completeRepeat(Repeat<T> repeat) {
		if (closed
				|| repeat == null
				|| current != repeat.press
				|| !repeatInFlight)
			return false;
		repeatInFlight = false;
		return true;
	}

	synchronized Release release(
			int keyCode,
			long downTime,
			long eventTime,
			long downTimeTolerance,
			long longPressTime) {
		Press<T> press = current;
		boolean tracked =
				!closed
						&& matches(
								press,
								keyCode,
								downTime,
								downTimeTolerance);
		boolean longPress =
				tracked
						&& elapsedAtLeast(
								press.downTime,
								eventTime,
								longPressTime);
		clear();
		return new Release(tracked, longPress);
	}

	synchronized void cancel() {
		clear();
	}

	synchronized boolean close() {
		if (closed)
			return false;
		closed = true;
		clear();
		return true;
	}

	private void clear() {
		current = null;
		repeatInFlight = false;
	}

	private static boolean matches(
			Press<?> press,
			int keyCode,
			long downTime,
			long tolerance) {
		if (press == null
				|| tolerance <= 0
				|| press.keyCode != keyCode
				|| downTime < press.downTime)
			return false;
		if (press.downTime > Long.MAX_VALUE - tolerance)
			return true;
		return downTime < press.downTime + tolerance;
	}

	private static boolean elapsedAtLeast(
			long startedAt, long now, long interval) {
		if (interval <= 0
				|| now < startedAt
				|| startedAt > Long.MAX_VALUE - interval)
			return false;
		return now >= startedAt + interval;
	}

	static final class Press<T> {
		private final int keyCode;
		private final long downTime;
		private final T repeatAction;

		private Press(
				int keyCode,
				long downTime,
				T repeatAction) {
			this.keyCode = keyCode;
			this.downTime = downTime;
			this.repeatAction = repeatAction;
		}
	}

	static final class Repeat<T> {
		private final Press<T> press;
		private final T action;

		private Repeat(Press<T> press, T action) {
			this.press = press;
			this.action = action;
		}

		T action() {
			return action;
		}
	}

	static final class RepeatEvent<T> {
		private final boolean tracked;
		private final boolean longPress;
		private final boolean hasRepeatAction;
		private final Repeat<T> repeat;

		private RepeatEvent(
				boolean tracked,
				boolean longPress,
				boolean hasRepeatAction,
				Repeat<T> repeat) {
			this.tracked = tracked;
			this.longPress = longPress;
			this.hasRepeatAction = hasRepeatAction;
			this.repeat = repeat;
		}

		boolean isTracked() {
			return tracked;
		}

		boolean isLongPress() {
			return longPress;
		}

		boolean hasRepeatAction() {
			return hasRepeatAction;
		}

		Repeat<T> repeat() {
			return repeat;
		}
	}

	static final class Release {
		private final boolean tracked;
		private final boolean longPress;

		private Release(boolean tracked, boolean longPress) {
			this.tracked = tracked;
			this.longPress = longPress;
		}

		boolean isTracked() {
			return tracked;
		}

		boolean isLongPress() {
			return longPress;
		}
	}
}
