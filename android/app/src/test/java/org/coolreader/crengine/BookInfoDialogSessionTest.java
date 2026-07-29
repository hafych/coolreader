package org.coolreader.crengine;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class BookInfoDialogSessionTest {
	@Test
	public void replacementInvalidatesOlderRequest() {
		BookInfoDialogSession session =
				new BookInfoDialogSession();
		BookInfoDialogSession.Request first =
				session.replace();
		BookInfoDialogSession.Request second =
				session.replace();

		assertFalse(session.isActive(first));
		assertTrue(session.isActive(second));
		assertFalse(session.complete(first));
		assertTrue(session.complete(second));
	}

	@Test
	public void completionClaimsExactRequestOnce() {
		BookInfoDialogSession session =
				new BookInfoDialogSession();
		BookInfoDialogSession.Request request =
				session.replace();

		assertTrue(session.complete(request));
		assertFalse(session.complete(request));
		assertFalse(session.isActive(request));
	}

	@Test
	public void closePermanentlyRejectsWork() {
		BookInfoDialogSession session =
				new BookInfoDialogSession();
		BookInfoDialogSession.Request request =
				session.replace();

		assertTrue(session.close());
		assertFalse(session.isActive(request));
		assertTrue(session.isClosed());
		assertFalse(session.close());
		assertNull(session.replace());
	}
}
