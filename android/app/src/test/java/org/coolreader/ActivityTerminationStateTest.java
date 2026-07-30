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

public class ActivityTerminationStateTest {
	@Test
	public void startsActiveAndCloseIsIdempotent() {
		ActivityTerminationState state =
				new ActivityTerminationState();

		assertTrue(state.isActive());
		assertFalse(state.isClosed());
		assertTrue(state.close());
		assertFalse(state.isActive());
		assertTrue(state.isClosed());
		assertFalse(state.close());
	}

	@Test
	public void concurrentCloseHasOneWinner()
			throws Exception {
		ActivityTerminationState state =
				new ActivityTerminationState();
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

	private static Thread closer(
			ActivityTerminationState state,
			CountDownLatch start,
			AtomicInteger winners) {
		return new Thread(() -> {
			try {
				start.await();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				return;
			}
			if (state.close())
				winners.incrementAndGet();
		});
	}
}
