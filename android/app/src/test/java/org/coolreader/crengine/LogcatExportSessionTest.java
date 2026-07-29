package org.coolreader.crengine;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class LogcatExportSessionTest {
	@Test
	public void beginCapturesImmutableExportBoundary() {
		LogcatExportSession session =
				new LogcatExportSession();

		LogcatExportSession.Request request =
				session.begin("cr3.log", 100L, 250L);

		assertEquals("cr3.log", request.getDisplayName());
		assertEquals(100L, request.getSinceMillis());
		assertEquals(
				250L,
				request.getCompletedThroughMillis());
		assertTrue(session.isActive(request));
	}

	@Test
	public void concurrentExportCannotReplaceCurrentOwner() {
		LogcatExportSession session =
				new LogcatExportSession();
		LogcatExportSession.Request first =
				session.begin("first.log", 0L, 1L);

		assertNull(session.begin("second.log", 0L, 2L));
		assertTrue(session.isActive(first));
	}

	@Test
	public void completionClaimsExactRequestOnceAndAllowsNext() {
		LogcatExportSession session =
				new LogcatExportSession();
		LogcatExportSession.Request first =
				session.begin("first.log", 0L, 1L);

		assertTrue(session.complete(first));
		assertFalse(session.complete(first));
		LogcatExportSession.Request second =
				session.begin("second.log", 1L, 2L);
		assertTrue(session.isActive(second));
	}

	@Test
	public void invalidBoundariesAreRejected() {
		LogcatExportSession session =
				new LogcatExportSession();

		assertNull(session.begin(null, 0L, 1L));
		assertNull(session.begin("", 0L, 1L));
		assertNull(session.begin("log", -1L, 1L));
		assertNull(session.begin("log", 2L, 1L));
	}

	@Test
	public void closePermanentlyRejectsLateAndNewWork() {
		LogcatExportSession session =
				new LogcatExportSession();
		LogcatExportSession.Request request =
				session.begin("log", 0L, 1L);

		assertTrue(session.close());
		assertFalse(session.close());
		assertTrue(session.isClosed());
		assertFalse(session.isActive(request));
		assertFalse(session.complete(request));
		assertNull(session.begin("new", 1L, 2L));
	}
}
