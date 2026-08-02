package org.coolreader.crengine;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class NightModeStateTest {
	@Test
	public void setAndClose() {
		NightModeState state = new NightModeState();
		assertFalse(state.isNightMode());
		state.set(true);
		assertTrue(state.isNightMode());
		state.set(false);
		assertFalse(state.isNightMode());

		state.set(true);
		assertTrue(state.close());
		assertTrue(state.isClosed());
		assertFalse(state.isNightMode());
		state.set(true);
		assertFalse(state.isNightMode());
		assertFalse(state.close());
	}
}
