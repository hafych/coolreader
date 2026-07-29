package org.coolreader.crengine;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class FontFilterSessionTest {
	@Test
	public void replacementStopsAndInvalidatesPreviousScan() {
		FontFilterSession session = new FontFilterSession();
		AtomicInteger firstStops = new AtomicInteger();
		AtomicInteger secondStops = new AtomicInteger();
		FontFilterSession.Request first =
				session.replace(firstStops::incrementAndGet);
		FontFilterSession.Request second =
				session.replace(secondStops::incrementAndGet);

		assertNotNull(first);
		assertNotNull(second);
		assertEquals(1, firstStops.get());
		assertFalse(session.isActive(first));
		assertFalse(session.complete(first));
		assertTrue(session.isActive(second));
		assertEquals(0, secondStops.get());
	}

	@Test
	public void completionDoesNotCancelFinishedScan() {
		FontFilterSession session = new FontFilterSession();
		AtomicInteger stops = new AtomicInteger();
		FontFilterSession.Request request =
				session.replace(stops::incrementAndGet);

		assertTrue(session.complete(request));
		assertFalse(session.complete(request));
		assertTrue(session.close());
		assertEquals(0, stops.get());
	}

	@Test
	public void cancelStopsCurrentScanAndAllowsReplacement() {
		FontFilterSession session = new FontFilterSession();
		AtomicInteger stops = new AtomicInteger();
		FontFilterSession.Request canceled =
				session.replace(stops::incrementAndGet);

		assertTrue(session.cancel());
		assertEquals(1, stops.get());
		assertFalse(session.isActive(canceled));
		assertFalse(session.cancel());
		assertNotNull(session.replace(() -> { }));
	}

	@Test
	public void closeStopsCurrentScanAndPermanentlyRejectsWork() {
		FontFilterSession session = new FontFilterSession();
		AtomicInteger stops = new AtomicInteger();
		FontFilterSession.Request request =
				session.replace(stops::incrementAndGet);

		assertTrue(session.close());
		assertEquals(1, stops.get());
		assertTrue(session.isClosed());
		assertFalse(session.isActive(request));
		assertFalse(session.close());
		assertNull(session.replace(() -> { }));
	}
}
