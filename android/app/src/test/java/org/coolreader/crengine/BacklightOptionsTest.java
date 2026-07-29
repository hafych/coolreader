package org.coolreader.crengine;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class BacklightOptionsTest {
	@Test
	public void valuesAreIndependentSnapshots() {
		int[] first = BacklightOptions.values();
		assertEquals(30, first.length);
		assertEquals(-1, first[0]);
		assertEquals(100, first[first.length - 1]);

		first[0] = 42;
		int[] second = BacklightOptions.values();
		assertEquals(-1, second[0]);
	}

	@Test
	public void nearestIndexUsesStableEdgesAndFirstTie() {
		assertEquals(0, BacklightOptions.nearestIndex(Integer.MIN_VALUE));
		assertEquals(10, BacklightOptions.nearestIndex(11));
		assertEquals(
				BacklightOptions.size() - 1,
				BacklightOptions.nearestIndex(Integer.MAX_VALUE));
	}

	@Test
	public void titlesAreLocalizedWithoutSharedMutation() {
		String[] first = BacklightOptions.titles("System");
		assertEquals("System", first[0]);
		assertEquals("1%", first[1]);

		first[0] = "Changed";
		String[] second = BacklightOptions.titles("Default");
		assertEquals("Default", second[0]);
		assertEquals(
				"100%",
				BacklightOptions.titleAt(
						BacklightOptions.size() - 1,
						"Default"));
	}
}
