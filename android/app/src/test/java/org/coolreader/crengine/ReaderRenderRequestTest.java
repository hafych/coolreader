/*
 * CoolReader for Android
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 2
 * of the License, or (at your option) any later version.
 */

package org.coolreader.crengine;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ReaderRenderRequestTest {
	@Test
	public void currentBookIsMatchedByIdentity() {
		DocumentLoadLifecycle lifecycle =
				new DocumentLoadLifecycle();
		BookInfo book = book();
		ReaderRenderRequest request =
				ReaderRenderRequest.fromInteraction(
						book, lifecycle.interaction());

		assertTrue(request.isCurrent(book, lifecycle));
		assertFalse(request.isCurrent(
				book(), lifecycle));
	}

	@Test
	public void emptyReaderGenerationOwnsOnlyNullBook() {
		DocumentLoadLifecycle lifecycle =
				new DocumentLoadLifecycle();
		ReaderRenderRequest request =
				ReaderRenderRequest.capture(null, lifecycle);

		assertTrue(request.isCurrent(null, lifecycle));
		assertFalse(request.isCurrent(book(), lifecycle));
	}

	@Test
	public void replacementInvalidatesRequestEvenForSameBook() {
		DocumentLoadLifecycle lifecycle =
				new DocumentLoadLifecycle();
		BookInfo book = book();
		ReaderRenderRequest request =
				ReaderRenderRequest.capture(book, lifecycle);

		lifecycle.replace();

		assertFalse(request.isCurrent(book, lifecycle));
	}

	@Test
	public void cancelAndCloseInvalidateCapturedInteraction() {
		DocumentLoadLifecycle cancelledLifecycle =
				new DocumentLoadLifecycle();
		BookInfo cancelledBook = book();
		ReaderRenderRequest cancelled =
				ReaderRenderRequest.capture(
						cancelledBook,
						cancelledLifecycle);
		cancelledLifecycle.cancel();

		DocumentLoadLifecycle closedLifecycle =
				new DocumentLoadLifecycle();
		BookInfo closedBook = book();
		ReaderRenderRequest closed =
				ReaderRenderRequest.capture(
						closedBook, closedLifecycle);
		closedLifecycle.close();

		assertFalse(cancelled.isCurrent(
				cancelledBook, cancelledLifecycle));
		assertFalse(closed.isCurrent(
				closedBook, closedLifecycle));
	}

	@Test
	public void missingOrClosedLifecycleCannotBeCaptured() {
		assertNull(ReaderRenderRequest.capture(
				book(), null));
		assertNull(ReaderRenderRequest.fromInteraction(
				book(), null));
		DocumentLoadLifecycle lifecycle =
				new DocumentLoadLifecycle();
		lifecycle.close();

		assertNull(ReaderRenderRequest.capture(
				book(), lifecycle));
	}

	private static BookInfo book() {
		return new BookInfo((FileInfo) null);
	}
}
