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

public class ReadingTimeTrackerTest {
	@Test
	public void pausedReadsAreIdempotent() {
		ReadingTimeTracker tracker = new ReadingTimeTracker();
		tracker.setElapsed(1_234);

		assertEquals(1_234, tracker.elapsed(10_000));
		assertEquals(1_234, tracker.elapsed(20_000));
		assertFalse(tracker.isRunning());
	}

	@Test
	public void repeatedLifecycleSignalsDoNotDoubleCount() {
		ReadingTimeTracker tracker = new ReadingTimeTracker();

		assertTrue(tracker.start(100));
		assertFalse(tracker.start(200));
		assertEquals(150, tracker.elapsed(250));
		assertTrue(tracker.stop(300));
		assertFalse(tracker.stop(400));
		assertEquals(200, tracker.elapsed(500));
	}

	@Test
	public void persistedBaselineCanBeReplacedDuringActiveSession() {
		ReadingTimeTracker tracker = new ReadingTimeTracker();
		tracker.start(100);

		tracker.setElapsed(1_000);
		assertEquals(1_050, tracker.elapsed(150));
		tracker.setElapsed(-10);
		assertEquals(60, tracker.elapsed(160));
	}

	@Test
	public void clockRegressionDoesNotSubtractReadingTime() {
		ReadingTimeTracker tracker = new ReadingTimeTracker();

		assertTrue(tracker.start(100));
		assertEquals(0, tracker.elapsed(90));
		assertTrue(tracker.stop(80));
		assertEquals(0, tracker.elapsed(1_000));
	}

	@Test
	public void elapsedTimeSaturatesInsteadOfOverflowing() {
		ReadingTimeTracker tracker = new ReadingTimeTracker();
		tracker.setElapsed(Long.MAX_VALUE - 5);
		tracker.start(10);

		assertEquals(Long.MAX_VALUE, tracker.elapsed(20));
		assertTrue(tracker.stop(20));
		assertEquals(Long.MAX_VALUE, tracker.elapsed(30));
	}

	@Test
	public void zeroTimestampIsValidAndNegativeStartIsRejected() {
		ReadingTimeTracker tracker = new ReadingTimeTracker();
		assertTrue(tracker.start(0));
		assertEquals(10, tracker.elapsed(10));

		try {
			new ReadingTimeTracker().start(-1);
			fail("negative reading timestamp was accepted");
		} catch (IllegalArgumentException expected) {
			// Expected.
		}
	}
}
