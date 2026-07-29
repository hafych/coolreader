package org.coolreader.crengine;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class AutoScrollSessionStateTest {
	@Test
	public void sessionIsNotRenderableUntilInitializationCompletes() {
		AutoScrollSessionState<Object> state =
				new AutoScrollSessionState<>();
		Object session = new Object();

		assertTrue(state.requestStart(session));

		assertTrue(state.isActive());
		assertTrue(state.isCurrent(session));
		assertSame(session, state.currentSession());
		assertFalse(state.isReady(session));
		assertFalse(state.isInitialized(session));
		assertNull(state.readySession());
		assertTrue(state.beginInitialization(session));
		assertTrue(state.markReady(session));
		assertTrue(state.isReady(session));
		assertTrue(state.isInitialized(session));
		assertSame(session, state.readySession());
	}

	@Test
	public void stoppedInitializationCannotResurrectItsSession() {
		AutoScrollSessionState<Object> state =
				new AutoScrollSessionState<>();
		Object stopped = new Object();
		assertTrue(state.requestStart(stopped));
		assertTrue(state.beginInitialization(stopped));

		assertTrue(state.stop(stopped));

		assertFalse(state.markReady(stopped));
		assertFalse(state.isActive());
		assertNull(state.readySession());
	}

	@Test
	public void staleOwnerCannotStopReplacementSession() {
		AutoScrollSessionState<Object> state =
				new AutoScrollSessionState<>();
		Object first = new Object();
		Object replacement = new Object();
		assertTrue(state.requestStart(first));
		assertTrue(state.stop(first));
		assertTrue(state.requestStart(replacement));

		assertFalse(state.stop(first));
		assertTrue(state.isCurrent(replacement));
	}

	@Test
	public void initializationTemporarilySuppressesRendering() {
		AutoScrollSessionState<Object> state =
				new AutoScrollSessionState<>();
		Object session = new Object();
		assertTrue(state.requestStart(session));
		assertTrue(state.markReady(session));

		assertTrue(state.beginInitialization(session));

		assertTrue(state.isCurrent(session));
		assertFalse(state.isReady(session));
		assertTrue(state.isInitialized(session));
		assertNull(state.readySession());
	}

	@Test
	public void stopCurrentReturnsItsExactOwnerOnce() {
		AutoScrollSessionState<Object> state =
				new AutoScrollSessionState<>();
		Object session = new Object();
		assertTrue(state.requestStart(session));
		assertTrue(state.markReady(session));

		assertSame(session, state.stopCurrent());

		assertNull(state.stopCurrent());
		assertFalse(state.isActive());
	}

	@Test
	public void nullIsNeverTreatedAsAnOwner() {
		AutoScrollSessionState<Object> state =
				new AutoScrollSessionState<>();

		assertFalse(state.isCurrent(null));
		assertFalse(state.beginInitialization(null));
		assertFalse(state.markReady(null));
		assertFalse(state.stop(null));
	}

	@Test
	public void closePermanentlyRejectsStaleAndNewSessions() {
		AutoScrollSessionState<Object> state =
				new AutoScrollSessionState<>();
		Object session = new Object();
		Object replacement = new Object();
		assertTrue(state.requestStart(session));
		assertTrue(state.markReady(session));

		assertSame(session, state.close());

		assertNull(state.close());
		assertFalse(state.isActive());
		assertFalse(state.isCurrent(session));
		assertFalse(state.markReady(session));
		assertFalse(state.requestStart(replacement));
	}
}
