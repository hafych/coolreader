/*
 * CoolReader for Android
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 2
 * of the License, or (at your option) any later version.
 */

package org.coolreader.crengine;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TapBounceStateTest {
	@Test
	public void firstTapIsNeverRejectedAtFreshBoot() {
		TapBounceState state = new TapBounceState();

		assertFalse(state.shouldReject(0, 150));
		assertFalse(state.shouldReject(149, 150));
	}

	@Test
	public void recordedTapUsesExactMonotonicBoundary() {
		TapBounceState state = new TapBounceState();
		assertTrue(state.recordTap(1_000));

		assertTrue(state.shouldReject(1_000, 150));
		assertTrue(state.shouldReject(1_149, 150));
		assertFalse(state.shouldReject(1_150, 150));
		assertFalse(state.shouldReject(1_001, 0));
	}

	@Test
	public void clockRegressionAndLargeValuesAreOverflowSafe() {
		TapBounceState state = new TapBounceState();
		assertTrue(state.recordTap(Long.MAX_VALUE - 100));

		assertFalse(
				state.shouldReject(
						Long.MAX_VALUE - 101,
						150));
		assertTrue(
				state.shouldReject(
						Long.MAX_VALUE,
						150));
	}

	@Test
	public void closeIsTerminalAndClearsBounceHistory() {
		TapBounceState state = new TapBounceState();
		assertTrue(state.recordTap(100));
		assertTrue(state.shouldReject(101, 150));

		assertTrue(state.close());
		assertFalse(state.close());
		assertFalse(state.shouldReject(101, 150));
		assertFalse(state.recordTap(200));
	}
}
