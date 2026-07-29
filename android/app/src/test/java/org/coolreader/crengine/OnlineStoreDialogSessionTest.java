package org.coolreader.crengine;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class OnlineStoreDialogSessionTest {
	@Test
	public void replacementCancelsOnlyTheSameChannel() {
		OnlineStoreDialogSession session =
				new OnlineStoreDialogSession();
		AtomicInteger oldBookCancels = new AtomicInteger();
		AtomicInteger coverCancels = new AtomicInteger();
		OnlineStoreDialogSession.Request oldBook =
				session.replace(
						OnlineStoreDialogSession.Channel.BOOK_INFO);
		OnlineStoreDialogSession.Request cover =
				session.replace(
						OnlineStoreDialogSession.Channel.COVER);
		session.attachCancellation(
				oldBook, oldBookCancels::incrementAndGet);
		session.attachCancellation(
				cover, coverCancels::incrementAndGet);

		OnlineStoreDialogSession.Request newBook =
				session.replace(
						OnlineStoreDialogSession.Channel.BOOK_INFO);

		assertEquals(1, oldBookCancels.get());
		assertEquals(0, coverCancels.get());
		assertFalse(session.isActive(oldBook));
		assertTrue(session.isActive(newBook));
		assertTrue(session.isActive(cover));
	}

	@Test
	public void lateCancellationAttachmentRunsImmediately() {
		OnlineStoreDialogSession session =
				new OnlineStoreDialogSession();
		AtomicInteger cancels = new AtomicInteger();
		OnlineStoreDialogSession.Request request =
				session.replace(
						OnlineStoreDialogSession.Channel.DOWNLOAD);
		session.cancel(OnlineStoreDialogSession.Channel.DOWNLOAD);

		assertFalse(session.attachCancellation(
				request, cancels::incrementAndGet));
		assertEquals(1, cancels.get());
	}

	@Test
	public void completionClearsCancellationWithoutRunningIt() {
		OnlineStoreDialogSession session =
				new OnlineStoreDialogSession();
		AtomicInteger cancels = new AtomicInteger();
		AtomicInteger lateCancels = new AtomicInteger();
		OnlineStoreDialogSession.Request request =
				session.replace(
						OnlineStoreDialogSession.Channel.AUTHENTICATION);
		session.attachCancellation(
				request, cancels::incrementAndGet);

		assertTrue(session.complete(request));
		assertFalse(session.complete(request));
		session.cancel(
				OnlineStoreDialogSession.Channel.AUTHENTICATION);
		assertEquals(0, cancels.get());
		assertFalse(session.attachCancellation(
				request, lateCancels::incrementAndGet));
		assertEquals(0, lateCancels.get());
	}

	@Test
	public void closeCancelsEveryChannelOnceAndRejectsWork() {
		OnlineStoreDialogSession session =
				new OnlineStoreDialogSession();
		AtomicInteger cancels = new AtomicInteger();
		OnlineStoreDialogSession.Request browser =
				session.replace(
						OnlineStoreDialogSession.Channel.BROWSER);
		OnlineStoreDialogSession.Request download =
				session.replace(
						OnlineStoreDialogSession.Channel.DOWNLOAD);
		session.attachCancellation(browser, cancels::incrementAndGet);
		session.attachCancellation(download, cancels::incrementAndGet);

		assertTrue(session.close());
		assertEquals(2, cancels.get());
		assertTrue(session.isClosed());
		assertFalse(session.close());
		assertNull(session.replace(
				OnlineStoreDialogSession.Channel.COVER));
		assertFalse(session.isActive(browser));
		assertFalse(session.isActive(download));
	}
}
