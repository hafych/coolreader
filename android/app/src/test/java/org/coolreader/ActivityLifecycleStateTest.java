/*
 * CoolReader for Android
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 2
 * of the License, or (at your option) any later version.
 */

package org.coolreader;

import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ActivityLifecycleStateTest {
	@Test
	public void startsPausedAndActive() {
		ActivityLifecycleState state =
				new ActivityLifecycleState();

		assertTrue(state.isActive());
		assertFalse(state.isClosed());
		assertFalse(state.isResumed());
	}

	@Test
	public void resumeAndPausePublishVisibility() {
		ActivityLifecycleState state =
				new ActivityLifecycleState();

		assertTrue(state.resume());
		assertTrue(state.isResumed());
		assertFalse(state.resume());
		assertTrue(state.pause());
		assertFalse(state.isResumed());
		assertFalse(state.pause());
	}

	@Test
	public void closeIsTerminalAndIdempotent() {
		ActivityLifecycleState state =
				new ActivityLifecycleState();

		assertTrue(state.resume());
		assertTrue(state.close());
		assertFalse(state.isActive());
		assertTrue(state.isClosed());
		assertFalse(state.isResumed());
		assertFalse(state.close());
		assertFalse(state.resume());
		assertFalse(state.pause());
	}

	@Test
	public void concurrentCloseHasOneWinner()
			throws Exception {
		ActivityLifecycleState state =
				new ActivityLifecycleState();
		CountDownLatch start = new CountDownLatch(1);
		AtomicInteger winners = new AtomicInteger();
		Thread first = closer(state, start, winners);
		Thread second = closer(state, start, winners);

		first.start();
		second.start();
		start.countDown();
		first.join();
		second.join();

		assertTrue(state.isClosed());
		assertEquals(1, winners.get());
	}

	@Test
	public void closeWinsAgainstConcurrentVisibilityChanges()
			throws Exception {
		ActivityLifecycleState state =
				new ActivityLifecycleState();
		CountDownLatch start = new CountDownLatch(1);
		Thread visibilityChanges = new Thread(() -> {
			await(start);
			for (int i = 0; i < 10_000; i++) {
				state.resume();
				state.pause();
			}
		});
		Thread close = new Thread(() -> {
			await(start);
			state.close();
		});

		visibilityChanges.start();
		close.start();
		start.countDown();
		visibilityChanges.join();
		close.join();

		assertTrue(state.isClosed());
		assertFalse(state.isActive());
		assertFalse(state.isResumed());
		assertFalse(state.resume());
		assertFalse(state.pause());
	}

	private static Thread closer(
			ActivityLifecycleState state,
			CountDownLatch start,
			AtomicInteger winners) {
		return new Thread(() -> {
			await(start);
			if (state.close())
				winners.incrementAndGet();
		});
	}

	private static void await(CountDownLatch latch) {
		try {
			latch.await();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}
}
