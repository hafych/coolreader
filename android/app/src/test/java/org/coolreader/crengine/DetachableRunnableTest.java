package org.coolreader.crengine;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class DetachableRunnableTest {
	@Test
	public void deliveryRunsDelegateOnlyOnce() {
		AtomicInteger calls = new AtomicInteger();
		DetachableRunnable callback =
				new DetachableRunnable(calls::incrementAndGet);

		callback.run();
		callback.run();

		assertTrue(calls.get() == 1);
		assertFalse(callback.detach());
	}

	@Test
	public void detachReleasesDelegateBeforeDelivery() {
		AtomicInteger calls = new AtomicInteger();
		DetachableRunnable callback =
				new DetachableRunnable(calls::incrementAndGet);

		assertTrue(callback.detach());
		assertFalse(callback.detach());
		callback.run();

		assertTrue(calls.get() == 0);
	}

	@Test
	public void nullDelegateIsRejected() {
		assertThrows(
				IllegalArgumentException.class,
				() -> new DetachableRunnable(null));
	}
}
