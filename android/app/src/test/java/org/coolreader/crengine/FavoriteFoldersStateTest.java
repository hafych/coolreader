package org.coolreader.crengine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;

import org.junit.Test;

/**
 * Pure ownership without constructing FileInfo (Android static init).
 */
public class FavoriteFoldersStateTest {
	@Test
	public void installAddRemoveAndClose() {
		FavoriteFoldersState state = new FavoriteFoldersState();
		assertFalse(state.isLoaded());
		assertFalse(state.install(null));
		assertTrue(state.install(new ArrayList<>()));
		assertTrue(state.isLoaded());
		assertFalse(state.install(new ArrayList<>()));
		assertTrue(state.isEmpty());

		// Cannot construct FileInfo here; empty-list path covers install once,
		// size/close contracts and permanent rejection.
		assertEquals(0, state.size());
		assertTrue(state.close());
		assertTrue(state.isClosed());
		assertFalse(state.isLoaded());
		assertNull(state.orNull());
		assertFalse(state.install(new ArrayList<>()));
		assertFalse(state.close());
	}
}
