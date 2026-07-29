package org.coolreader.crengine;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class EinkRefreshLeaseTrackerTest {
	@Test
	public void firstAcquireDisablesAndLastReleaseRestores() {
		EinkRefreshLeaseTracker tracker =
				new EinkRefreshLeaseTracker();

		assertTrue(tracker.acquire(1, 6));
		assertTrue(tracker.isActive());
		assertEquals(Integer.valueOf(6), tracker.release(1));
		assertFalse(tracker.isActive());
	}

	@Test
	public void overlappingClientsRestoreOnlyAfterLastRelease() {
		EinkRefreshLeaseTracker tracker =
				new EinkRefreshLeaseTracker();

		assertTrue(tracker.acquire(1, 6));
		assertFalse(tracker.acquire(2, 0));
		assertNull(tracker.release(1));
		assertTrue(tracker.isActive());
		assertEquals(Integer.valueOf(6), tracker.release(2));
		assertFalse(tracker.isActive());
	}

	@Test
	public void duplicateAndUnmatchedTransitionsAreNoOps() {
		EinkRefreshLeaseTracker tracker =
				new EinkRefreshLeaseTracker();

		assertNull(tracker.release(99));
		assertTrue(tracker.acquire(1, 6));
		assertFalse(tracker.acquire(1, 99));
		assertNull(tracker.release(99));
		assertEquals(Integer.valueOf(6), tracker.release(1));
		assertNull(tracker.release(1));
	}

	@Test
	public void negativeIntervalsAreValuesRatherThanSentinels() {
		EinkRefreshLeaseTracker tracker =
				new EinkRefreshLeaseTracker();

		assertTrue(tracker.acquire(1, -1));
		assertFalse(tracker.acquire(2, 0));
		assertNull(tracker.release(1));
		assertEquals(Integer.valueOf(-1), tracker.release(2));

		assertTrue(tracker.acquire(3, 4));
		assertEquals(Integer.valueOf(4), tracker.release(3));
	}
}
