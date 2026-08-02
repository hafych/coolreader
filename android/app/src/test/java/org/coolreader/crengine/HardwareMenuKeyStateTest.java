package org.coolreader.crengine;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class HardwareMenuKeyStateTest {
	@Test
	public void lazyResolveAndClose() {
		HardwareMenuKeyState state = new HardwareMenuKeyState();
		assertNull(state.get());
		assertFalse(state.isResolved());

		state.set(true);
		assertTrue(state.isResolved());
		assertTrue(state.get());

		assertTrue(state.close());
		assertTrue(state.isClosed());
		assertNull(state.get());
		assertFalse(state.isResolved());
		state.set(false);
		assertNull(state.get());
		assertFalse(state.close());
	}
}
