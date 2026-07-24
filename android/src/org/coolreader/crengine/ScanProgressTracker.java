/*
 * CoolReader for Android
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 2
 * of the License, or (at your option) any later version.
 */

package org.coolreader.crengine;

final class ScanProgressTracker {
	interface Sink {
		void setProgress(int progress);
		void hide();
	}

	static final int TOTAL = 10_000;
	static final int DISCOVERY_MAX = 3_000;
	private final Sink sink;
	private int lastProgress;
	private boolean hidden;

	ScanProgressTracker(Sink sink) {
		if (sink == null)
			throw new IllegalArgumentException("sink must not be null");
		this.sink = sink;
	}

	void setDiscoveryProgress(int progress) {
		setProgress(Math.min(DISCOVERY_MAX, progress));
	}

	void setMetadataProgress(int completedCount, int totalCount) {
		if (completedCount < 0 || totalCount <= 0
				|| completedCount > totalCount) {
			throw new IllegalArgumentException(
					"invalid metadata progress");
		}
		int metadataRange = TOTAL - DISCOVERY_MAX;
		setProgress(DISCOVERY_MAX
				+ (int) ((long) completedCount
						* metadataRange / totalCount));
	}

	void hide() {
		if (hidden)
			return;
		hidden = true;
		sink.hide();
	}

	private void setProgress(int progress) {
		if (hidden)
			return;
		int bounded = Math.max(0, Math.min(TOTAL, progress));
		if (bounded < lastProgress)
			bounded = lastProgress;
		lastProgress = bounded;
		sink.setProgress(bounded);
	}
}
