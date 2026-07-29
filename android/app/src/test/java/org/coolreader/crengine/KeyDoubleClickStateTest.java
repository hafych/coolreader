package org.coolreader.crengine;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class KeyDoubleClickStateTest {
	@Test
	public void matchingSecondPressConsumesDoubleAction() {
		KeyDoubleClickState<String> state =
				new KeyDoubleClickState<>();
		state.defer(7, 100, "single", "double");

		KeyDoubleClickState.PressResult<String> result =
				state.resolvePress(7, 499, 400);

		assertEquals("double", result.action());
		assertTrue(result.consumesPress());
		assertNull(state.resolvePress(7, 500, 400));
	}

	@Test
	public void differentKeyFlushesSingleWithoutConsumingPress() {
		KeyDoubleClickState<String> state =
				new KeyDoubleClickState<>();
		state.defer(7, 100, "single", "double");

		KeyDoubleClickState.PressResult<String> result =
				state.resolvePress(8, 200, 400);

		assertEquals("single", result.action());
		assertFalse(result.consumesPress());

		KeyDoubleClickState<String> overflow =
				new KeyDoubleClickState<>();
		overflow.defer(
				7,
				Long.MIN_VALUE,
				"single",
				"double");
		KeyDoubleClickState.PressResult<String> overflowResult =
				overflow.resolvePress(
						7,
						Long.MAX_VALUE,
						400);

		assertEquals("single", overflowResult.action());
		assertFalse(overflowResult.consumesPress());
	}

	@Test
	public void expiredOrRegressedClockFlushesSingle() {
		KeyDoubleClickState<String> expired =
				new KeyDoubleClickState<>();
		expired.defer(7, 100, "single", "double");
		KeyDoubleClickState.PressResult<String> boundary =
				expired.resolvePress(7, 500, 400);

		assertEquals("single", boundary.action());
		assertFalse(boundary.consumesPress());

		KeyDoubleClickState<String> regressed =
				new KeyDoubleClickState<>();
		regressed.defer(7, 100, "single", "double");
		KeyDoubleClickState.PressResult<String> result =
				regressed.resolvePress(7, 99, 400);

		assertEquals("single", result.action());
		assertFalse(result.consumesPress());
	}

	@Test
	public void staleTimerCannotClaimReplacementDecision() {
		KeyDoubleClickState<String> state =
				new KeyDoubleClickState<>();
		KeyDoubleClickState.Pending<String> stale =
				state.defer(7, 100, "first", "double-first");
		KeyDoubleClickState.Pending<String> current =
				state.defer(8, 200, "second", "double-second");

		assertNull(state.claimSingle(stale));
		assertEquals("second", state.claimSingle(current));
		assertNull(state.claimSingle(current));
	}

	@Test
	public void cancelAllowsAnotherDecision() {
		KeyDoubleClickState<String> state =
				new KeyDoubleClickState<>();
		KeyDoubleClickState.Pending<String> stale =
				state.defer(7, 100, "first", "double-first");

		state.cancel();
		assertNull(state.claimSingle(stale));

		KeyDoubleClickState.Pending<String> current =
				state.defer(8, 200, "second", "double-second");
		assertEquals("second", state.claimSingle(current));
	}

	@Test
	public void closePermanentlyRejectsDecisions() {
		KeyDoubleClickState<String> state =
				new KeyDoubleClickState<>();
		KeyDoubleClickState.Pending<String> pending =
				state.defer(7, 100, "single", "double");

		assertTrue(state.close());
		assertFalse(state.close());
		assertNull(state.claimSingle(pending));
		assertNull(state.defer(8, 200, "next", "double-next"));
		assertNull(state.resolvePress(8, 201, 400));
	}
}
