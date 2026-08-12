package org.coolreader.crengine;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Pure view-policy ownership. Avoids SortOrder enum values so the class can
 * load without Android-backed FileInfo static work beyond the default field
 * initializer path used by production.
 */
public class BrowserViewOptionsStateTest {
	@Test
	public void hideInitSimpleUntilClose() {
		BrowserViewOptionsState state = new BrowserViewOptionsState();
		assertFalse(state.isHideEmptyGenres());
		assertTrue(state.isSimpleViewMode());
		assertFalse(state.isInitStarted());

		state.setHideEmptyGenres(true);
		assertTrue(state.isHideEmptyGenres());
		assertTrue(state.beginInit());
		assertTrue(state.isInitStarted());
		assertFalse(state.beginInit());

		assertTrue(state.setSimpleViewMode(false));
		assertFalse(state.isSimpleViewMode());
		assertFalse(state.setSimpleViewMode(false));

		assertTrue(state.close());
		assertTrue(state.isClosed());
		state.setHideEmptyGenres(false);
		assertTrue(state.isHideEmptyGenres());
		assertFalse(state.setSimpleViewMode(true));
		assertFalse(state.beginInit());
		assertFalse(state.close());
	}
}
