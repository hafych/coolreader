package org.coolreader;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class LibraryRootStoreStateTest {
	@Test
	public void installOnceAndClosePermanently() {
		LibraryRootStoreState state =
				new LibraryRootStoreState();
		assertNull(state.get());
		assertFalse(state.install(null));
		assertNull(state.close());
		assertTrue(state.isClosed());
		assertNull(state.get());
		assertFalse(state.install(null));
		assertNull(state.close());
	}
}
