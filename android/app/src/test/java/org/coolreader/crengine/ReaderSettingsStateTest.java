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
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;

public class ReaderSettingsStateTest {
	@Test
	public void constructorAndReplacementCloneCandidates() {
		Properties initial = settings("old", 1);
		ReaderSettingsState state =
				new ReaderSettingsState(initial);
		initial.setProperty("name", "mutated");

		assertEquals(
				"old",
				state.getProperty("name"));
		Properties replacement = settings("new", 2);
		ReaderSettingsState.Snapshot published =
				state.replace(replacement);
		replacement.setInt("generation", 99);

		assertSame(published, state.snapshot());
		assertEquals("new", state.getProperty("name"));
		assertEquals(
				2,
				state.getInt("generation", -1));
	}

	@Test
	public void copiesCannotMutatePublishedOrCapturedSnapshot() {
		ReaderSettingsState state =
				new ReaderSettingsState(
						settings("old", 1));
		ReaderSettingsState.Snapshot captured =
				state.snapshot();
		Properties copy = state.copy();
		copy.setProperty("name", "copy");
		Properties capturedCopy = captured.copy();
		capturedCopy.setInt("generation", 99);

		state.replace(settings("new", 2));

		assertEquals("copy", copy.getProperty("name"));
		assertEquals(
				"old",
				captured.getProperty("name"));
		assertEquals(
				1,
				captured.getInt("generation", -1));
		assertEquals("new", state.getProperty("name"));
		assertNotSame(copy, state.copy());
		assertNotSame(capturedCopy, captured.copy());
	}

	@Test
	public void typedGettersReadOnePublishedGeneration() {
		Properties values = new Properties();
		values.setBool("bool", true);
		values.setInt("int", 42);
		values.setColor("color", 0x123456);
		ReaderSettingsState state =
				new ReaderSettingsState(values);

		assertEquals(true, state.getBool("bool", false));
		assertEquals(42, state.getInt("int", -1));
		assertEquals(
				0xFF123456,
				state.getColor("color", 0));
		assertEquals(
				"fallback",
				state.getProperty(
						"missing", "fallback"));
	}

	@Test
	public void concurrentSnapshotsNeverMixReplacementPairs()
			throws Exception {
		ReaderSettingsState state =
				new ReaderSettingsState(
						settings("a", 1));
		CountDownLatch start = new CountDownLatch(1);
		AtomicReference<Throwable> failure =
				new AtomicReference<>();
		Thread writer = new Thread(() -> {
			await(start, failure);
			for (int i = 0; i < 10000; i++)
				state.replace(
						(i & 1) == 0
								? settings("a", 1)
								: settings("b", 2));
		});
		Thread reader = new Thread(() -> {
			await(start, failure);
			for (int i = 0; i < 10000; i++) {
				ReaderSettingsState.Snapshot snapshot =
						state.snapshot();
				String name =
						snapshot.getProperty("name");
				int generation =
						snapshot.getInt(
								"generation", -1);
				if (!(name.equals("a") && generation == 1)
						&& !(name.equals("b")
								&& generation == 2)) {
					failure.compareAndSet(
							null,
							new AssertionError(
									"mixed settings generation"));
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
	public void nullSettingsAreRejected() {
		new ReaderSettingsState(null);
	}

	private static Properties settings(
			String name, int generation) {
		Properties values = new Properties();
		values.setProperty("name", name);
		values.setInt("generation", generation);
		return values;
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
