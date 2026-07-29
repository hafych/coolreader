/*
 * CoolReader for Android
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 2
 * of the License, or (at your option) any later version.
 */

package org.coolreader.crengine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class VMRuntimeHackTest {
	@Test
	public void unavailableTrackerIsANoop() {
		VMRuntimeHack tracker = new VMRuntimeHack(null, null, null);

		assertFalse(tracker.trackAlloc(1));
		assertFalse(tracker.trackFree(1));
		assertEquals(0, tracker.trackedSize());
	}

	@Test
	public void incompleteBindingsAreRejected() throws Exception {
		FakeRuntime runtime = new FakeRuntime();
		Method allocation = FakeRuntime.class.getMethod(
				"trackExternalAllocation", long.class);

		try {
			new VMRuntimeHack(runtime, allocation, null);
			fail("incomplete bindings were accepted");
		} catch (IllegalArgumentException expected) {
			// Expected.
		}
	}

	@Test
	public void successfulCallsUseLongAccountingAndStayInstanceOwned()
			throws Exception {
		FakeRuntime firstRuntime = new FakeRuntime();
		FakeRuntime secondRuntime = new FakeRuntime();
		VMRuntimeHack first = tracker(firstRuntime);
		VMRuntimeHack second = tracker(secondRuntime);
		long largeAllocation = (long) Integer.MAX_VALUE + 17L;

		assertTrue(first.trackAlloc(largeAllocation));
		assertTrue(first.trackFree(7));
		assertTrue(second.trackAlloc(3));

		assertEquals(largeAllocation - 7, first.trackedSize());
		assertEquals(3, second.trackedSize());
		assertEquals(largeAllocation, firstRuntime.allocated);
		assertEquals(7, firstRuntime.freed);
		assertEquals(3, secondRuntime.allocated);
	}

	@Test
	public void rejectedOrThrowingCallsDoNotChangeAccounting()
			throws Exception {
		FakeRuntime runtime = new FakeRuntime();
		VMRuntimeHack tracker = tracker(runtime);

		runtime.accept = false;
		assertFalse(tracker.trackAlloc(11));
		assertEquals(0, tracker.trackedSize());
		runtime.accept = true;
		runtime.throwOnFree = true;
		assertFalse(tracker.trackFree(5));
		assertEquals(0, tracker.trackedSize());
		assertFalse(tracker.trackAlloc(-1));
		assertFalse(tracker.trackFree(-1));
	}

	@Test
	public void concurrentCallsCannotLoseAccountingUpdates()
			throws Exception {
		FakeRuntime runtime = new FakeRuntime();
		VMRuntimeHack tracker = tracker(runtime);
		ExecutorService executor = Executors.newFixedThreadPool(4);
		try {
			List<Future<?>> tasks = new ArrayList<>();
			for (int worker = 0; worker < 8; worker++) {
				tasks.add(executor.submit(() -> {
					for (int iteration = 0; iteration < 250; iteration++)
						assertTrue(tracker.trackAlloc(2));
				}));
			}
			for (Future<?> task : tasks)
				task.get();
		} finally {
			executor.shutdownNow();
			assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
		}

		assertEquals(4_000, tracker.trackedSize());
		assertEquals(4_000, runtime.allocated);
	}

	private static VMRuntimeHack tracker(FakeRuntime runtime)
			throws Exception {
		return new VMRuntimeHack(
				runtime,
				FakeRuntime.class.getMethod(
						"trackExternalAllocation", long.class),
				FakeRuntime.class.getMethod(
						"trackExternalFree", long.class));
	}

	public static final class FakeRuntime {
		long allocated;
		long freed;
		boolean accept = true;
		boolean throwOnFree;

		public boolean trackExternalAllocation(long size) {
			allocated += size;
			return accept;
		}

		public boolean trackExternalFree(long size) {
			if (throwOnFree)
				throw new IllegalStateException("free failed");
			freed += size;
			return accept;
		}
	}
}
