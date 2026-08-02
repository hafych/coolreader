package org.coolreader.crengine;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ShortcutModeStateTest {
	@Test
	public void setAndClose() {
		ShortcutModeState state = new ShortcutModeState();
		assertFalse(state.isShortcutMode());
		state.set(true);
		assertTrue(state.isShortcutMode());
		assertTrue(state.close());
		assertTrue(state.isClosed());
		state.set(false);
		// closed freezes last value for pure reads; isShortcutMode false when closed
		assertFalse(state.isShortcutMode());
		assertFalse(state.close());
	}
}
