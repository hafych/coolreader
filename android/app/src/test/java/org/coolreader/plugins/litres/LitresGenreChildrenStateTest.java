package org.coolreader.plugins.litres;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class LitresGenreChildrenStateTest {
	@Test
	public void addGetSnapshotAndClose() {
		LitresGenreChildrenState state =
				new LitresGenreChildrenState();
		LitresConnection.LitresGenre child =
				new LitresConnection.LitresGenre();
		child.id = "1";
		state.add(child);
		state.add(null);
		assertEquals(1, state.size());
		assertEquals(child, state.get(0));
		assertEquals(1, state.snapshot().size());
		assertTrue(state.close());
		assertTrue(state.isClosed());
		state.add(child);
		assertEquals(0, state.size());
		assertNull(state.get(0));
		assertEquals(0, state.snapshot().size());
		assertFalse(state.close());
	}
}
