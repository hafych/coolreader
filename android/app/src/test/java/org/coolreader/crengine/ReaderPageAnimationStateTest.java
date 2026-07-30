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
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class ReaderPageAnimationStateTest {
	@Test
	public void defaultAndConfiguredModesPublishCoherentSnapshots() {
		ReaderPageAnimationState state = createState();

		assertSnapshot(3, 300, true, state.snapshot());
		assertSnapshot(
				0, 0, false,
				state.configure("0"));
		assertSnapshot(
				1, 300, true,
				state.configure("1"));
	}

	@Test
	public void malformedAndMissingModesUseDefault() {
		ReaderPageAnimationState state = createState();

		assertSnapshot(
				3, 300, true,
				state.configure(null));
		assertSnapshot(
				3, 300, true,
				state.configure("not-a-mode"));
	}

	@Test
	public void outOfRangeModesClampToSupportedEdges() {
		ReaderPageAnimationState state = createState();

		assertSnapshot(
				0, 0, false,
				state.configure("-100"));
		assertSnapshot(
				3, 300, true,
				state.configure("100"));
	}

	@Test
	public void replacementCannotMutateCapturedSnapshot() {
		ReaderPageAnimationState state = createState();
		ReaderPageAnimationState.Snapshot captured =
				state.snapshot();

		ReaderPageAnimationState.Snapshot replacement =
				state.configure("0");

		assertSnapshot(3, 300, true, captured);
		assertSnapshot(0, 0, false, replacement);
		assertSame(replacement, state.snapshot());
	}

	@Test
	public void concurrentPublicationNeverExposesMixedPair()
			throws Exception {
		ReaderPageAnimationState state = createState();
		CountDownLatch start = new CountDownLatch(1);
		AtomicReference<Throwable> failure =
				new AtomicReference<>();
		Thread writer = new Thread(() -> {
			await(start, failure);
			for (int i = 0; i < 20000; i++)
				state.configure((i & 1) == 0 ? "0" : "3");
		});
		Thread reader = new Thread(() -> {
			await(start, failure);
			for (int i = 0; i < 20000; i++) {
				ReaderPageAnimationState.Snapshot snapshot =
						state.snapshot();
				boolean disabled =
						snapshot.mode() == 0
								&& snapshot.durationMs() == 0
								&& !snapshot.isEnabled();
				boolean enabled =
						snapshot.mode() == 3
								&& snapshot.durationMs() == 300
								&& snapshot.isEnabled();
				if (!disabled && !enabled) {
					failure.compareAndSet(
							null,
							new AssertionError(
									"mixed animation snapshot"));
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

	@Test(expected = IllegalArgumentException.class)
	public void invalidStaticConfigurationIsRejected() {
		new ReaderPageAnimationState(
				0, 3, 3, 0, 300);
	}

	private static ReaderPageAnimationState createState() {
		return new ReaderPageAnimationState(
				0, 3, 0, 3, 300);
	}

	private static void assertSnapshot(
			int mode,
			int durationMs,
			boolean enabled,
			ReaderPageAnimationState.Snapshot actual) {
		assertEquals(mode, actual.mode());
		assertEquals(durationMs, actual.durationMs());
		assertEquals(enabled, actual.isEnabled());
		if (enabled)
			assertTrue(actual.durationMs() > 0);
		else
			assertFalse(actual.durationMs() > 0);
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
