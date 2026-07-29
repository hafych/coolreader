package org.coolreader.crengine;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TapZoneGeometryTest {
	@Test
	public void zonesFollowRowMajorThreeByThreeLayout() {
		assertEquals(1, TapZoneGeometry.zoneAt(0, 0, 300, 600));
		assertEquals(2, TapZoneGeometry.zoneAt(100, 0, 300, 600));
		assertEquals(3, TapZoneGeometry.zoneAt(299, 0, 300, 600));
		assertEquals(4, TapZoneGeometry.zoneAt(0, 200, 300, 600));
		assertEquals(5, TapZoneGeometry.zoneAt(100, 200, 300, 600));
		assertEquals(6, TapZoneGeometry.zoneAt(299, 200, 300, 600));
		assertEquals(7, TapZoneGeometry.zoneAt(0, 599, 300, 600));
		assertEquals(8, TapZoneGeometry.zoneAt(100, 599, 300, 600));
		assertEquals(9, TapZoneGeometry.zoneAt(299, 599, 300, 600));
	}

	@Test
	public void highlightBoundsMatchEveryPixelForNonDivisibleSize() {
		int width = 5;
		int height = 7;
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				int zone =
						TapZoneGeometry.zoneAt(x, y, width, height);
				TapZoneGeometry.Bounds bounds =
						TapZoneGeometry.boundsAt(x, y, width, height);
				assertTrue(x >= bounds.left());
				assertTrue(x < bounds.right());
				assertTrue(y >= bounds.top());
				assertTrue(y < bounds.bottom());
				assertEquals(
						zone,
						TapZoneGeometry.zoneAt(
								bounds.left(),
								bounds.top(),
								width,
								height));
			}
		}

		assertBounds(
				1, 2, 3, 4,
				TapZoneGeometry.boundsAt(1, 2, width, height));
	}

	@Test
	public void coordinatesClampAndInvalidSurfacesUseSafeFallbacks() {
		assertEquals(1, TapZoneGeometry.zoneAt(-10, -20, 5, 7));
		assertEquals(
				9,
				TapZoneGeometry.zoneAt(
						Integer.MAX_VALUE,
						Integer.MAX_VALUE,
						5,
						7));
		assertBounds(
				0, 0, 1, 2,
				TapZoneGeometry.boundsAt(-10, -20, 5, 7));
		assertBounds(
				3, 4, 5, 7,
				TapZoneGeometry.boundsAt(
						Integer.MAX_VALUE,
						Integer.MAX_VALUE,
						5,
						7));

		assertEquals(5, TapZoneGeometry.zoneAt(0, 0, 0, 7));
		assertEquals(5, TapZoneGeometry.zoneAt(0, 0, 5, 0));
		assertTrue(TapZoneGeometry.boundsAt(0, 0, 0, 7).isEmpty());
		assertTrue(TapZoneGeometry.boundsAt(0, 0, 5, 0).isEmpty());
	}

	@Test
	public void boundaryArithmeticWidensBeforeMultiplication() {
		int size = Integer.MAX_VALUE;
		int firstBoundary = size / 3;
		int secondBoundary = (int) ((long) size * 2 / 3);

		assertEquals(
				4,
				TapZoneGeometry.zoneAt(
						firstBoundary - 1, 1, size, 3));
		assertEquals(
				5,
				TapZoneGeometry.zoneAt(
						firstBoundary, 1, size, 3));
		assertEquals(
				5,
				TapZoneGeometry.zoneAt(
						secondBoundary - 1, 1, size, 3));
		assertEquals(
				6,
				TapZoneGeometry.zoneAt(
						secondBoundary, 1, size, 3));
		assertBounds(
				secondBoundary, 1, size, 2,
				TapZoneGeometry.boundsAt(
						Integer.MAX_VALUE,
						1,
						size,
						3));
	}

	private static void assertBounds(
			int left,
			int top,
			int right,
			int bottom,
			TapZoneGeometry.Bounds actual) {
		assertEquals(left, actual.left());
		assertEquals(top, actual.top());
		assertEquals(right, actual.right());
		assertEquals(bottom, actual.bottom());
	}
}
