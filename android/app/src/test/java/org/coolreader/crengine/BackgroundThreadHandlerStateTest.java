package org.coolreader.crengine;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Pure slot ownership without constructing android.os.Handler.
 */
public class BackgroundThreadHandlerStateTest {
	@Test
	public void setTakeAndClose() {
		BackgroundThreadHandlerState state =
				new BackgroundThreadHandlerState();
		assertNull(state.get());
		state.set(null);
		assertNull(state.get());
		assertNull(state.take());
		assertTrue(state.close());
		assertTrue(state.isClosed());
		state.set(null);
		assertNull(state.get());
		assertNull(state.take());
		assertFalse(state.close());
	}
}
