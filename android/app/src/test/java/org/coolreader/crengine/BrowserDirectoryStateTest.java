package org.coolreader.crengine;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Pure directory slot ownership without constructing FileInfo (Android static
 * init).
 */
public class BrowserDirectoryStateTest {
	@Test
	public void setAndClose() {
		BrowserDirectoryState state = new BrowserDirectoryState();
		BrowserDirectoryState.Change first =
				state.setReturningPrevious(null);
		assertTrue(first.accepted);
		assertNull(first.previous);
		assertNull(first.current);
		assertFalse(first.directoryChanged());

		assertTrue(state.close());
		assertTrue(state.isClosed());
		assertNull(state.get());
		BrowserDirectoryState.Change rejected =
				state.setReturningPrevious(null);
		assertFalse(rejected.accepted);
		assertFalse(state.close());
	}
}
