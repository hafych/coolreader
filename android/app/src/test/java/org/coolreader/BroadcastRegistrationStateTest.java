package org.coolreader;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Exercises the real {@link BroadcastRegistrationState} used by CoolReader
 * for battery and time-tick receiver lifecycle.
 */
public class BroadcastRegistrationStateTest {
	@Test
	public void registerUnregisterRoundTrip() {
		BroadcastRegistrationState state =
				new BroadcastRegistrationState();
		assertFalse(state.isRegistered());
		assertFalse(state.beginUnregister());
		assertTrue(state.onRegistered());
		assertTrue(state.isRegistered());
		assertTrue(state.beginUnregister());
		assertFalse(state.isRegistered());
		assertFalse(state.beginUnregister());
	}

	@Test
	public void closeWhileRegisteredRequestsUnregisterOnce() {
		BroadcastRegistrationState state =
				new BroadcastRegistrationState();
		state.onRegistered();
		assertTrue(state.close());
		assertTrue(state.isClosed());
		assertFalse(state.isRegistered());
		assertFalse(state.onRegistered());
		assertFalse(state.beginUnregister());
		assertFalse(state.close());
	}

	@Test
	public void closeWhileUnregisteredDoesNotRequestUnregister() {
		BroadcastRegistrationState state =
				new BroadcastRegistrationState();
		assertFalse(state.close());
		assertTrue(state.isClosed());
		assertFalse(state.onRegistered());
	}

	@Test
	public void postCloseRegisterRejected() {
		BroadcastRegistrationState state =
				new BroadcastRegistrationState();
		state.close();
		assertFalse(state.onRegistered());
		assertFalse(state.isRegistered());
	}
}
