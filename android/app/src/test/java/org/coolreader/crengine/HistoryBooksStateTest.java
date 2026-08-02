package org.coolreader.crengine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;

import org.junit.Test;

public class HistoryBooksStateTest {
	private static BookInfo book() {
		// Avoid FileInfo construction (Android static init in pure JVM).
		return new BookInfo((FileInfo) null);
	}

	@Test
	public void addMoveRemoveAndReplace() {
		HistoryBooksState state = new HistoryBooksState();
		BookInfo a = book();
		BookInfo b = book();

		state.addFirst(a);
		state.addFirst(b);
		assertEquals(2, state.size());
		assertSame(b, state.get(0));
		assertSame(a, state.get(1));

		assertSame(a, state.moveToFront(1));
		assertSame(a, state.get(0));
		assertSame(b, state.get(1));

		assertSame(b, state.removeAt(1));
		assertEquals(1, state.size());

		ArrayList<BookInfo> loaded = new ArrayList<>();
		loaded.add(b);
		loaded.add(a);
		state.replaceAll(loaded);
		assertEquals(2, state.size());
		assertSame(b, state.get(0));
		assertSame(a, state.get(1));

		assertEquals(2, state.copyAsArrayList().size());
		assertFalse(state.isEmpty());

		assertTrue(state.close());
		assertTrue(state.isClosed());
		assertEquals(0, state.size());
		assertTrue(state.isEmpty());
		assertNull(state.getRecentFolder());
		state.addFirst(a);
		assertEquals(0, state.size());
		assertFalse(state.close());
	}
}
