package org.coolreader.crengine;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class ReaderProgressStateTest {
	@Test
	public void initialStateIsHiddenAndComplete() {
		ReaderProgressState state = new ReaderProgressState();
		ReaderProgressState.Snapshot snapshot = state.snapshot();

		assertFalse(snapshot.isActive());
		assertEquals(-1, snapshot.getPosition());
		assertEquals(0, snapshot.getTitleResource());
		assertEquals(null, snapshot.getTitle());
	}

	@Test
	public void firstShowIsActiveEvenAtZeroPosition() {
		ReaderProgressState state = new ReaderProgressState();

		assertEquals(
				ReaderProgressState.Change.FIRST,
				state.show(0, 10, "Loading"));
		ReaderProgressState.Snapshot snapshot = state.snapshot();
		assertTrue(snapshot.isActive());
		assertEquals(0, snapshot.getPosition());
		assertEquals(10, snapshot.getTitleResource());
		assertEquals("Loading", snapshot.getTitle());
	}

	@Test
	public void duplicateShowIsNoOpAndUpdatesPublishNewSnapshot() {
		ReaderProgressState state = new ReaderProgressState();
		state.show(100, 10, "Loading");
		ReaderProgressState.Snapshot first = state.snapshot();

		assertEquals(
				ReaderProgressState.Change.NONE,
				state.show(100, 10, "Loading"));
		assertSame(first, state.snapshot());

		assertEquals(
				ReaderProgressState.Change.UPDATE,
				state.show(200, 10, "Loading"));
		ReaderProgressState.Snapshot positionUpdate = state.snapshot();
		assertEquals(200, positionUpdate.getPosition());

		assertEquals(
				ReaderProgressState.Change.UPDATE,
				state.show(200, 11, "Formatting"));
		ReaderProgressState.Snapshot titleUpdate = state.snapshot();
		assertEquals(11, titleUpdate.getTitleResource());
		assertEquals("Formatting", titleUpdate.getTitle());
	}

	@Test
	public void hideIsIdempotentAndNextShowIsFirstAgain() {
		ReaderProgressState state = new ReaderProgressState();

		assertFalse(state.hide());
		state.show(100, 10, "Loading");
		assertTrue(state.hide());
		assertFalse(state.snapshot().isActive());
		assertFalse(state.hide());
		assertEquals(
				ReaderProgressState.Change.FIRST,
				state.show(200, 11, "Formatting"));
	}

	@Test(expected = IllegalArgumentException.class)
	public void nullTitleIsRejected() {
		new ReaderProgressState().show(100, 10, null);
	}
}
