package org.coolreader.crengine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class EngineKeyBacklightStateTest {
	@Test
	public void setAndClose() {
		EngineKeyBacklightState state = new EngineKeyBacklightState();
		assertEquals(1, state.getLevel());
		state.setLevel(0);
		assertEquals(0, state.getLevel());
		assertTrue(state.close());
		assertTrue(state.isClosed());
		state.setLevel(2);
		assertEquals(0, state.getLevel());
		assertFalse(state.close());
	}
}
