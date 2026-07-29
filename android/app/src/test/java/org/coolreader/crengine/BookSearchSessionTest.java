package org.coolreader.crengine;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class BookSearchSessionTest {
	@Test
	public void replacementInvalidatesOlderPreview() {
		BookSearchSession session = new BookSearchSession();
		BookSearchSession.Preview first = session.replacePreview();
		BookSearchSession.Preview second = session.replacePreview();

		assertNotNull(first);
		assertNotNull(second);
		assertFalse(session.isPreviewActive(first));
		assertFalse(session.completePreview(first));
		assertTrue(session.isPreviewActive(second));
		assertTrue(session.completePreview(second));
	}

	@Test
	public void submitClosesPendingPreviewAndRejectsCancel() {
		BookSearchSession session = new BookSearchSession();
		BookSearchSession.Preview preview = session.replacePreview();

		assertTrue(session.submit());
		assertTrue(session.isClosed());
		assertFalse(session.isPreviewActive(preview));
		assertFalse(session.completePreview(preview));
		assertFalse(session.cancel());
		assertNull(session.replacePreview());
	}

	@Test
	public void cancelIsTheOnlyAcceptedTerminalAction() {
		BookSearchSession session = new BookSearchSession();

		assertTrue(session.cancel());
		assertFalse(session.cancel());
		assertFalse(session.submit());
		assertNull(session.replacePreview());
	}
}
