package org.coolreader.crengine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class TocItemChildrenStateTest {
	@Test
	public void addIndexOfAndClose() {
		TocItemChildrenState state = new TocItemChildrenState();
		TOCItem child = new TOCItem();
		assertEquals(0, state.add(child));
		assertEquals(-1, state.add(null));
		assertEquals(1, state.size());
		assertEquals(child, state.get(0));
		assertEquals(0, state.indexOf(child));
		assertEquals(1, state.snapshot().size());
		assertTrue(state.close());
		assertTrue(state.isClosed());
		assertEquals(-1, state.add(child));
		assertEquals(0, state.size());
		assertNull(state.get(0));
		assertEquals(-1, state.indexOf(child));
		assertFalse(state.close());
	}
}
