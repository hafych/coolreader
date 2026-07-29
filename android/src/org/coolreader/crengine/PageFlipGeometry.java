package org.coolreader.crengine;

final class PageFlipGeometry {
	private PageFlipGeometry() {
	}

	static int tableIndex(int value, int maximum, int lastIndex) {
		if (value <= 0 || maximum <= 0 || lastIndex <= 0)
			return 0;
		if (value >= maximum)
			return lastIndex;
		return (int) ((long) value * lastIndex / maximum);
	}
}
