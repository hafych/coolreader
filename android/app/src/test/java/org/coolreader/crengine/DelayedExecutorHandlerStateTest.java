package org.coolreader.crengine;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

/**
 * Pure slot ownership without constructing android.os.Handler.
 */
public class DelayedExecutorHandlerStateTest {
	@Test
	public void nullEnsureRejectedAndCloseIsPermanent() {
		DelayedExecutorHandlerState state =
				new DelayedExecutorHandlerState();
		assertNull(state.get());
		try {
			state.ensure(null);
			fail("expected IllegalArgumentException");
		} catch (IllegalArgumentException expected) {
			// empty slot rejects null install
		}
		assertTrue(state.close());
		assertTrue(state.isClosed());
		assertNull(state.get());
		assertNull(state.ensure(null));
		assertFalse(state.close());
	}
}
