/*
 * CoolReader for Android
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 2
 * of the License, or (at your option) any later version.
 */

package org.coolreader.crengine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;

public class GestureAccelerationTest {
	@Test
	public void legacyCurveMatchesKnownSamples() {
		GestureAcceleration curve = GestureAcceleration.legacy();

		assertEquals(0, curve.apply(0, 1_000, 0));
		assertEquals(146, curve.apply(0, 1_000, 250));
		assertEquals(176, curve.apply(0, 1_000, 275));
		assertEquals(500, curve.apply(0, 1_000, 500));
		assertEquals(853, curve.apply(0, 1_000, 750));
		assertEquals(1_000, curve.apply(0, 1_000, 1_000));
	}

	@Test
	public void inputIsClampedAndDegenerateRangeIsStable() {
		GestureAcceleration curve = GestureAcceleration.legacy();

		assertEquals(100, curve.apply(100, 1_100, -500));
		assertEquals(1_100, curve.apply(100, 1_100, 5_000));
		assertEquals(17, curve.apply(17, 17, 900));
		assertEquals(20, curve.apply(20, 10, 15));
	}

	@Test
	public void fullIntegerRangeUsesWidenedArithmetic() {
		GestureAcceleration curve = GestureAcceleration.legacy();

		assertEquals(
				-1,
				curve.apply(
						Integer.MIN_VALUE,
						Integer.MAX_VALUE,
						0));
	}

	@Test
	public void constructorCopiesAndValidatesShape() {
		int[] input = {0, 500, 1000};
		GestureAcceleration curve = new GestureAcceleration(input);
		input[1] = 1;
		assertEquals(500, curve.apply(0, 1_000, 500));

		assertRejected(null);
		assertRejected(new int[]{0});
		assertRejected(new int[]{1, 1000});
		assertRejected(new int[]{0, 600, 500, 1000});
		assertRejected(new int[]{0, 1001, 1000});
	}

	private static void assertRejected(int[] shape) {
		try {
			new GestureAcceleration(shape);
			fail("invalid acceleration shape was accepted");
		} catch (IllegalArgumentException expected) {
			// Expected.
		}
	}
}
