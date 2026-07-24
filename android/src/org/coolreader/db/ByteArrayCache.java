/*
 * CoolReader for Android
 * Copyright (C) 2012 Vadim Lopatin <coolreader.org@gmail.com>
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.coolreader.db;

import java.util.ArrayList;


public class ByteArrayCache {

	private static final int DEFAULT_MAX_ITEM_COUNT = 256;

	public ByteArrayCache(int maxSize) {
		this(maxSize, DEFAULT_MAX_ITEM_COUNT);
	}

	ByteArrayCache(int maxSize, int maxItemCount) {
		if (maxSize < 0 || maxItemCount < 1)
			throw new IllegalArgumentException("Invalid cache bounds");
		this.maxSize = maxSize;
		this.maxItemCount = maxItemCount;
	}

	public synchronized void put(String id, byte[] data) {
		if (data == null) {
			remove(id);
			return;
		}
		int index = find(id);
		if (data.length > maxSize) {
			if (index >= 0)
				removeAt(index, true);
			return;
		}
		if (index >= 0) {
			ByteArrayItem item = list.get(index);
			currentSize -= item.data.length;
			item.data = data;
			currentSize += item.data.length;
			moveOnTop(index);
		} else {
			ByteArrayItem item = new ByteArrayItem(id, data);
			list.add(item);
			currentSize += item.data.length;
		}
		checkSize();
	}

	public synchronized byte[] get(String id) {
		int index = find(id);
		if (index < 0) {
			misses++;
			return null;
		}
		hits++;
		ByteArrayItem item = list.get(index);
		moveOnTop(index);
		return item.data;
	}

	public synchronized void remove(String id) {
		int index = find(id);
		if (index < 0)
			return;
		removeAt(index, false);
	}

	public synchronized void clear() {
		for (ByteArrayItem item : list)
			item.data = null;
		list.clear();
		currentSize = 0;
	}

	public synchronized Stats getStats() {
		return new Stats(maxSize, maxItemCount, currentSize, list.size(),
				hits, misses, evictions);
	}

	public synchronized void resetStats() {
		hits = 0;
		misses = 0;
		evictions = 0;
	}

	public static final class Stats {
		public final int capacityBytes;
		public final int capacityItems;
		public final int sizeBytes;
		public final int itemCount;
		public final long hits;
		public final long misses;
		public final long evictions;

		private Stats(int capacityBytes, int capacityItems, int sizeBytes,
				int itemCount, long hits, long misses, long evictions) {
			this.capacityBytes = capacityBytes;
			this.capacityItems = capacityItems;
			this.sizeBytes = sizeBytes;
			this.itemCount = itemCount;
			this.hits = hits;
			this.misses = misses;
			this.evictions = evictions;
		}
	}

	private static class ByteArrayItem {
		public String id;
		public byte[] data;
		public ByteArrayItem(String id, byte[] data) {
			this.id = id;
			this.data = data;
		}
	}
	
	private final int maxSize;
	private final int maxItemCount;
	private int currentSize;
	private long hits;
	private long misses;
	private long evictions;
	private final ArrayList<ByteArrayItem> list =
			new ArrayList<ByteArrayItem>();

	private int find(String id) {
		for (int i=0; i<list.size(); i++)
			if (list.get(i).id.equals(id))
				return i;
		return -1;
	}

	private void moveOnTop(int index) {
		if (index >= list.size() - 1)
			return; // already on top
		ByteArrayItem item = list.remove(index);
		list.add(item);
	}

	private void checkSize() {
		while (currentSize > maxSize || list.size() > maxItemCount)
			removeAt(0, true);
	}

	private void removeAt(int index, boolean eviction) {
		ByteArrayItem item = list.remove(index);
		currentSize -= item.data.length;
		item.data = null; // faster GC
		if (eviction)
			evictions++;
	}
}
