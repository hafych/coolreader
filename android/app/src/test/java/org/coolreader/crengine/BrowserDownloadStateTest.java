package org.coolreader.crengine;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Pure download slot ownership without constructing DownloadTask (needs
 * Activity/Engine).
 */
public class BrowserDownloadStateTest {
	@Test
	public void installClearIfTakeAndClose() {
		BrowserDownloadState state = new BrowserDownloadState();
		assertNull(state.get());
		assertTrue(state.clearIf(null) == false);
		// Null task identity still exercises the slot contract.
		assertNull(state.install(null));
		assertTrue(state.clearIf(null) == false);
		assertNull(state.take());
		assertNull(state.close());
		assertTrue(state.isClosed());
		assertNull(state.get());
		assertNull(state.take());
		assertNull(state.close());
		assertFalse(state.isClosed() == false);
	}
}
