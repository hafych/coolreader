package org.coolreader.crengine;

final class DocumentPositionPolicy {
	private DocumentPositionPolicy() {
	}

	static int displayPageNumber(int pageIndex, int pageCount) {
		if (pageCount <= 0)
			return 0;
		long display = (long) pageIndex + 1;
		return (int) Math.max(1, Math.min(display, pageCount));
	}

	static int pageIndexForPercent(int pageCount, int percent) {
		if (pageCount <= 0)
			return -1;
		int boundedPercent = Math.max(0, Math.min(percent, 100));
		if (boundedPercent == 100)
			return pageCount - 1;
		return (int) ((long) pageCount * boundedPercent / 100);
	}

	static String formatPercent(int percent) {
		int bounded = Math.max(0, Math.min(percent, 10000));
		return (bounded / 100)
				+ "."
				+ (bounded / 10 % 10)
				+ "%";
	}
}
