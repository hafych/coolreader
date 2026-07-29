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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

public class BitmapMemoryAccountingTest {
	@Test
	public void bitmapAccountingUsesActualRowStride() {
		assertEquals(
				4_096L * 2_160L,
				BitmapMemoryAccounting.bitmapBytes(4_096, 2_160));
	}

	@Test
	public void surfaceAccountingWidensBeforeMultiplication() {
		long bytes = BitmapMemoryAccounting.surfaceBytes(
				Integer.MAX_VALUE,
				Integer.MAX_VALUE);

		assertTrue(bytes > Integer.MAX_VALUE);
		assertEquals(
				(long) Integer.MAX_VALUE * Integer.MAX_VALUE * 2L,
				bytes);
	}

	@Test
	public void negativeDimensionsAreRejected() {
		assertRejected(() -> BitmapMemoryAccounting.bitmapBytes(-1, 10));
		assertRejected(() -> BitmapMemoryAccounting.surfaceBytes(10, -1));
	}

	private static void assertRejected(Runnable operation) {
		try {
			operation.run();
			fail("negative dimension was accepted");
		} catch (IllegalArgumentException expected) {
			// Expected.
		}
	}
}
