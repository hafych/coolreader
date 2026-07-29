package org.coolreader.crengine;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class TtsInitializationSessionTest {
	@Test
	public void replacementCancelsPredecessorAndLatestCompletes() {
		TtsInitializationSession session =
				new TtsInitializationSession();
		ServiceLifecycle lifecycle = new ServiceLifecycle(1L);
		AtomicInteger firstSuccess = new AtomicInteger();
		AtomicInteger firstFailure = new AtomicInteger();
		AtomicInteger secondSuccess = new AtomicInteger();
		AtomicInteger secondFailure = new AtomicInteger();
		TtsInitializationSession.Replacement first =
				session.replace(
						lifecycle,
						"engine.one",
						firstSuccess::incrementAndGet,
						firstFailure::incrementAndGet);
		TtsInitializationSession.Replacement second =
				session.replace(
						lifecycle,
						"engine.two",
						secondSuccess::incrementAndGet,
						secondFailure::incrementAndGet);

		assertNull(first.getCancellation());
		assertNotNull(second.getCancellation());
		assertTrue(second.getCancellation().run());
		assertFalse(second.getCancellation().run());
		assertEquals(0, firstSuccess.get());
		assertEquals(1, firstFailure.get());
		assertFalse(session.isActive(first.getCurrent()));
		assertNull(session.complete(first.getCurrent()));

		TtsInitializationSession.Completion completion =
				session.complete(second.getCurrent());
		assertNotNull(completion);
		assertTrue(completion.runSuccess());
		assertFalse(completion.runFailure());
		assertEquals(1, secondSuccess.get());
		assertEquals(0, secondFailure.get());
	}

	@Test
	public void failureCompletionIsExactAndOneShot() {
		TtsInitializationSession session =
				new TtsInitializationSession();
		ServiceLifecycle lifecycle = new ServiceLifecycle(2L);
		AtomicInteger success = new AtomicInteger();
		AtomicInteger failure = new AtomicInteger();
		TtsInitializationSession.Request request =
				session.replace(
						lifecycle,
						null,
						success::incrementAndGet,
						failure::incrementAndGet)
						.getCurrent();

		assertSame(lifecycle, request.getLifecycle());
		assertNull(request.getEngine());
		TtsInitializationSession.Completion completion =
				session.complete(request);
		assertNotNull(completion);
		assertTrue(completion.runFailure());
		assertFalse(completion.runSuccess());
		assertEquals(0, success.get());
		assertEquals(1, failure.get());
		assertNull(session.complete(request));
	}

	@Test
	public void cancelDetachesCallbacksWithoutClosingSession() {
		TtsInitializationSession session =
				new TtsInitializationSession();
		ServiceLifecycle lifecycle = new ServiceLifecycle(3L);
		AtomicInteger failure = new AtomicInteger();
		TtsInitializationSession.Request canceled =
				session.replace(
						lifecycle,
						"engine",
						null,
						failure::incrementAndGet)
						.getCurrent();

		TtsInitializationSession.Cancellation cancellation =
				session.cancel();

		assertNotNull(cancellation);
		assertSame(lifecycle, cancellation.getLifecycle());
		assertTrue(cancellation.run());
		assertEquals(1, failure.get());
		assertFalse(session.isActive(canceled));
		assertNotNull(session.replace(
				lifecycle, "replacement", null, null));
	}

	@Test
	public void closeDropsCallbacksAndRejectsLateWork() {
		TtsInitializationSession session =
				new TtsInitializationSession();
		ServiceLifecycle lifecycle = new ServiceLifecycle(4L);
		AtomicInteger success = new AtomicInteger();
		AtomicInteger failure = new AtomicInteger();
		TtsInitializationSession.Request request =
				session.replace(
						lifecycle,
						"engine",
						success::incrementAndGet,
						failure::incrementAndGet)
						.getCurrent();

		assertTrue(session.close());
		assertFalse(session.close());
		assertTrue(session.isClosed());
		assertFalse(session.isActive(request));
		assertNull(session.complete(request));
		assertNull(session.cancel());
		assertNull(session.replace(
				lifecycle, "late", null, null));
		assertEquals(0, success.get());
		assertEquals(0, failure.get());
	}

	@Test
	public void inactiveOrMissingLifecycleIsRejected() {
		TtsInitializationSession session =
				new TtsInitializationSession();
		ServiceLifecycle inactive = new ServiceLifecycle(5L);
		inactive.close();

		assertNull(session.replace(
				null, "engine", null, null));
		assertNull(session.replace(
				inactive, "engine", null, null));
	}
}
