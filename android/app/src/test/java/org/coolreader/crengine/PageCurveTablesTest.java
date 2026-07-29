package org.coolreader.crengine;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class PageCurveTablesTest {
	private static final int LAST_INDEX = 1024;
	private static final int SCALE = 0x10000;

	@Test
	public void valuesMatchLegacyPageCurlLookup() {
		PageCurveTables tables =
				new PageCurveTables(LAST_INDEX, SCALE);

		assertValues(tables, 0, 0, 0, 3, 3);
		assertValues(tables, 256, 25080, 16560, 63206, 53853);
		assertValues(tables, 512, 46341, 34315, 80403, 61698);
		assertValues(tables, 768, 60547, 55579, 92812, 64754);
		assertValues(
				tables,
				LAST_INDEX,
				65536,
				102944,
				102941,
				65536);
	}

	@Test
	public void everyCurveIsMonotonic() {
		PageCurveTables tables =
				new PageCurveTables(LAST_INDEX, SCALE);
		for (int i = 1; i <= LAST_INDEX; i++) {
			assertTrue(tables.sine(i) >= tables.sine(i - 1));
			assertTrue(tables.arcsine(i) >= tables.arcsine(i - 1));
			assertTrue(
					tables.sourceAngle(i)
							>= tables.sourceAngle(i - 1));
			assertTrue(
					tables.destinationShift(i)
							>= tables.destinationShift(i - 1));
		}
	}

	@Test
	public void invalidTableShapeIsRejected() {
		assertInvalidShape(0, SCALE);
		assertInvalidShape(LAST_INDEX, 0);
	}

	private static void assertValues(
			PageCurveTables tables,
			int index,
			int sine,
			int arcsine,
			int sourceAngle,
			int destinationShift) {
		assertEquals(sine, tables.sine(index));
		assertEquals(arcsine, tables.arcsine(index));
		assertEquals(sourceAngle, tables.sourceAngle(index));
		assertEquals(
				destinationShift,
				tables.destinationShift(index));
	}

	private static void assertInvalidShape(int lastIndex, int scale) {
		try {
			new PageCurveTables(lastIndex, scale);
			fail("Invalid page-curve table shape was accepted");
		} catch (IllegalArgumentException expected) {
			// Expected: malformed lookup geometry is never published.
		}
	}
}
