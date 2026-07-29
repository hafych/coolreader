package org.coolreader.crengine;

final class BacklightTimeoutPolicy {
	private BacklightTimeoutPolicy() {
	}

	static boolean shouldDim(long inactiveMillis, int durationMillis) {
		return durationMillis > 0
				&& inactiveMillis > durationMillis * 8L / 10;
	}

	static boolean isExpired(long inactiveMillis, int durationMillis) {
		return durationMillis <= 0
				|| inactiveMillis > durationMillis;
	}

	static int nextCheckDelay(
			long inactiveMillis,
			int durationMillis) {
		if (durationMillis <= 0)
			return 0;
		int delay = Math.max(1, durationMillis / 20);
		if (shouldDim(inactiveMillis, durationMillis))
			delay = Math.max(1, delay / 8);
		return delay;
	}
}
