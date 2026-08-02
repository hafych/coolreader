package org.coolreader.crengine;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Pure install/close without constructing FileInfo (Android static init).
 */
public class ScannerRootStateTest {
	@Test
	public void installNullRejectedAndClosePermanent() {
		ScannerRootState state = new ScannerRootState();
		assertFalse(state.install(null));
		assertNull(state.get());
		assertTrue(state.close());
		assertTrue(state.isClosed());
		assertNull(state.get());
		assertFalse(state.install(null));
		assertFalse(state.close());
	}

	@Test(expected = IllegalStateException.class)
	public void requireThrowsWhenEmpty() {
		new ScannerRootState().require();
	}
}
