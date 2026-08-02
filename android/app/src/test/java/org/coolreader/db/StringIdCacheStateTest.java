package org.coolreader.db;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class StringIdCacheStateTest {
	@Test
	public void putGetClearAndClose() {
		StringIdCacheState state = new StringIdCacheState();
		state.put("a", 1L);
		state.put(null, 2L);
		assertEquals(Long.valueOf(1L), state.get("a"));
		assertEquals(1, state.size());
		state.clear();
		assertEquals(0, state.size());
		state.put("b", 3L);
		assertTrue(state.close());
		assertTrue(state.isClosed());
		state.put("c", 4L);
		assertNull(state.get("b"));
		assertEquals(0, state.size());
		assertFalse(state.close());
	}
}
