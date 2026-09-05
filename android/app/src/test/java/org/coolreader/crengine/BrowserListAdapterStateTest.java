package org.coolreader.crengine;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Pure adapter slot ownership without constructing adapter (needs Activity).
 */
public class BrowserListAdapterStateTest {
	@Test
	public void installGetAndClose() {
		BrowserListAdapterState state = new BrowserListAdapterState();
		assertNull(state.get());
		assertNull(state.install(null));
		assertNull(state.get());
		assertNull(state.close());
		assertTrue(state.isClosed());
		assertNull(state.get());
		assertNull(state.close());
		assertFalse(!state.isClosed());
		// Rejected after close.
		assertNull(state.install(null));
	}
}
