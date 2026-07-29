package org.coolreader.crengine;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class FileBrowserNavigationSessionTest {
	@Test
	public void replacementCancelsAndInvalidatesPreviousNavigation() {
		FileBrowserNavigationSession session =
				new FileBrowserNavigationSession();
		AtomicInteger cancels = new AtomicInteger();
		FileBrowserNavigationSession.Request first =
				session.replace();
		session.attachCancellation(
				first, cancels::incrementAndGet);

		FileBrowserNavigationSession.Request second =
				session.replace();

		assertEquals(1, cancels.get());
		assertFalse(session.isActive(first));
		assertTrue(session.isActive(second));
		assertFalse(session.complete(first));
		assertTrue(session.complete(second));
	}

	@Test
	public void cancellationAttachedAfterReplacementRunsImmediately() {
		FileBrowserNavigationSession session =
				new FileBrowserNavigationSession();
		AtomicInteger cancels = new AtomicInteger();
		FileBrowserNavigationSession.Request stale =
				session.replace();
		session.replace();

		assertFalse(session.attachCancellation(
				stale, cancels::incrementAndGet));
		assertEquals(1, cancels.get());
	}

	@Test
	public void completedNavigationDoesNotCancelLateControl() {
		FileBrowserNavigationSession session =
				new FileBrowserNavigationSession();
		AtomicInteger cancels = new AtomicInteger();
		FileBrowserNavigationSession.Request request =
				session.replace();

		assertTrue(session.complete(request));
		assertFalse(session.attachCancellation(
				request, cancels::incrementAndGet));
		assertEquals(0, cancels.get());
	}

	@Test
	public void closeCancelsCurrentAndPermanentlyRejectsNavigation() {
		FileBrowserNavigationSession session =
				new FileBrowserNavigationSession();
		AtomicInteger cancels = new AtomicInteger();
		FileBrowserNavigationSession.Request request =
				session.replace();
		session.attachCancellation(
				request, cancels::incrementAndGet);

		assertTrue(session.close());
		assertEquals(1, cancels.get());
		assertFalse(session.isActive(request));
		assertTrue(session.isClosed());
		assertFalse(session.close());
		assertNull(session.replace());
	}
}
