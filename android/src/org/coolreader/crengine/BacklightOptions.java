package org.coolreader.crengine;

final class BacklightOptions {
	private static final int[] VALUES = {
			-1, 1, 2, 3, 4, 5, 6, 7, 8, 9,
			10, 12, 15, 20, 25, 30, 35, 40, 45, 50,
			55, 60, 65, 70, 75, 80, 85, 90, 95, 100
	};

	private BacklightOptions() {
	}

	static int size() {
		return VALUES.length;
	}

	static int valueAt(int index) {
		return VALUES[index];
	}

	static int nearestIndex(int value) {
		int bestIndex = 0;
		long bestDifference = Long.MAX_VALUE;
		for (int i = 0; i < VALUES.length; i++) {
			long difference = Math.abs((long) VALUES[i] - value);
			if (difference < bestDifference) {
				bestDifference = difference;
				bestIndex = i;
			}
		}
		return bestIndex;
	}

	static int[] values() {
		return VALUES.clone();
	}

	static String titleAt(int index, String defaultTitle) {
		if (index == 0)
			return defaultTitle;
		return VALUES[index] + "%";
	}

	static String[] titles(String defaultTitle) {
		String[] titles = new String[VALUES.length];
		for (int i = 0; i < titles.length; i++)
			titles[i] = titleAt(i, defaultTitle);
		return titles;
	}
}
