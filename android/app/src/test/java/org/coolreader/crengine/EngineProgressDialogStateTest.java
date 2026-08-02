package org.coolreader.crengine;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Pure ownership without constructing ProgressDialog (needs Activity).
 */
public class EngineProgressDialogStateTest {
	@Test
	public void takeAndClose() {
		EngineProgressDialogState state =
				new EngineProgressDialogState();
		assertNull(state.get());
		state.set(null);
		assertNull(state.get());
		assertNull(state.take());
		assertTrue(state.close() == null);
		assertTrue(state.isClosed());
		state.set(null);
		assertNull(state.get());
		assertNull(state.take());
		assertNull(state.close());
		assertFalse(state.close() != null);
	}
}
