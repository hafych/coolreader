package org.coolreader.crengine;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class NoticeDialogStateTest {
	@Test
	public void beginShowIsExclusiveUntilEndShow() {
		NoticeDialogState state = new NoticeDialogState();

		assertTrue(state.beginShow());
		assertTrue(state.isVisible());
		assertFalse(state.beginShow());
		state.endShow();
		assertFalse(state.isVisible());
		assertTrue(state.beginShow());
	}

	@Test
	public void closeIsPermanent() {
		NoticeDialogState state = new NoticeDialogState();
		assertTrue(state.beginShow());

		assertTrue(state.close());
		assertTrue(state.isClosed());
		assertFalse(state.isVisible());
		assertFalse(state.beginShow());
		state.endShow();
		assertFalse(state.isVisible());
		assertFalse(state.close());
	}
}
