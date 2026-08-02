package org.coolreader.crengine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class DisplayMetricsStateTest {
	@Test
	public void metricsPublishUntilClose() {
		DisplayMetricsState state = new DisplayMetricsState();
		assertEquals(160, state.getDensityDpi());
		assertEquals(1.0f, state.getDensityFactor(), 0.001f);

		state.setDensityDpi(320);
		state.setDiagonalInches(5.5f);
		state.setPreferredItemHeight(40);
		state.setFontSizeBounds(10, 80);

		assertEquals(320, state.getDensityDpi());
		assertEquals(2.0f, state.getDensityFactor(), 0.001f);
		assertEquals(5.5f, state.getDiagonalInches(), 0.001f);
		assertTrue(state.isSmartphone());
		assertEquals(40, state.getPreferredItemHeight());
		assertEquals(10, state.getMinFontSize());
		assertEquals(80, state.getMaxFontSize());
		assertEquals(320 / 3, state.getPalmTipPixels());

		assertTrue(state.close());
		assertTrue(state.isClosed());
		// closed owner keeps last published metrics for late pure reads
		assertEquals(320, state.getDensityDpi());
		state.setDensityDpi(120);
		assertEquals(320, state.getDensityDpi());
		assertFalse(state.close());
	}

	@Test
	public void rejectsNonPositiveUpdates() {
		DisplayMetricsState state = new DisplayMetricsState();
		state.setDensityDpi(0);
		assertEquals(160, state.getDensityDpi());
		state.setDiagonalInches(-1f);
		assertEquals(5f, state.getDiagonalInches(), 0.001f);
	}
}
