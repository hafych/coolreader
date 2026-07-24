package org.coolreader.crengine;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ScanBatchCursorTest {
	@Test
	public void tenThousandItemsStayWithinBatchBound() {
		AtomicBoolean stopped = new AtomicBoolean();
		ScanBatchCursor cursor =
				new ScanBatchCursor(10_000, 64, stopped::get);
		int expectedStart = 0;
		int batchCount = 0;
		int largestBatch = 0;

		ScanBatchCursor.Range range;
		while ((range = cursor.next()) != null) {
			assertEquals(expectedStart, range.start);
			assertTrue(range.size() > 0);
			assertTrue(range.size() <= 64);
			expectedStart = range.end;
			largestBatch = Math.max(largestBatch, range.size());
			batchCount++;
		}

		assertEquals(10_000, expectedStart);
		assertEquals(157, batchCount);
		assertEquals(64, largestBatch);
		assertEquals(10_000, cursor.getTotalCount());
	}

	@Test
	public void cancellationStopsBeforeAnotherBatchIsIssued() {
		AtomicBoolean stopped = new AtomicBoolean();
		ScanBatchCursor cursor =
				new ScanBatchCursor(1_000, 64, stopped::get);

		assertEquals(64, cursor.next().end);
		assertEquals(128, cursor.next().end);
		stopped.set(true);

		assertNull(cursor.next());
	}

	@Test
	public void invalidBoundsAreRejected() {
		assertRejected(() -> new ScanBatchCursor(-1, 1, () -> false));
		assertRejected(() -> new ScanBatchCursor(1, 0, () -> false));
		assertRejected(() -> new ScanBatchCursor(1, 1, null));
	}

	private static void assertRejected(Runnable constructor) {
		try {
			constructor.run();
			fail("Invalid scan bounds were accepted");
		} catch (IllegalArgumentException expected) {
			// Expected.
		}
	}
}
