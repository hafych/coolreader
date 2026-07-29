package org.coolreader.crengine;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class BlockingResultTest {
	@Test
	public void awaitBlocksUntilValueIsPublished() throws Exception {
		BlockingResult<String> result = new BlockingResult<>();
		AtomicReference<String> received = new AtomicReference<>();
		CountDownLatch started = new CountDownLatch(1);
		CountDownLatch finished = new CountDownLatch(1);
		Thread waiter = new Thread(() -> {
			started.countDown();
			received.set(result.await());
			finished.countDown();
		});

		waiter.start();
		assertTrue(started.await(2, TimeUnit.SECONDS));
		assertFalse(finished.await(50, TimeUnit.MILLISECONDS));
		result.complete("ready");
		assertTrue(finished.await(2, TimeUnit.SECONDS));
		waiter.join(2_000);

		assertFalse(waiter.isAlive());
		assertEquals("ready", received.get());
	}

	@Test
	public void completionReleasesEveryWaiter() throws Exception {
		BlockingResult<String> result = new BlockingResult<>();
		List<String> received =
				Collections.synchronizedList(new ArrayList<>());
		CountDownLatch started = new CountDownLatch(2);
		CountDownLatch finished = new CountDownLatch(2);
		Runnable await = () -> {
			started.countDown();
			received.add(result.await());
			finished.countDown();
		};
		Thread first = new Thread(await);
		Thread second = new Thread(await);

		first.start();
		second.start();
		assertTrue(started.await(2, TimeUnit.SECONDS));
		result.complete("shared");
		assertTrue(finished.await(2, TimeUnit.SECONDS));
		first.join(2_000);
		second.join(2_000);

		assertFalse(first.isAlive());
		assertFalse(second.isAlive());
		assertEquals(2, received.size());
		assertEquals("shared", received.get(0));
		assertEquals("shared", received.get(1));
	}

	@Test
	public void interruptedWaitStillReceivesResultAndRestoresFlag()
			throws Exception {
		BlockingResult<String> result = new BlockingResult<>();
		AtomicReference<String> received = new AtomicReference<>();
		AtomicBoolean interrupted = new AtomicBoolean();
		CountDownLatch started = new CountDownLatch(1);
		CountDownLatch finished = new CountDownLatch(1);
		Thread waiter = new Thread(() -> {
			started.countDown();
			received.set(result.await());
			interrupted.set(Thread.currentThread().isInterrupted());
			finished.countDown();
		});

		waiter.start();
		assertTrue(started.await(2, TimeUnit.SECONDS));
		waiter.interrupt();
		assertFalse(finished.await(50, TimeUnit.MILLISECONDS));
		result.complete("after-interrupt");
		assertTrue(finished.await(2, TimeUnit.SECONDS));
		waiter.join(2_000);

		assertFalse(waiter.isAlive());
		assertEquals("after-interrupt", received.get());
		assertTrue(interrupted.get());
	}

	@Test
	public void resultCanOnlyCompleteOnce() {
		BlockingResult<String> result = new BlockingResult<>();
		result.complete(null);
		assertEquals(null, result.await());
		try {
			result.complete("late");
			fail("Blocking result accepted a second completion");
		} catch (IllegalStateException expected) {
			assertEquals(null, result.await());
		}
	}
}
