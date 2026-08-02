package org.coolreader.crengine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ScreenBrightnessStateTest {
	@Test
	public void coldWarmAndHackErrorUntilClose() {
		ScreenBrightnessState state = new ScreenBrightnessState(true);
		assertEquals(-1, state.getColdLevel());
		assertEquals(-1, state.getWarmLevel());
		assertTrue(state.isBrightnessHackError());

		state.setColdLevel(40);
		state.setWarmLevel(20);
		state.setBrightnessHackError(false);
		assertEquals(40, state.getColdLevel());
		assertEquals(20, state.getWarmLevel());
		assertFalse(state.isBrightnessHackError());

		assertTrue(state.close());
		assertTrue(state.isClosed());
		state.setColdLevel(10);
		assertEquals(40, state.getColdLevel());
		assertFalse(state.close());
	}
}
