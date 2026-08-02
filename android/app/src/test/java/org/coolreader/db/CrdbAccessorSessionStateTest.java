package org.coolreader.db;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Pure session ownership without constructing ServiceConnection (Android).
 */
public class CrdbAccessorSessionStateTest {
	@Test
	public void pathCorrectorAndClose() {
		CrdbAccessorSessionState state =
				new CrdbAccessorSessionState(null);
		assertNull(state.getPathCorrector());
		assertNull(state.getBinding());
		state.setPathCorrector(null);
		assertNull(state.takeBinding());
		assertTrue(state.close() == null);
		assertTrue(state.isClosed());
		state.setPathCorrector(null);
		state.setBinding(null);
		assertNull(state.getPathCorrector());
		assertNull(state.getBinding());
		assertNull(state.close());
		assertFalse(state.close() != null);
	}
}
