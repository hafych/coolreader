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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ReaderSettingsApplyRequestTest {
	@Test
	public void immutableLanguageSnapshotIsRetained() {
		DocumentLoadLifecycle lifecycle =
				new DocumentLoadLifecycle();
		ReaderSettingsApplyRequest request =
				ReaderSettingsApplyRequest.fromSnapshot(
						"uk", lifecycle.interaction());

		assertTrue(request.isCurrent(lifecycle));
		assertEquals("uk", request.bookLanguage(lifecycle));
	}

	@Test
	public void replacementInvalidatesLanguageAndRenderHandoff() {
		DocumentLoadLifecycle lifecycle =
				new DocumentLoadLifecycle();
		ReaderSettingsApplyRequest request =
				ReaderSettingsApplyRequest.capture(
						book(), lifecycle);

		lifecycle.replace();

		assertFalse(request.isCurrent(lifecycle));
		assertNull(request.bookLanguage(lifecycle));
		assertNull(request.renderRequest(
				book(), lifecycle));
	}

	@Test
	public void streamReconciliationCanRebindBookWithinSameInteraction() {
		DocumentLoadLifecycle lifecycle =
				new DocumentLoadLifecycle();
		ReaderSettingsApplyRequest request =
				ReaderSettingsApplyRequest.capture(
						book(), lifecycle);
		BookInfo resolvedBook = book();

		ReaderRenderRequest renderRequest =
				request.renderRequest(
						resolvedBook, lifecycle);

		assertNotNull(renderRequest);
		assertTrue(renderRequest.isCurrent(
				resolvedBook, lifecycle));
	}

	@Test
	public void initialEmptyReaderCanProduceExactNullBookRender() {
		DocumentLoadLifecycle lifecycle =
				new DocumentLoadLifecycle();
		ReaderSettingsApplyRequest request =
				ReaderSettingsApplyRequest.capture(
						null, lifecycle);

		ReaderRenderRequest renderRequest =
				request.renderRequest(null, lifecycle);

		assertNotNull(renderRequest);
		assertTrue(renderRequest.isCurrent(null, lifecycle));
		assertNull(request.bookLanguage(lifecycle));
	}

	@Test
	public void missingOrClosedInteractionCannotBeCaptured() {
		assertNull(ReaderSettingsApplyRequest.capture(
				book(), null));
		assertNull(ReaderSettingsApplyRequest.fromInteraction(
				book(), null));
		assertNull(ReaderSettingsApplyRequest.fromSnapshot(
				"en", null));
		DocumentLoadLifecycle lifecycle =
				new DocumentLoadLifecycle();
		lifecycle.close();

		assertNull(ReaderSettingsApplyRequest.capture(
				book(), lifecycle));
	}

	private static BookInfo book() {
		return new BookInfo((FileInfo) null);
	}
}
