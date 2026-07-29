package org.coolreader.crengine;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DeferredTaskQueueTest {
	private static final class Delivery {
		final String task;
		final long delay;

		Delivery(String task, long delay) {
			this.task = task;
			this.delay = delay;
		}
	}

	@Test
	public void queuedTasksKeepDelayAndDrainOnlyOnce() {
		DeferredTaskQueue<String> queue = new DeferredTaskQueue<>();
		List<Delivery> firstTarget = new ArrayList<>();
		List<Delivery> secondTarget = new ArrayList<>();

		assertFalse(queue.post("first", 25));
		assertFalse(queue.post("second", 0));
		assertEquals(2, queue.pendingCount());
		assertEquals(
				2,
				queue.attach((task, delay) -> {
					firstTarget.add(new Delivery(task, delay));
					return true;
				}));
		assertEquals(0, queue.pendingCount());

		assertEquals(
				0,
				queue.attach((task, delay) -> {
					secondTarget.add(new Delivery(task, delay));
					return true;
				}));
		assertTrue(queue.post("third", 7));

		assertEquals(2, firstTarget.size());
		assertEquals("first", firstTarget.get(0).task);
		assertEquals(25, firstTarget.get(0).delay);
		assertEquals("second", firstTarget.get(1).task);
		assertEquals(0, firstTarget.get(1).delay);
		assertEquals(1, secondTarget.size());
		assertEquals("third", secondTarget.get(0).task);
		assertEquals(7, secondTarget.get(0).delay);
	}

	@Test
	public void attachAndPostSerializeWithoutLoss() throws Exception {
		DeferredTaskQueue<String> queue = new DeferredTaskQueue<>();
		List<String> delivered =
				Collections.synchronizedList(new ArrayList<>());
		CountDownLatch draining = new CountDownLatch(1);
		CountDownLatch releaseDrain = new CountDownLatch(1);
		CountDownLatch postStarted = new CountDownLatch(1);
		CountDownLatch postFinished = new CountDownLatch(1);

		queue.post("queued", 0);
		Thread attachThread = new Thread(() -> queue.attach((task, delay) -> {
			delivered.add(task);
			if ("queued".equals(task)) {
				draining.countDown();
				try {
					if (!releaseDrain.await(2, TimeUnit.SECONDS))
						return false;
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					return false;
				}
			}
			return true;
		}));
		attachThread.start();
		assertTrue(draining.await(2, TimeUnit.SECONDS));

		Thread postThread = new Thread(() -> {
			postStarted.countDown();
			queue.post("concurrent", 0);
			postFinished.countDown();
		});
		postThread.start();
		assertTrue(postStarted.await(2, TimeUnit.SECONDS));
		assertFalse(postFinished.await(50, TimeUnit.MILLISECONDS));

		releaseDrain.countDown();
		attachThread.join(2_000);
		postThread.join(2_000);

		assertFalse(attachThread.isAlive());
		assertFalse(postThread.isAlive());
		assertEquals(
				java.util.Arrays.asList("queued", "concurrent"),
				delivered);
		assertEquals(0, queue.pendingCount());
	}

	@Test
	public void rejectedDrainRemainsAvailableForNextTarget() {
		DeferredTaskQueue<String> queue = new DeferredTaskQueue<>();
		List<String> delivered = new ArrayList<>();

		queue.post("retry", -10);
		assertEquals(0, queue.attach((task, delay) -> false));
		assertEquals(1, queue.pendingCount());
		assertEquals(
				1,
				queue.attach((task, delay) -> {
					delivered.add(task + ":" + delay);
					return true;
				}));

		assertEquals(
				java.util.Collections.singletonList("retry:0"),
				delivered);
		assertEquals(0, queue.pendingCount());
	}
}
