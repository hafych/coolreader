package org.coolreader.sync2;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class SyncServiceBindingStateTest {
	@Test
	public void bindFlagsAndClose() {
		SyncServiceBindingState state = new SyncServiceBindingState();
		assertFalse(state.isReady());
		assertFalse(state.isServiceBound());
		state.setBindCalled(true);
		state.setServiceBound(true);
		assertTrue(state.isBindCalled());
		assertTrue(state.isServiceBound());
		// binder null => not ready
		assertFalse(state.isReady());
		state.unbind();
		assertFalse(state.isServiceBound());
		assertFalse(state.isBindCalled());
		assertNull(state.getBinder());
		assertTrue(state.close());
		assertTrue(state.isClosed());
		state.setServiceBound(true);
		assertFalse(state.isServiceBound());
		assertFalse(state.close());
	}
}
