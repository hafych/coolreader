package org.coolreader.crengine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;

import org.junit.Test;

public class ReaderActionListStateTest {
	@Test
	public void replaceAddSnapshotAndClose() {
		ReaderActionListState state = new ReaderActionListState();
		ReaderAction a = new ReaderAction(
				"a", 0, ReaderCommand.DCMD_NONE, 0);
		ReaderAction b = new ReaderAction(
				"b", 0, ReaderCommand.DCMD_NONE, 0);
		ArrayList<ReaderAction> source = new ArrayList<>();
		source.add(a);
		state.replaceAll(source);
		state.add(b);
		assertEquals(2, state.size());
		assertEquals(a, state.get(0));
		assertEquals(b, state.get(1));
		assertEquals(2, state.snapshot().size());
		assertEquals(2, state.copyAsArrayList().size());
		state.clear();
		assertEquals(0, state.size());
		assertTrue(state.close());
		assertTrue(state.isClosed());
		state.add(a);
		state.replaceAll(source);
		assertEquals(0, state.size());
		assertNull(state.get(0));
		assertFalse(state.close());
	}
}
