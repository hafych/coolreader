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

import static org.junit.Assert.assertEquals;

public class AutoScrollSpeedStateTest {
	@Test
	public void startsAtLegacyDefaultAndClampsConfiguration() {
		AutoScrollSpeedState state =
				new AutoScrollSpeedState();

		assertEquals(1500, state.speed());
		assertEquals(200, state.configure(Integer.MIN_VALUE));
		assertEquals(10000, state.configure(Integer.MAX_VALUE));
		assertEquals(2750, state.configure(2750));
	}

	@Test
	public void exactLegacyTiersSelectStepMultiplier() {
		assertStep(299, 1, 309);
		assertStep(300, 1, 320);
		assertStep(499, 1, 519);
		assertStep(500, 1, 540);
		assertStep(999, 1, 1039);
		assertStep(1000, 1, 1080);
		assertStep(1999, 1, 2079);
		assertStep(2000, 1, 2200);
		assertStep(4999, 1, 5199);
		assertStep(5000, 1, 5300);
	}

	@Test
	public void directionUsesTierBeforeChange() {
		AutoScrollSpeedState state =
				new AutoScrollSpeedState();

		assertEquals(1580, state.change(1));
		assertEquals(1500, state.change(-1));
	}

	@Test
	public void extremeDeltasSaturateWithoutOverflow() {
		AutoScrollSpeedState state =
				new AutoScrollSpeedState();

		assertEquals(
				10000,
				state.change(Integer.MAX_VALUE));
		assertEquals(
				200,
				state.change(Integer.MIN_VALUE));
	}

	private static void assertStep(
			int initial,
			int delta,
			int expected) {
		AutoScrollSpeedState state =
				new AutoScrollSpeedState();
		state.configure(initial);
		assertEquals(expected, state.change(delta));
		assertEquals(expected, state.speed());
	}
}
