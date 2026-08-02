package org.coolreader.plugins;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class AsyncOperationStateTest {
	@Test
	public void cancelFinishAndClose() {
		AsyncOperationState state = new AsyncOperationState();
		assertTrue(state.isActive());
		state.cancel();
		assertTrue(state.isCancelled());
		assertFalse(state.isActive());
		state.finished();
		assertTrue(state.isFinished());
		assertTrue(state.close());
		assertTrue(state.isClosed());
		assertFalse(state.isActive());
		assertFalse(state.close());
	}
}
