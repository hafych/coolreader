package org.coolreader.crengine;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Pure ownership contracts without constructing Engine/Scanner (Android).
 * Install rejection and permanent close are driven on the shipped owner.
 */
public class ServicesGraphStateTest {
	@Test
	public void installNullRejectedAndCloseIsPermanent() {
		ServicesGraphState state = new ServicesGraphState();
		assertFalse(state.isStarted());
		assertFalse(state.install(
				null, null, null, null, null, null, null, null));
		assertNull(state.engine());
		assertTrue(state.close().engine() == null);
		assertTrue(state.isClosed());
		assertFalse(state.install(
				null, null, null, null, null, null, null, null));
		assertTrue(state.close().engine() == null);
	}
}
