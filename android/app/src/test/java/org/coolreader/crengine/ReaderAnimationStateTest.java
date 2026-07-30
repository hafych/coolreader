/*
 * CoolReader for Android
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 2
 * of the License, or (at your option) any later version.
 */

package org.coolreader.crengine;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class ReaderAnimationStateTest {
	@Test
	public void exactAnimationOwnsTheSessionUntilItFinishes() {
		ReaderAnimationState<Object, Object> state =
				new ReaderAnimationState<>();
		Object first = new Object();
		Object competing = new Object();

		assertTrue(state.installIfIdle(first));
		assertFalse(state.installIfIdle(competing));
		assertSame(first, state.current());
		assertTrue(state.isCurrent(first));
		assertFalse(state.isCurrent(competing));

		assertFalse(state.finish(competing));
		assertTrue(state.finish(first));
		assertNull(state.current());
		assertTrue(state.installIfIdle(competing));
		assertFalse(state.finish(first));
		assertSame(competing, state.current());
	}

	@Test
	public void pointerUpdateCanWaitForAnimationPublication() {
		ReaderAnimationState<Object, Object> state =
				new ReaderAnimationState<>();
		Object update = new Object();
		Object animation = new Object();

		assertTrue(state.installPendingUpdate(update));
		assertTrue(state.isPendingUpdate(update));
		assertSame(update, state.pendingUpdate());
		assertTrue(state.installIfIdle(animation));
		assertSame(animation, state.current());
		assertTrue(state.clearPendingUpdate(update));
		assertNull(state.pendingUpdate());
	}

	@Test
	public void pendingUpdateIsCoalescedByExactIdentity() {
		ReaderAnimationState<Object, Object> state =
				new ReaderAnimationState<>();
		Object first = new Object();
		Object ignored = new Object();

		assertTrue(state.installPendingUpdate(first));
		assertFalse(state.installPendingUpdate(ignored));
		assertSame(first, state.pendingUpdate());
		assertFalse(state.clearPendingUpdate(ignored));
		assertTrue(state.clearPendingUpdate(first));
		assertFalse(state.clearPendingUpdate(first));
	}

	@Test
	public void finishingAnimationInvalidatesItsQueuedUpdate() {
		ReaderAnimationState<Object, Object> state =
				new ReaderAnimationState<>();
		Object animation = new Object();
		Object update = new Object();
		assertTrue(state.installIfIdle(animation));
		assertTrue(state.installPendingUpdate(update));

		assertTrue(state.finish(animation));

		assertNull(state.current());
		assertNull(state.pendingUpdate());
		assertFalse(state.isPendingUpdate(update));
	}

	@Test
	public void staleUpdateCannotClearReplacementAfterReset() {
		ReaderAnimationState<Object, Object> state =
				new ReaderAnimationState<>();
		Object stale = new Object();
		Object replacement = new Object();
		assertTrue(state.installPendingUpdate(stale));
		state.reset();
		assertTrue(state.installPendingUpdate(replacement));

		assertFalse(state.clearPendingUpdate(stale));
		assertSame(replacement, state.pendingUpdate());
	}

	@Test
	public void resetAllowsReuseButCloseIsTerminal() {
		ReaderAnimationState<Object, Object> state =
				new ReaderAnimationState<>();
		Object first = new Object();
		Object update = new Object();
		Object replacement = new Object();
		assertTrue(state.installIfIdle(first));
		assertTrue(state.installPendingUpdate(update));

		assertSame(first, state.reset());
		assertNull(state.current());
		assertNull(state.pendingUpdate());
		assertTrue(state.installIfIdle(replacement));
		assertSame(replacement, state.close());

		assertNull(state.close());
		assertNull(state.current());
		assertFalse(state.isCurrent(replacement));
		assertFalse(state.installIfIdle(new Object()));
		assertFalse(state.installPendingUpdate(new Object()));
	}

	@Test
	public void nullNeverBecomesAnOwner() {
		ReaderAnimationState<Object, Object> state =
				new ReaderAnimationState<>();

		assertFalse(state.installIfIdle(null));
		assertFalse(state.isCurrent(null));
		assertFalse(state.installPendingUpdate(null));
		assertFalse(state.isPendingUpdate(null));
		assertFalse(state.clearPendingUpdate(null));
	}
}
