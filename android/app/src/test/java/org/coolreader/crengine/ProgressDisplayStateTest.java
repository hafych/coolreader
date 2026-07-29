package org.coolreader.crengine;

import org.junit.Test;

import java.util.Locale;

import static org.junit.Assert.assertEquals;

public class ProgressDisplayStateTest {
	@Test
	public void normalProgressFormatsNumberAndPercent() {
		ProgressDisplayState.Snapshot display =
				ProgressDisplayState.format(
						4325, 10000, Locale.US);

		assertEquals("4325/10000", display.number());
		assertEquals("43%", display.percent());
	}

	@Test
	public void progressIsClampedToValidBounds() {
		ProgressDisplayState.Snapshot before =
				ProgressDisplayState.format(
						Integer.MIN_VALUE, 100, Locale.US);
		ProgressDisplayState.Snapshot after =
				ProgressDisplayState.format(
						Integer.MAX_VALUE, 100, Locale.US);

		assertEquals("0/100", before.number());
		assertEquals("0%", before.percent());
		assertEquals("100/100", after.number());
		assertEquals("100%", after.percent());
	}

	@Test
	public void invalidMaximumHasStableEmptyDisplay() {
		ProgressDisplayState.Snapshot zero =
				ProgressDisplayState.format(50, 0, Locale.US);
		ProgressDisplayState.Snapshot negative =
				ProgressDisplayState.format(
						50, Integer.MIN_VALUE, Locale.US);

		assertEquals("0/0", zero.number());
		assertEquals("0%", zero.percent());
		assertEquals(zero.number(), negative.number());
		assertEquals(zero.percent(), negative.percent());
	}

	@Test(expected = IllegalArgumentException.class)
	public void nullLocaleIsRejected() {
		ProgressDisplayState.format(1, 2, null);
	}
}
