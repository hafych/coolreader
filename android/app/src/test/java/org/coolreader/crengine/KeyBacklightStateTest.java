package org.coolreader.crengine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class KeyBacklightStateTest {
	@Test
	public void levelAndDisabledUntilClose() {
		KeyBacklightState state = new KeyBacklightState();
		assertEquals(1, state.getLevel());
		assertTrue(state.isDisabled());

		state.setLevel(0);
		state.setDisabled(false);
		assertEquals(0, state.getLevel());
		assertFalse(state.isDisabled());

		assertTrue(state.close());
		assertTrue(state.isClosed());
		assertFalse(state.isDisabled());
		state.setLevel(2);
		state.setDisabled(true);
		assertEquals(0, state.getLevel());
		assertFalse(state.isDisabled());
		assertFalse(state.close());
	}
}
