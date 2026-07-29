package org.coolreader.crengine;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class KeyRepeatStateTest {
	@Test
	public void initialAndRepeatedActionsCompleteByExactIdentity() {
		KeyRepeatState<String> state =
				new KeyRepeatState<>();
		KeyRepeatState.Press<String> press =
				state.begin(7, 100, "page-down");
		KeyRepeatState.Repeat<String> initial =
				state.startRepeat(press);

		assertSame("page-down", initial.action());
		KeyRepeatState.RepeatEvent<String> inFlight =
				state.repeat(7, 100, 800, 300, 700);
		assertTrue(inFlight.isTracked());
		assertTrue(inFlight.isLongPress());
		assertTrue(inFlight.hasRepeatAction());
		assertNull(inFlight.repeat());

		assertTrue(state.completeRepeat(initial));
		KeyRepeatState.RepeatEvent<String> repeated =
				state.repeat(7, 100, 801, 300, 700);
		assertSame("page-down", repeated.repeat().action());
		assertTrue(state.completeRepeat(repeated.repeat()));
		assertFalse(state.completeRepeat(repeated.repeat()));
	}

	@Test
	public void staleCompletionCannotReleaseReplacementPress() {
		KeyRepeatState<String> state =
				new KeyRepeatState<>();
		KeyRepeatState.Repeat<String> stale =
				state.startRepeat(
						state.begin(7, 100, "first"));
		KeyRepeatState.Repeat<String> replacement =
				state.startRepeat(
						state.begin(8, 200, "second"));

		assertFalse(state.completeRepeat(stale));
		assertTrue(state.completeRepeat(replacement));
		KeyRepeatState.RepeatEvent<String> next =
				state.repeat(8, 200, 900, 300, 700);
		assertSame("second", next.repeat().action());
	}

	@Test
	public void downTimeToleranceIsBoundedAndRejectsRegression() {
		KeyRepeatState<String> state =
				new KeyRepeatState<>();
		state.begin(7, 100, "repeat");

		assertTrue(state.repeat(
				7, 399, 800, 300, 700).isTracked());

		state.begin(7, 100, "repeat");
		assertFalse(state.repeat(
				7, 400, 800, 300, 700).isTracked());

		state.begin(7, 100, "repeat");
		assertFalse(state.repeat(
				7, 99, 800, 300, 700).isTracked());

		state.begin(7, 100, "repeat");
		assertFalse(state.repeat(
				8, 100, 800, 300, 700).isTracked());
	}

	@Test
	public void releaseUsesMonotonicEventTimeAndClearsPress() {
		KeyRepeatState<String> state =
				new KeyRepeatState<>();
		state.begin(7, 100, null);

		KeyRepeatState.Release shortRelease =
				state.release(
						7, 100, 999, 300, 900);
		assertTrue(shortRelease.isTracked());
		assertFalse(shortRelease.isLongPress());
		assertFalse(state.release(
				7, 100, 1000, 300, 900).isTracked());

		state.begin(7, 100, null);
		KeyRepeatState.Release longRelease =
				state.release(
						7, 100, 1000, 300, 900);
		assertTrue(longRelease.isTracked());
		assertTrue(longRelease.isLongPress());
	}

	@Test
	public void clockRegressionAndOverflowCannotForgeLongPress() {
		KeyRepeatState<String> state =
				new KeyRepeatState<>();
		state.begin(7, 100, null);
		assertFalse(state.release(
				7, 100, 99, 300, 900).isLongPress());

		state.begin(
				7, Long.MAX_VALUE - 100, null);
		assertFalse(state.release(
				7,
				Long.MAX_VALUE - 100,
				Long.MAX_VALUE,
				300,
				900).isLongPress());
	}

	@Test
	public void cancelAllowsReuseAndCloseRejectsNewPresses() {
		KeyRepeatState<String> state =
				new KeyRepeatState<>();
		KeyRepeatState.Press<String> stale =
				state.begin(7, 100, "first");

		state.cancel();
		assertNull(state.startRepeat(stale));
		assertTrue(state.startRepeat(
				state.begin(8, 200, "second")) != null);

		assertTrue(state.close());
		assertFalse(state.close());
		assertNull(state.begin(9, 300, "third"));
		assertFalse(state.repeat(
				9, 300, 1000, 300, 700).isTracked());
		assertFalse(state.release(
				9, 300, 1000, 300, 900).isTracked());
	}
}
