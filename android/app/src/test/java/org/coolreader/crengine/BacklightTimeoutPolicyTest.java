package org.coolreader.crengine;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BacklightTimeoutPolicyTest {
	@Test
	public void dimmingStartsStrictlyAfterEightyPercent() {
		assertFalse(BacklightTimeoutPolicy.shouldDim(800, 1_000));
		assertEquals(
				50,
				BacklightTimeoutPolicy.nextCheckDelay(800, 1_000));

		assertTrue(BacklightTimeoutPolicy.shouldDim(801, 1_000));
		assertEquals(
				6,
				BacklightTimeoutPolicy.nextCheckDelay(801, 1_000));
	}

	@Test
	public void expiryStartsStrictlyAfterFullDuration() {
		assertFalse(BacklightTimeoutPolicy.isExpired(1_000, 1_000));
		assertTrue(BacklightTimeoutPolicy.isExpired(1_001, 1_000));
		assertTrue(BacklightTimeoutPolicy.isExpired(0, 0));
		assertEquals(0, BacklightTimeoutPolicy.nextCheckDelay(0, 0));
	}

	@Test
	public void thresholdArithmeticDoesNotOverflow() {
		int duration = Integer.MAX_VALUE;
		long dimThreshold = duration * 8L / 10;

		assertFalse(
				BacklightTimeoutPolicy.shouldDim(
						dimThreshold,
						duration));
		assertTrue(
				BacklightTimeoutPolicy.shouldDim(
						dimThreshold + 1,
						duration));
		assertTrue(
				BacklightTimeoutPolicy.nextCheckDelay(
						dimThreshold + 1,
						duration) > 0);
		assertTrue(
				BacklightTimeoutPolicy.isExpired(
						(long) duration + 1,
						duration));
	}
}
