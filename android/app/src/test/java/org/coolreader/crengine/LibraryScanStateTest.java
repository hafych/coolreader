package org.coolreader.crengine;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class LibraryScanStateTest {
	@Test
	public void tenThousandEntriesFitExactlyWithinBudget() {
		LibraryScanState state = new LibraryScanState(10_000, 256);
		for (int i = 0; i < 10_000; i++)
			assertTrue(state.recordEntry(i % 100 == 0));

		assertFalse(state.isStopped());
		assertEquals(10_000, state.getDiscoveredEntries());
		assertFalse(state.recordEntry(false));
		assertEquals(
				ScanStopReason.ENTRY_LIMIT,
				state.getStopReason());
	}

	@Test
	public void firstStopReasonIsStable() {
		LibraryScanState state = new LibraryScanState(1, 2);
		state.stopAtDepthLimit();
		state.stopByUser();

		assertTrue(state.isStopped());
		assertEquals(
				ScanStopReason.DEPTH_LIMIT,
				state.getStopReason());
		assertEquals(2, state.getMaxDepth());
	}

	@Test
	public void discoveryProgressNeverMovesBackward() {
		LibraryScanState state = new LibraryScanState(10, 4);
		state.startRootDirectory();
		state.completeDirectory();
		assertEquals(3_000, state.discoveryProgress(3_000));

		assertTrue(state.recordEntry(true));
		assertEquals(3_000, state.discoveryProgress(3_000));
	}
}
