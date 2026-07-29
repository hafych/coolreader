package org.coolreader.crengine;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PositionPropertiesTest {
	@Test
	public void percentUsesScrollableHeight() {
		PositionProperties position = position(500, 1200, 200);

		assertEquals(5000, position.getPercent());
	}

	@Test
	public void emptyAndNegativeRangesStartAtZero() {
		assertEquals(0, position(100, 200, 200).getPercent());
		assertEquals(0, position(100, 100, 200).getPercent());
	}

	@Test
	public void percentClampsBeforeAndAfterDocument() {
		assertEquals(0, position(-100, 1200, 200).getPercent());
		assertEquals(10000, position(1500, 1200, 200).getPercent());
	}

	@Test
	public void percentWidensMultiplicationAndRangeSubtraction() {
		assertEquals(
				9999,
				position(
						Integer.MAX_VALUE - 1,
						Integer.MAX_VALUE,
						0).getPercent());
		assertEquals(
				4999,
				position(
						Integer.MAX_VALUE,
						Integer.MAX_VALUE,
						Integer.MIN_VALUE).getPercent());
	}

	@Test
	public void scrollMovementUsesTheSameWidenedRange() {
		PositionProperties position = position(
				Integer.MAX_VALUE,
				Integer.MAX_VALUE,
				Integer.MIN_VALUE);
		position.pageMode = 0;
		assertTrue(position.canMoveToNextPage());

		position.y = Integer.MAX_VALUE;
		position.fullHeight = Integer.MAX_VALUE;
		position.pageHeight = 0;
		assertFalse(position.canMoveToNextPage());
	}

	private static PositionProperties position(
			int y,
			int fullHeight,
			int pageHeight) {
		PositionProperties position = new PositionProperties();
		position.y = y;
		position.fullHeight = fullHeight;
		position.pageHeight = pageHeight;
		return position;
	}
}
