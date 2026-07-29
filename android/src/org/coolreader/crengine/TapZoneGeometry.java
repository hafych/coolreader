package org.coolreader.crengine;

final class TapZoneGeometry {
	private static final int SEGMENT_COUNT = 3;
	private static final int CENTER_ZONE = 5;

	private TapZoneGeometry() {
	}

	static int zoneAt(int x, int y, int width, int height) {
		if (width <= 0 || height <= 0)
			return CENTER_ZONE;
		int column = segmentAt(x, width);
		int row = segmentAt(y, height);
		return row * SEGMENT_COUNT + column + 1;
	}

	static Bounds boundsAt(int x, int y, int width, int height) {
		if (width <= 0 || height <= 0)
			return Bounds.EMPTY;
		int column = segmentAt(x, width);
		int row = segmentAt(y, height);
		return new Bounds(
				boundary(width, column),
				boundary(height, row),
				boundary(width, column + 1),
				boundary(height, row + 1));
	}

	private static int segmentAt(int coordinate, int size) {
		int clamped = Math.max(0, Math.min(coordinate, size - 1));
		if (clamped < boundary(size, 1))
			return 0;
		if (clamped < boundary(size, 2))
			return 1;
		return 2;
	}

	private static int boundary(int size, int segment) {
		if (segment <= 0)
			return 0;
		if (segment >= SEGMENT_COUNT)
			return size;
		return (int) ((long) size * segment / SEGMENT_COUNT);
	}

	static final class Bounds {
		private static final Bounds EMPTY = new Bounds(0, 0, 0, 0);

		private final int left;
		private final int top;
		private final int right;
		private final int bottom;

		private Bounds(int left, int top, int right, int bottom) {
			this.left = left;
			this.top = top;
			this.right = right;
			this.bottom = bottom;
		}

		int left() {
			return left;
		}

		int top() {
			return top;
		}

		int right() {
			return right;
		}

		int bottom() {
			return bottom;
		}

		boolean isEmpty() {
			return left >= right || top >= bottom;
		}
	}
}
