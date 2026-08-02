package org.coolreader.crengine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class EinkUpdateStateTest {
	@Test
	public void modeAndIntervalUntilClose() {
		EinkUpdateState state = new EinkUpdateState();
		assertEquals(EinkScreen.EinkUpdateMode.Clear, state.getMode());
		assertEquals(0, state.getInterval());

		state.setMode(EinkScreen.EinkUpdateMode.Active);
		state.setInterval(12);
		assertEquals(EinkScreen.EinkUpdateMode.Active, state.getMode());
		assertEquals(12, state.getInterval());

		state.setMode(null);
		assertEquals(EinkScreen.EinkUpdateMode.Active, state.getMode());

		assertTrue(state.close());
		assertTrue(state.isClosed());
		state.setMode(EinkScreen.EinkUpdateMode.Clear);
		state.setInterval(1);
		assertEquals(EinkScreen.EinkUpdateMode.Active, state.getMode());
		assertEquals(12, state.getInterval());
		assertFalse(state.close());
	}
}
