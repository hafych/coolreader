package org.coolreader.crengine;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

/**
 * Pure slot ownership without constructing HandlerThread.
 */
public class WordTimingCalcHandlerStateTest {
	@Test
	public void nullEnsureRejectedAndCloseIsPermanent() {
		WordTimingCalcHandlerState state =
				new WordTimingCalcHandlerState();
		assertNull(state.get());
		assertNull(state.takeRunning());
		try {
			state.ensure(null, null);
			fail("expected IllegalArgumentException");
		} catch (IllegalArgumentException expected) {
			// empty slot rejects null install
		}
		assertTrue(state.close());
		assertTrue(state.isClosed());
		assertNull(state.get());
		assertNull(state.ensure(null, null));
		assertNull(state.takeRunning());
		assertFalse(state.close());
	}
}
