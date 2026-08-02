package org.coolreader.crengine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Pure list ownership without constructing OptionBase (needs Android UI).
 */
public class OptionsListStateTest {
	@Test
	public void emptyAddRemoveClose() {
		OptionsListState state = new OptionsListState();
		assertEquals(0, state.size());
		assertNull(state.get(0));
		assertFalse(state.remove(0));
		assertFalse(state.remove((OptionsDialog.OptionBase) null));
		state.clear();
		assertTrue(state.close());
		assertTrue(state.isClosed());
		state.add(null);
		assertEquals(0, state.size());
		assertFalse(state.close());
	}
}
