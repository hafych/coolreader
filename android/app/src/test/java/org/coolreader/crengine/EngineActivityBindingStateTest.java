package org.coolreader.crengine;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Pure attach/clear/close without constructing a BaseActivity.
 */
public class EngineActivityBindingStateTest {
	@Test
	public void nullAttachRejectedAndClosePermanent() {
		EngineActivityBindingState state = new EngineActivityBindingState();
		assertFalse(state.attach(null));
		assertNull(state.get());
		assertFalse(state.isAttachedTo(null));
		assertFalse(state.clearIf(null));
		assertTrue(state.close());
		assertTrue(state.isClosed());
		assertNull(state.get());
		assertFalse(state.attach(null));
		assertFalse(state.close());
	}
}
