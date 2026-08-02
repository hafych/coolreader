package org.coolreader.crengine;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import android.view.View;

import org.junit.Test;

public class ContentViewStateTest {
	@Test
	public void contentAndDecorUntilClose() {
		ContentViewState state = new ContentViewState();
		// View cannot be constructed without Android; null install/clear
		// and permanent close still drive the shipped owner.
		assertNull(state.getContentView());
		assertNull(state.getDecorView());

		state.setContentView(null);
		state.setDecorView(null);
		assertNull(state.getContentView());
		assertNull(state.getDecorView());

		assertTrue(state.close());
		assertTrue(state.isClosed());
		assertNull(state.getContentView());
		assertNull(state.getDecorView());
		assertFalse(state.close());
	}
}
