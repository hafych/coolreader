package org.coolreader.crengine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class CoverSizeStateTest {
	@Test
	public void setAndClose() {
		CoverSizeState state = new CoverSizeState();
		state.set(120, 160);
		assertEquals(120, state.getWidth());
		assertEquals(160, state.getHeight());
		assertTrue(state.close());
		assertTrue(state.isClosed());
		state.set(1, 1);
		assertEquals(120, state.getWidth());
		assertEquals(160, state.getHeight());
		assertFalse(state.close());
	}
}
