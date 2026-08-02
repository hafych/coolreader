package org.coolreader.crengine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Pure list ownership without constructing EntryInfo (heavy OPDS types).
 */
public class OpdsEntryListStateTest {
	@Test
	public void emptyAddLimitClose() {
		OpdsEntryListState state = new OpdsEntryListState();
		assertEquals(0, state.size());
		assertFalse(state.add(null, 10));
		assertEquals(0, state.copyAsArrayList().size());
		assertTrue(state.close());
		assertTrue(state.isClosed());
		assertFalse(state.add(null, 10));
		assertEquals(0, state.size());
		assertFalse(state.close());
	}
}
