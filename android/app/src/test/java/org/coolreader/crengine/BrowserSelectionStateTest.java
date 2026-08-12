package org.coolreader.crengine;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Pure selection slot ownership without constructing FileInfo.
 */
public class BrowserSelectionStateTest {
	@Test
	public void setAndClose() {
		BrowserSelectionState state = new BrowserSelectionState();
		state.set(null);
		assertNull(state.get());
		assertTrue(state.close());
		assertTrue(state.isClosed());
		state.set(null);
		assertNull(state.get());
		assertFalse(state.close());
	}
}
