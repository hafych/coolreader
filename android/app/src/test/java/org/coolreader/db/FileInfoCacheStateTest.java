package org.coolreader.db;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Pure cache ownership without constructing FileInfo (Android static init).
 */
public class FileInfoCacheStateTest {
	@Test
	public void emptyAndClose() {
		FileInfoCacheState state = new FileInfoCacheState(10);
		assertEquals(0, state.size());
		assertEquals(0, state.listSize());
		assertEquals(-1, state.findByPath("/a"));
		assertEquals(-1, state.findByBookKey("k"));
		assertEquals(-1, state.findById(1L));
		assertNull(state.getAt(0));
		state.clear();
		assertTrue(state.close());
		assertTrue(state.isClosed());
		state.add(null);
		assertEquals(0, state.size());
		assertFalse(state.close());
	}
}
