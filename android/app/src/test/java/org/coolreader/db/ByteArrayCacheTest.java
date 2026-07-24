package org.coolreader.db;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class ByteArrayCacheTest {
	@Test
	public void enforcesLruByteAndItemBoundsWithObservableCounters() {
		ByteArrayCache cache = new ByteArrayCache(6, 2);
		assertNull(cache.get("missing"));

		byte[] first = new byte[] { 1, 1 };
		byte[] second = new byte[] { 2, 2 };
		byte[] third = new byte[] { 3, 3 };
		cache.put("first", first);
		cache.put("second", second);
		assertArrayEquals(first, cache.get("first"));
		cache.put("third", third);

		assertArrayEquals(first, cache.get("first"));
		assertNull(cache.get("second"));
		assertArrayEquals(third, cache.get("third"));

		ByteArrayCache.Stats stats = cache.getStats();
		assertEquals(6, stats.capacityBytes);
		assertEquals(2, stats.capacityItems);
		assertEquals(4, stats.sizeBytes);
		assertEquals(2, stats.itemCount);
		assertEquals(3, stats.hits);
		assertEquals(2, stats.misses);
		assertEquals(1, stats.evictions);

		cache.remove("first");
		stats = cache.getStats();
		assertEquals(2, stats.sizeBytes);
		assertEquals(1, stats.itemCount);
		assertEquals(1, stats.evictions);

		cache.clear();
		stats = cache.getStats();
		assertEquals(0, stats.sizeBytes);
		assertEquals(0, stats.itemCount);

		cache.resetStats();
		stats = cache.getStats();
		assertEquals(0, stats.hits);
		assertEquals(0, stats.misses);
		assertEquals(0, stats.evictions);
	}

	@Test
	public void rejectsSingleEntryLargerThanByteCapacity() {
		ByteArrayCache cache = new ByteArrayCache(4, 10);
		cache.put("first", new byte[] { 1, 2, 3 });
		cache.put("second", new byte[] { 4, 5, 6 });

		ByteArrayCache.Stats stats = cache.getStats();
		assertEquals(3, stats.sizeBytes);
		assertEquals(1, stats.itemCount);
		assertEquals(1, stats.evictions);
		assertNull(cache.get("first"));

		cache.put("oversized", new byte[] { 1, 2, 3, 4, 5 });
		assertNull(cache.get("oversized"));
		stats = cache.getStats();
		assertEquals(3, stats.sizeBytes);
		assertEquals(1, stats.itemCount);
	}
}
