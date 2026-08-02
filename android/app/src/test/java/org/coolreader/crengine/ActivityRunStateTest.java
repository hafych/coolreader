package org.coolreader.crengine;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ActivityRunStateTest {
	@Test
	public void lifecycleTransitionsPublishStartedAndPaused() {
		ActivityRunState state = new ActivityRunState();

		assertFalse(state.isStarted());
		assertFalse(state.isPaused());

		state.onStart();
		assertTrue(state.isStarted());
		assertFalse(state.isPaused());

		state.onPause();
		assertFalse(state.isStarted());
		assertTrue(state.isPaused());

		state.onResume();
		assertTrue(state.isStarted());
		assertFalse(state.isPaused());

		state.onStop();
		assertFalse(state.isStarted());
		assertFalse(state.isPaused());
	}

	@Test
	public void closeIsPermanentAndClearsFlags() {
		ActivityRunState state = new ActivityRunState();
		state.onStart();

		assertTrue(state.close());
		assertTrue(state.isClosed());
		assertFalse(state.isStarted());
		assertFalse(state.isPaused());

		state.onStart();
		state.onPause();
		state.onResume();
		assertFalse(state.isStarted());
		assertFalse(state.isPaused());
		assertFalse(state.close());
	}
}
