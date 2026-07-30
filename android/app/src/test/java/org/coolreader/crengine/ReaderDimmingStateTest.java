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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ReaderDimmingStateTest {
	@Test
	public void initialStateUsesNoDimming() {
		ReaderDimmingState state =
				new ReaderDimmingState();

		assertEquals(255, state.alpha());
	}

	@Test
	public void updatesClampAndDuplicatesAreNoOps() {
		ReaderDimmingState state =
				new ReaderDimmingState();

		assertFalse(state.update(1000));
		assertTrue(state.update(-1));
		assertEquals(32, state.alpha());
		assertFalse(state.update(32));
		assertTrue(state.update(100));
		assertEquals(100, state.alpha());
		assertTrue(state.update(1000));
		assertEquals(255, state.alpha());
	}

	@Test
	public void concurrentReadersOnlyObservePublishedValues()
			throws Exception {
		ReaderDimmingState state =
				new ReaderDimmingState();
		CountDownLatch start = new CountDownLatch(1);
		AtomicReference<Throwable> failure =
				new AtomicReference<>();
		Thread writer = new Thread(() -> {
			await(start, failure);
			for (int i = 0; i < 10000; i++)
				state.update((i & 1) == 0 ? -1 : 1000);
		});
		Thread reader = new Thread(() -> {
			await(start, failure);
			for (int i = 0; i < 10000; i++) {
				int alpha = state.alpha();
				if (alpha != 32 && alpha != 255) {
					failure.compareAndSet(
							null,
							new AssertionError(
									"unpublished alpha "
											+ alpha));
					return;
				}
			}
		});

		writer.start();
		reader.start();
		start.countDown();
		writer.join();
		reader.join();

		if (failure.get() != null)
			throw new AssertionError(failure.get());
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
