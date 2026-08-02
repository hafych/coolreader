package org.coolreader.crengine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class MountPathLinkListStateTest {
	@Test
	public void addSnapshotAndClose() {
		MountPathLinkListState<String> state =
				new MountPathLinkListState<>();
		state.add("a");
		state.add(null);
		assertEquals(1, state.size());
		assertEquals(1, state.snapshot().size());
		assertEquals("a", state.snapshot().get(0));
		assertTrue(state.close());
		assertTrue(state.isClosed());
		state.add("b");
		assertEquals(0, state.size());
		assertEquals(0, state.snapshot().size());
		assertFalse(state.close());
	}
}
