package org.coolreader.crengine;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class TouchScreenLockStateTest {
	@Test
	public void toggleFlipsEnabledState() {
		TouchScreenLockState state = new TouchScreenLockState();

		assertTrue(state.isEnabled());
		assertFalse(state.toggle());
		assertFalse(state.isEnabled());
		assertTrue(state.toggle());
		assertTrue(state.isEnabled());
	}

	@Test
	public void closeDisablesTouchAndIsPermanent() {
		TouchScreenLockState state = new TouchScreenLockState();
		assertTrue(state.isEnabled());

		assertTrue(state.close());
		assertFalse(state.isEnabled());
		assertFalse(state.toggle());
		assertFalse(state.isEnabled());
		assertTrue(state.isClosed());
		assertFalse(state.close());
	}
}
