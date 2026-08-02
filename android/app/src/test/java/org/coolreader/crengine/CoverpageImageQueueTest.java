package org.coolreader.crengine;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;

import org.junit.Test;

/**
 * Pure queue contracts without ImageItem/FileInfo construction (Android
 * static init). Empty-queue paths drive the shipped CoverpageImageQueue.
 */
public class CoverpageImageQueueTest {
	@Test
	public void emptyQueueContracts() {
		CoverpageImageQueue queue = new CoverpageImageQueue();
		assertTrue(queue.empty());
		assertNull(queue.next());
		queue.clear();
		assertTrue(queue.empty());
		ArrayList<CoverpageManager.ImageItem> drained = queue.drain();
		assertTrue(drained.isEmpty());
		assertFalse(queue.addOnTop(null));
	}
}
