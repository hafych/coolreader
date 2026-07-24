/*
 * CoolReader for Android
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 2
 * of the License, or (at your option) any later version.
 */

package org.coolreader.crengine;

/**
 * Produces bounded, non-overlapping ranges for one-at-a-time scan processing.
 */
final class ScanBatchCursor {
	interface StopSignal {
		boolean isStopped();
	}

	static final class Range {
		final int start;
		final int end;

		Range(int start, int end) {
			this.start = start;
			this.end = end;
		}

		int size() {
			return end - start;
		}
	}

	private final int totalCount;
	private final int batchSize;
	private final StopSignal stopSignal;
	private int nextIndex;

	ScanBatchCursor(int totalCount, int batchSize, StopSignal stopSignal) {
		if (totalCount < 0)
			throw new IllegalArgumentException("totalCount must not be negative");
		if (batchSize <= 0)
			throw new IllegalArgumentException("batchSize must be positive");
		if (stopSignal == null)
			throw new IllegalArgumentException("stopSignal must not be null");
		this.totalCount = totalCount;
		this.batchSize = batchSize;
		this.stopSignal = stopSignal;
	}

	Range next() {
		if (stopSignal.isStopped() || nextIndex >= totalCount)
			return null;
		int start = nextIndex;
		nextIndex = Math.min(totalCount, nextIndex + batchSize);
		return new Range(start, nextIndex);
	}

	int getTotalCount() {
		return totalCount;
	}
}
