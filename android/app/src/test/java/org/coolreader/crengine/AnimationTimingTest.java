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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

public class AnimationTimingTest {
	@Test
	public void rollingAveragePreservesLegacySamplingRules() {
		AnimationTiming timing = new AnimationTiming(3, 50);

		assertEquals(50, timing.averageDrawDuration());
		timing.recordDrawDuration(10);
		assertEquals(10, timing.averageDrawDuration());
		timing.recordDrawDuration(20);
		assertEquals(15, timing.averageDrawDuration());
		timing.recordDrawDuration(0);
		assertEquals(10, timing.averageDrawDuration());
		timing.recordDrawDuration(30);
		assertEquals(17, timing.averageDrawDuration());
		timing.recordDrawDuration(1_001);
		assertEquals(17, timing.averageDrawDuration());
	}

	@Test
	public void persistedAverageResetsTheWholeWindowSafely() {
		AnimationTiming timing = new AnimationTiming(3, 50);

		assertFalse(timing.hasSamples());
		timing.resetSamples(80);
		assertTrue(timing.hasSamples());
		assertEquals(80, timing.averageDrawDuration());
		timing.recordDrawDuration(20);
		assertEquals(60, timing.averageDrawDuration());
		timing.resetSamples(5_000);
		assertEquals(50, timing.averageDrawDuration());
	}

	@Test
	public void linearScrollStepsUseFractionalProgress() {
		assertEquals(
				0.25,
				AnimationTiming.scrollStep(1, 4, false),
				0.000_001);
		assertEquals(
				0.5,
				AnimationTiming.scrollStep(2, 4, false),
				0.000_001);
		assertEquals(
				0.75,
				AnimationTiming.scrollStep(3, 4, false),
				0.000_001);
	}

	@Test
	public void acceleratedScrollStepsAreBoundedAndMonotonic() {
		double previous = 0.0;
		for (int step = 0; step <= 8; step++) {
			double progress =
					AnimationTiming.scrollStep(step, 8, true);
			assertTrue(progress >= 0.0);
			assertTrue(progress <= 1.0);
			if (step > 0)
				assertTrue(progress > previous);
			previous = progress;
		}
		assertEquals(1.0, previous, 0.0);
		assertEquals(
				0.625,
				AnimationTiming.scrollStep(2, 4, true),
				0.000_001);
	}

	@Test
	public void autoscrollProgressMatchesNormalTimingAndClamps() {
		assertEquals(
				2_500,
				AnimationTiming.autoscrollProgress(1_500, 150, 1_500));
		assertEquals(
				10_000,
				AnimationTiming.autoscrollProgress(60_000, 150, 1_500));
		assertEquals(
				0,
				AnimationTiming.autoscrollProgress(-1, 150, 1_500));
		assertEquals(
				0,
				AnimationTiming.autoscrollProgress(1_000, 0, 1_500));
	}

	@Test
	public void autoscrollWidensCharacterDurationMultiplication() {
		assertEquals(
				5_000,
				AnimationTiming.autoscrollProgress(
						15_000_000L,
						100_000,
						200));
	}

	@Test
	public void invalidSampleConfigurationIsRejected() {
		assertRejected(0, 50);
		assertRejected(3, 0);
		assertRejected(3, 1_001);
	}

	private static void assertRejected(int sampleWindow, long initialAverage) {
		try {
			new AnimationTiming(sampleWindow, initialAverage);
			fail("invalid animation timing configuration was accepted");
		} catch (IllegalArgumentException expected) {
			// Expected.
		}
	}
}
