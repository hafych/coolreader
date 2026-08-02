package org.coolreader.crengine;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Pure slot ownership without constructing MotionWatchdogHandler.
 */
public class MotionWatchdogSlotStateTest {
	@Test
	public void takeAndCloseWithoutHandler() {
		MotionWatchdogSlotState state =
				new MotionWatchdogSlotState();
		assertNull(state.get());
		assertNull(state.take());
		assertNull(state.install(null));
		assertNull(state.get());
		assertTrue(state.close());
		assertTrue(state.isClosed());
		// after close, install returns the rejected candidate for cleanup
		assertNull(state.install(null));
		assertNull(state.take());
		assertFalse(state.close());
	}
}
