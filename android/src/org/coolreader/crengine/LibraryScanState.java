/*
 * CoolReader for Android
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 2
 * of the License, or (at your option) any later version.
 */

package org.coolreader.crengine;

final class LibraryScanState {
	private final int maxEntries;
	private final int maxDepth;
	private ScanStopReason stopReason = ScanStopReason.NONE;
	private int discoveredEntries;
	private int discoveredDirectories;
	private int completedDirectories;
	private int lastDiscoveryProgress;

	LibraryScanState(int maxEntries, int maxDepth) {
		if (maxEntries <= 0)
			throw new IllegalArgumentException(
					"maxEntries must be positive");
		if (maxDepth <= 0)
			throw new IllegalArgumentException(
					"maxDepth must be positive");
		this.maxEntries = maxEntries;
		this.maxDepth = maxDepth;
	}

	synchronized boolean isStopped() {
		return stopReason != ScanStopReason.NONE;
	}

	synchronized void stopByUser() {
		stop(ScanStopReason.USER_REQUEST);
	}

	synchronized void stopAtDepthLimit() {
		stop(ScanStopReason.DEPTH_LIMIT);
	}

	synchronized void startRootDirectory() {
		if (!isStopped())
			discoveredDirectories++;
	}

	synchronized boolean recordEntry(boolean directory) {
		if (isStopped())
			return false;
		if (discoveredEntries >= maxEntries) {
			stop(ScanStopReason.ENTRY_LIMIT);
			return false;
		}
		discoveredEntries++;
		if (directory)
			discoveredDirectories++;
		return true;
	}

	synchronized void completeDirectory() {
		if (completedDirectories < discoveredDirectories)
			completedDirectories++;
	}

	synchronized int discoveryProgress(int progressMaximum) {
		if (progressMaximum < 0)
			throw new IllegalArgumentException(
					"progressMaximum must not be negative");
		if (discoveredDirectories == 0)
			return 0;
		int candidate = (int) Math.min(
				progressMaximum,
				(long) completedDirectories
						* progressMaximum
						/ discoveredDirectories);
		if (candidate > lastDiscoveryProgress)
			lastDiscoveryProgress = candidate;
		return lastDiscoveryProgress;
	}

	synchronized ScanStopReason getStopReason() {
		return stopReason;
	}

	synchronized int getDiscoveredEntries() {
		return discoveredEntries;
	}

	int getMaxEntries() {
		return maxEntries;
	}

	int getMaxDepth() {
		return maxDepth;
	}

	private void stop(ScanStopReason reason) {
		if (stopReason == ScanStopReason.NONE)
			stopReason = reason;
	}
}
