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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;

public class ReaderBackgroundStateTest {
	@Test
	public void initialSnapshotIsRenderedAsOneValue() {
		ReaderBackgroundState<String, String> state =
				new ReaderBackgroundState<>(
						"paper", "bitmap", true, 0x123456);
		AtomicReference<
				ReaderBackgroundState.Snapshot<String, String>>
				rendered = new AtomicReference<>();

		assertTrue(state.render(rendered::set));

		assertEquals("paper", rendered.get().texture());
		assertEquals("bitmap", rendered.get().bitmap());
		assertTrue(rendered.get().isTiled());
		assertEquals(0x123456, rendered.get().color());
	}

	@Test
	public void replacementPublishesAllFieldsAndReleasesPreviousBitmap() {
		ReaderBackgroundState<String, String> state =
				new ReaderBackgroundState<>(
						"old", "old-bitmap", false, 1);

		ReaderBackgroundState.Publication<String> publication =
				state.replace(
						"new", "new-bitmap", true, 2);
		AtomicReference<
				ReaderBackgroundState.Snapshot<String, String>>
				rendered = new AtomicReference<>();
		state.render(rendered::set);

		assertTrue(publication.isAccepted());
		assertEquals(
				"old-bitmap", publication.releasable());
		assertEquals("new", rendered.get().texture());
		assertEquals("new-bitmap", rendered.get().bitmap());
		assertTrue(rendered.get().isTiled());
		assertEquals(2, rendered.get().color());
	}

	@Test
	public void unchangedConfigurationRejectsCandidateWithoutReplacingCurrent() {
		ReaderBackgroundState<String, String> state =
				new ReaderBackgroundState<>(
						"paper", "current", true, 7);

		assertFalse(state.needsReplacement(
				"paper", true, 7));
		ReaderBackgroundState.Publication<String> publication =
				state.replace(
						"paper", "candidate", true, 7);
		AtomicReference<String> renderedBitmap =
				new AtomicReference<>();
		state.render(snapshot ->
				renderedBitmap.set(snapshot.bitmap()));

		assertFalse(publication.isAccepted());
		assertEquals(
				"candidate", publication.releasable());
		assertEquals("current", renderedBitmap.get());
	}

	@Test
	public void replacementWaitsUntilInFlightRenderReleasesSnapshot()
			throws Exception {
		ReaderBackgroundState<String, String> state =
				new ReaderBackgroundState<>(
						"old", "old-bitmap", false, 1);
		CountDownLatch renderStarted = new CountDownLatch(1);
		CountDownLatch releaseRender = new CountDownLatch(1);
		CountDownLatch replacementStarted =
				new CountDownLatch(1);
		CountDownLatch replacementFinished =
				new CountDownLatch(1);
		ExecutorService executor =
				Executors.newFixedThreadPool(2);
		try {
			Future<Boolean> render = executor.submit(
					() -> state.render(snapshot -> {
						renderStarted.countDown();
						await(releaseRender);
					}));
			assertTrue(renderStarted.await(
					5, TimeUnit.SECONDS));
			Future<ReaderBackgroundState.Publication<String>>
					replacement = executor.submit(() -> {
						replacementStarted.countDown();
						try {
							return state.replace(
									"new", "new-bitmap",
									false, 2);
						} finally {
							replacementFinished.countDown();
						}
					});
			assertTrue(replacementStarted.await(
					5, TimeUnit.SECONDS));

			assertFalse(replacementFinished.await(
					50, TimeUnit.MILLISECONDS));
			releaseRender.countDown();

			assertTrue(render.get(5, TimeUnit.SECONDS));
			ReaderBackgroundState.Publication<String>
					publication =
							replacement.get(
									5, TimeUnit.SECONDS);
			assertTrue(publication.isAccepted());
			assertEquals(
					"old-bitmap",
					publication.releasable());
		} finally {
			releaseRender.countDown();
			executor.shutdownNow();
		}
	}

	@Test
	public void closeReleasesCurrentAndRejectsLateRenderOrPublication() {
		ReaderBackgroundState<String, String> state =
				new ReaderBackgroundState<>(
						"paper", "current", true, 7);

		assertEquals("current", state.close());
		assertNull(state.close());
		assertFalse(state.render(snapshot -> {
			throw new AssertionError(
					"closed state rendered");
		}));
		assertFalse(state.needsReplacement(
				"other", false, 8));
		ReaderBackgroundState.Publication<String> publication =
				state.replace(
						"other", "late", false, 8);

		assertFalse(publication.isAccepted());
		assertEquals("late", publication.releasable());
	}

	private static void await(CountDownLatch latch) {
		try {
			latch.await();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new AssertionError(e);
		}
	}
}
