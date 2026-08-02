package org.coolreader.crengine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class SensorOrientationStateTest {
	@Test
	public void setAndClose() {
		SensorOrientationState state = new SensorOrientationState();
		assertEquals(0, state.get());
		state.set(1);
		assertEquals(1, state.get());
		assertTrue(state.close());
		assertTrue(state.isClosed());
		assertEquals(0, state.get());
		state.set(1);
		assertEquals(0, state.get());
		assertFalse(state.close());
	}
}
