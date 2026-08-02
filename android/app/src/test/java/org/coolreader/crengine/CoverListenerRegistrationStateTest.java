package org.coolreader.crengine;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class CoverListenerRegistrationStateTest {
	@Test
	public void oneShotRegisterUnregisterAndClose() {
		CoverListenerRegistrationState state =
				new CoverListenerRegistrationState();
		assertTrue(state.beginRegister());
		assertTrue(state.isRegistered());
		assertFalse(state.beginRegister());
		assertTrue(state.beginUnregister());
		assertFalse(state.isRegistered());
		assertFalse(state.beginUnregister());
		assertTrue(state.beginRegister());
		assertTrue(state.close());
		assertTrue(state.isClosed());
		assertFalse(state.isRegistered());
		assertFalse(state.beginRegister());
		assertFalse(state.beginUnregister());
		assertFalse(state.close());
	}
}
