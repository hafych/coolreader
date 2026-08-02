package org.coolreader.crengine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;

import org.junit.Test;

public class BookInfoBookmarksStateTest {
	private static Bookmark bm(String keyHint) {
		Bookmark b = new Bookmark();
		// unique key comes from type+positions; set type comment and pos text
		b.setType(Bookmark.TYPE_COMMENT);
		b.setStartPos(keyHint);
		b.setEndPos(keyHint);
		return b;
	}

	@Test
	public void addFindRemoveAndReplace() {
		BookInfoBookmarksState state = new BookInfoBookmarksState();
		Bookmark a = bm("a");
		Bookmark b = bm("b");
		state.add(a);
		state.add(b);
		assertEquals(2, state.size());
		assertSame(a, state.get(0));
		assertEquals(0, state.findIndex(a));
		assertEquals(1, state.findIndex(b));
		assertSame(b, state.remove(1));
		assertEquals(1, state.size());

		ArrayList<Bookmark> list = new ArrayList<>();
		list.add(b);
		list.add(a);
		state.replaceAll(list);
		assertEquals(2, state.size());
		assertSame(b, state.get(0));
		assertEquals(2, state.copyAsArrayList().size());
		state.clear();
		assertEquals(0, state.size());
		assertNull(state.findIndex(a) >= 0 ? a : null);
		assertTrue(state.findIndex(a) < 0);
	}
}
