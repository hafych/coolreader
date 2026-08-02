package org.coolreader.crengine;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class FullscreenStateTest {
	@Test
	public void setAndClose() {
		FullscreenState state = new FullscreenState();
		assertFalse(state.isFullscreen());
		state.set(true);
		assertTrue(state.isFullscreen());
		assertTrue(state.close());
		assertTrue(state.isClosed());
		assertFalse(state.isFullscreen());
		state.set(true);
		assertFalse(state.isFullscreen());
		assertFalse(state.close());
	}
}
