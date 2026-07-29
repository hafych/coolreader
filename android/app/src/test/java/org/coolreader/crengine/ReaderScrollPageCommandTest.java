package org.coolreader.crengine;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class ReaderScrollPageCommandTest {
	@Test
	public void scrollsSevenEighthsOfViewport() {
		PositionProperties position =
				position(1000, 5000, 800);

		assertEquals(
				Integer.valueOf(1700),
				ReaderScrollPageCommand.destination(
						position, 1));
		assertEquals(
				Integer.valueOf(300),
				ReaderScrollPageCommand.destination(
						position, -1));
	}

	@Test
	public void clampsAtDocumentBoundaries() {
		assertEquals(
				Integer.valueOf(4200),
				ReaderScrollPageCommand.destination(
						position(4000, 5000, 800),
						1));
		assertEquals(
				Integer.valueOf(0),
				ReaderScrollPageCommand.destination(
						position(100, 5000, 800),
						-1));
	}

	@Test
	public void directionMagnitudeDoesNotChangeStep() {
		PositionProperties position =
				position(1000, 5000, 800);

		assertEquals(
				ReaderScrollPageCommand.destination(
						position, 1),
				ReaderScrollPageCommand.destination(
						position, Integer.MAX_VALUE));
		assertEquals(
				ReaderScrollPageCommand.destination(
						position, -1),
				ReaderScrollPageCommand.destination(
						position, Integer.MIN_VALUE));
	}

	@Test
	public void widenedArithmeticCannotWrapDestination() {
		PositionProperties position =
				position(
						Integer.MAX_VALUE,
						Integer.MAX_VALUE,
						Integer.MAX_VALUE);

		assertEquals(
				Integer.valueOf(0),
				ReaderScrollPageCommand.destination(
						position, 1));

		position.pageHeight = Integer.MIN_VALUE;
		assertEquals(
				Integer.valueOf(Integer.MAX_VALUE),
				ReaderScrollPageCommand.destination(
						position, 1));
	}

	@Test
	public void missingPositionAndDirectionAreRejected() {
		assertNull(
				ReaderScrollPageCommand.destination(
						null, 1));
		assertNull(
				ReaderScrollPageCommand.destination(
						position(100, 5000, 800),
						0));
	}

	private static PositionProperties position(
			int y,
			int fullHeight,
			int pageHeight) {
		PositionProperties position =
				new PositionProperties();
		position.y = y;
		position.fullHeight = fullHeight;
		position.pageHeight = pageHeight;
		position.pageMode = 0;
		return position;
	}
}
