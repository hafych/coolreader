package org.coolreader.crengine;

import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

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
		assertFalse(snapshot.isCloudActive());
		assertEquals(-1, snapshot.getCloudPosition());
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

	@Test
	public void cloudProgressIsExplicitAtZeroAndClamped() {
		ReaderProgressState state = new ReaderProgressState();

		assertEquals(
				ReaderProgressState.Change.FIRST,
				state.showCloud(0));
		ReaderProgressState.Snapshot zero =
				state.snapshot();
		assertTrue(zero.isCloudActive());
		assertEquals(0, zero.getCloudPosition());
		assertEquals(
				ReaderProgressState.Change.NONE,
				state.showCloud(0));

		assertEquals(
				ReaderProgressState.Change.UPDATE,
				state.showCloud(20000));
		assertEquals(
				10000,
				state.snapshot().getCloudPosition());
		assertEquals(
				ReaderProgressState.Change.UPDATE,
				state.showCloud(-10));
		assertEquals(0, state.snapshot().getCloudPosition());

		assertTrue(state.hideCloud());
		assertFalse(state.snapshot().isCloudActive());
		assertFalse(state.hideCloud());
	}

	@Test
	public void mainAndCloudChannelsPreserveEachOther() {
		ReaderProgressState state = new ReaderProgressState();

		state.show(250, 10, "Loading");
		state.showCloud(500);
		ReaderProgressState.Snapshot both =
				state.snapshot();
		assertTrue(both.isActive());
		assertEquals(250, both.getPosition());
		assertTrue(both.isCloudActive());
		assertEquals(500, both.getCloudPosition());

		assertTrue(state.hide());
		ReaderProgressState.Snapshot cloudOnly =
				state.snapshot();
		assertFalse(cloudOnly.isActive());
		assertTrue(cloudOnly.isCloudActive());
		assertEquals(500, cloudOnly.getCloudPosition());

		state.show(750, 11, "Formatting");
		assertTrue(state.hideCloud());
		ReaderProgressState.Snapshot mainOnly =
				state.snapshot();
		assertTrue(mainOnly.isActive());
		assertEquals(750, mainOnly.getPosition());
		assertFalse(mainOnly.isCloudActive());
	}

	@Test
	public void concurrentChannelWritersKeepBothFinalValues()
			throws Exception {
		ReaderProgressState state = new ReaderProgressState();
		CountDownLatch start = new CountDownLatch(1);
		AtomicReference<Throwable> failure =
				new AtomicReference<>();
		Thread mainWriter = new Thread(() -> {
			await(start, failure);
			for (int i = 0; i <= 10000; i++)
				state.show(i, 10, "Loading");
		});
		Thread cloudWriter = new Thread(() -> {
			await(start, failure);
			for (int i = 0; i <= 10000; i++)
				state.showCloud(i);
		});

		mainWriter.start();
		cloudWriter.start();
		start.countDown();
		mainWriter.join();
		cloudWriter.join();

		if (failure.get() != null)
			throw new AssertionError(failure.get());
		ReaderProgressState.Snapshot snapshot =
				state.snapshot();
		assertTrue(snapshot.isActive());
		assertEquals(10000, snapshot.getPosition());
		assertTrue(snapshot.isCloudActive());
		assertEquals(10000, snapshot.getCloudPosition());
	}

	@Test(expected = IllegalArgumentException.class)
	public void nullTitleIsRejected() {
		new ReaderProgressState().show(100, 10, null);
	}

	private static void await(
			CountDownLatch start,
			AtomicReference<Throwable> failure) {
		try {
			start.await();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			failure.compareAndSet(null, e);
		}
	}
}
