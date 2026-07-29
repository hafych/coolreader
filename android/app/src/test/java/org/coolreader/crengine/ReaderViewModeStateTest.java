package org.coolreader.crengine;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ReaderViewModeStateTest {
	@Test
	public void overlappingLeasesRestoreOnlyAfterLastRelease() {
		ReaderViewModeState state = new ReaderViewModeState();

		ReaderViewModeState.Acquisition first =
				state.acquireScrollMode();
		assertNotNull(first);
		assertFalse(first.transition().isPageMode());
		assertFalse(state.isPageMode());

		ReaderViewModeState.Acquisition second =
				state.acquireScrollMode();
		assertNotNull(second);
		assertNull(second.transition());
		assertNull(state.release(first.lease()));
		assertFalse(state.isPageMode());

		ReaderViewModeState.Transition restored =
				state.release(second.lease());
		assertNotNull(restored);
		assertTrue(restored.isPageMode());
		assertTrue(state.isPageMode());
	}

	@Test
	public void scrollConfigurationNeedsNoTemporaryLease() {
		ReaderViewModeState state = new ReaderViewModeState();

		state.configure(false);

		assertNull(state.acquireScrollMode());
		assertFalse(state.isPageMode());
	}

	@Test
	public void configurationChangeInvalidatesOldLease() {
		ReaderViewModeState state = new ReaderViewModeState();
		ReaderViewModeState.Acquisition acquisition =
				state.acquireScrollMode();

		state.configure(false);

		assertNull(state.release(acquisition.lease()));
		assertFalse(state.isPageMode());

		state.configure(true);
		assertTrue(state.isPageMode());
	}

	@Test
	public void reapplyingConfigurationKeepsOutstandingLease() {
		ReaderViewModeState state = new ReaderViewModeState();
		ReaderViewModeState.Acquisition acquisition =
				state.acquireScrollMode();

		state.configure(true);

		assertFalse(state.snapshot().isPageMode());
		assertTrue(state.isConfiguredPageMode());
		assertNotNull(state.release(acquisition.lease()));
		assertTrue(state.isPageMode());
	}

	@Test
	public void resetRestoresConfiguredModeExactlyOnce() {
		ReaderViewModeState state = new ReaderViewModeState();
		ReaderViewModeState.Acquisition first =
				state.acquireScrollMode();
		ReaderViewModeState.Acquisition second =
				state.acquireScrollMode();

		ReaderViewModeState.Transition restored = state.reset();

		assertNotNull(restored);
		assertTrue(restored.isPageMode());
		assertNull(state.reset());
		assertNull(state.release(first.lease()));
		assertNull(state.release(second.lease()));
	}

	@Test
	public void closeRejectsNewAndOutstandingLeases() {
		ReaderViewModeState state = new ReaderViewModeState();
		ReaderViewModeState.Acquisition acquisition =
				state.acquireScrollMode();

		state.close();

		assertNull(state.acquireScrollMode());
		assertNull(state.release(acquisition.lease()));
	}
}
