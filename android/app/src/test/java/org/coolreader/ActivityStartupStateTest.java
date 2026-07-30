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

public class ActivityStartupStateTest {
	@Test
	public void initialStartCanBeTakenOnlyOnce() {
		ActivityStartupState state =
				new ActivityStartupState();

		assertTrue(state.takeInitialStart());
		assertFalse(state.takeInitialStart());
	}

	@Test
	public void settingsWaitForInitialStartAndInterface() {
		ActivityStartupState interfaceFirst =
				new ActivityStartupState();

		assertTrue(interfaceFirst.markInterfaceReady());
		assertTrue(interfaceFirst.isInterfaceReady());
		assertFalse(interfaceFirst.shouldValidateSettings());
		assertTrue(interfaceFirst.takeInitialStart());
		assertTrue(interfaceFirst.shouldValidateSettings());

		ActivityStartupState startFirst =
				new ActivityStartupState();
		assertTrue(startFirst.takeInitialStart());
		assertFalse(startFirst.shouldValidateSettings());
		assertTrue(startFirst.markInterfaceReady());
		assertTrue(startFirst.shouldValidateSettings());
		assertFalse(startFirst.markInterfaceReady());
	}

	@Test
	public void closeIsTerminalAndClearsInterfaceReadiness() {
		ActivityStartupState state =
				new ActivityStartupState();
		assertTrue(state.takeInitialStart());
		assertTrue(state.markInterfaceReady());

		assertTrue(state.close());
		assertFalse(state.close());
		assertFalse(state.isInterfaceReady());
		assertFalse(state.shouldValidateSettings());
		assertFalse(state.takeInitialStart());
		assertFalse(state.markInterfaceReady());
	}

	@Test
	public void concurrentInitialStartHasOneWinner()
			throws Exception {
		ActivityStartupState state =
				new ActivityStartupState();
		CountDownLatch start = new CountDownLatch(1);
		AtomicInteger winners = new AtomicInteger();
		Thread first = taker(state, start, winners);
		Thread second = taker(state, start, winners);

		first.start();
		second.start();
		start.countDown();
		first.join();
		second.join();

		assertEquals(1, winners.get());
		assertFalse(state.takeInitialStart());
	}

	private static Thread taker(
			ActivityStartupState state,
			CountDownLatch start,
			AtomicInteger winners) {
		return new Thread(() -> {
			try {
				start.await();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				return;
			}
			if (state.takeInitialStart())
				winners.incrementAndGet();
		});
	}
}
