package org.coolreader.crengine;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Pure progress slot ownership without constructing ProgressPopup (needs
 * Activity).
 */
public class BrowserProgressStateTest {
	@Test
	public void setShowHideAndClose() {
		BrowserProgressState state = new BrowserProgressState();
		assertNull(state.get());
		state.set(null);
		assertNull(state.get());
		// No popup: show/hide are safe no-ops.
		state.show();
		state.hide();
		assertNull(state.close());
		assertTrue(state.isClosed());
		state.set(null);
		assertNull(state.get());
		state.show();
		state.hide();
		assertNull(state.close());
		assertFalse(!state.isClosed());
	}
}
