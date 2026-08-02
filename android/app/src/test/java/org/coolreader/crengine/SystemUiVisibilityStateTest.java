package org.coolreader.crengine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class SystemUiVisibilityStateTest {
	@Test
	public void cacheAndListenerUntilClose() {
		SystemUiVisibilityState state = new SystemUiVisibilityState();
		assertEquals(-1, state.getLastVisibility());
		assertFalse(state.isListenerSet());

		assertTrue(state.markListenerSet());
		assertTrue(state.isListenerSet());
		assertFalse(state.markListenerSet());

		state.setLastVisibility(7);
		assertEquals(7, state.getLastVisibility());
		state.invalidateCache();
		assertEquals(-1, state.getLastVisibility());
		assertTrue(state.isListenerSet());

		assertTrue(state.close());
		assertTrue(state.isClosed());
		assertFalse(state.isListenerSet());
		state.setLastVisibility(3);
		assertEquals(-1, state.getLastVisibility());
		assertFalse(state.markListenerSet());
		assertFalse(state.close());
	}
}
