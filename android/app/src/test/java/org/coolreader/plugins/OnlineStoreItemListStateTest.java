package org.coolreader.plugins;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class OnlineStoreItemListStateTest {
	@Test
	public void addGetSortClose() {
		OnlineStoreItemListState<String> state =
				new OnlineStoreItemListState<>();
		state.add("b");
		state.add("a");
		assertEquals(2, state.size());
		assertEquals("b", state.get(0));
		state.sort(String::compareTo);
		assertEquals("a", state.get(0));
		assertEquals("b", state.get(1));
		assertTrue(state.close());
		assertTrue(state.isClosed());
		state.add("c");
		assertEquals(0, state.size());
		assertNull(state.get(0));
		assertFalse(state.close());
	}
}
