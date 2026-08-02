package org.coolreader.crengine;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ReaderPageCacheStateTest {
	@Test
	public void publishCurrentReleasesOnlyUnreferencedPrevious() {
		ReaderPageCacheState<Object> state =
				new ReaderPageCacheState<>();
		Object first = new Object();
		Object second = new Object();

		assertTrue(state.publishCurrent(first).isAccepted());
		ReaderPageCacheState.Publication<Object> replaced =
				state.publishCurrent(second);

		assertTrue(replaced.isAccepted());
		assertSame(second, replaced.published());
		assertSame(first, replaced.releasable());
		assertSame(second, state.current());
	}

	@Test
	public void publishCurrentKeepsPreviousWhenStillNext() {
		ReaderPageCacheState<Object> state =
				new ReaderPageCacheState<>();
		Object shared = new Object();
		Object next = new Object();
		state.publishCurrent(shared);
		state.publishNext(next);
		// force next to hold the same identity as previous current
		state.publishNext(shared);
		Object replacement = new Object();

		ReaderPageCacheState.Publication<Object> replaced =
				state.publishCurrent(replacement);

		assertTrue(replaced.isAccepted());
		assertNull(replaced.releasable());
		assertSame(replacement, state.current());
		assertSame(shared, state.next());
	}

	@Test
	public void makeCurrentPromotesNextSlot() {
		ReaderPageCacheState<Object> state =
				new ReaderPageCacheState<>();
		Object current = new Object();
		Object next = new Object();
		state.publishCurrent(current);
		state.publishNext(next);

		assertSame(next, state.makeCurrent(next));
		assertSame(next, state.current());
		assertSame(current, state.next());
	}

	@Test
	public void makeCurrentRejectsUnknownIdentity() {
		ReaderPageCacheState<Object> state =
				new ReaderPageCacheState<>();
		Object current = new Object();
		state.publishCurrent(current);

		assertNull(state.makeCurrent(new Object()));
		assertSame(current, state.current());
	}

	@Test
	public void clearDetachesBothSlots() {
		ReaderPageCacheState<Object> state =
				new ReaderPageCacheState<>();
		Object current = new Object();
		Object next = new Object();
		state.publishCurrent(current);
		state.publishNext(next);

		ReaderPageCacheState.Snapshot<Object> cleared =
				state.clear();

		assertSame(current, cleared.current());
		assertSame(next, cleared.next());
		assertNull(state.current());
		assertNull(state.next());
	}

	@Test
	public void publishSerializedCloseDetachesOnce() {
		ReaderPageCacheState<Object> state =
				new ReaderPageCacheState<>();
		Object current = new Object();
		Object next = new Object();
		state.publishCurrent(current);
		state.publishNext(next);
		ReaderPageCacheClose<Object> close =
				state.beginClose();

		assertTrue(state.publishSerializedClose(close));
		assertNull(state.current());
		assertNull(state.next());
		assertFalse(state.publishSerializedClose(close));
	}

	@Test
	public void closedOwnerRejectsPublicationAndKeepsCandidateReleasable() {
		ReaderPageCacheState<Object> state =
				new ReaderPageCacheState<>();
		Object candidate = new Object();
		state.close();

		ReaderPageCacheState.Publication<Object> rejected =
				state.publishCurrent(candidate);

		assertFalse(rejected.isAccepted());
		assertSame(candidate, rejected.releasable());
		assertNull(state.current());
		assertTrue(state.isClosed());
		assertNull(state.makeCurrent(candidate));
	}
}
