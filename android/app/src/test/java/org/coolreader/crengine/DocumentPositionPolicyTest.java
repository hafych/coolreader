package org.coolreader.crengine;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class DocumentPositionPolicyTest {
	@Test
	public void displayPageIsOneBasedAndClamped() {
		assertEquals(0, DocumentPositionPolicy.displayPageNumber(0, 0));
		assertEquals(1, DocumentPositionPolicy.displayPageNumber(0, 10));
		assertEquals(1, DocumentPositionPolicy.displayPageNumber(-5, 10));
		assertEquals(10, DocumentPositionPolicy.displayPageNumber(9, 10));
		assertEquals(10, DocumentPositionPolicy.displayPageNumber(50, 10));
		assertEquals(
				Integer.MAX_VALUE,
				DocumentPositionPolicy.displayPageNumber(
						Integer.MAX_VALUE,
						Integer.MAX_VALUE));
	}

	@Test
	public void percentMapsToValidZeroBasedPage() {
		assertEquals(-1, DocumentPositionPolicy.pageIndexForPercent(0, 50));
		assertEquals(0, DocumentPositionPolicy.pageIndexForPercent(10, 0));
		assertEquals(5, DocumentPositionPolicy.pageIndexForPercent(10, 50));
		assertEquals(9, DocumentPositionPolicy.pageIndexForPercent(10, 100));
		assertEquals(0, DocumentPositionPolicy.pageIndexForPercent(10, -1));
		assertEquals(9, DocumentPositionPolicy.pageIndexForPercent(10, 101));
	}

	@Test
	public void percentPageMappingWidensBeforeMultiplication() {
		assertEquals(
				1073741823,
				DocumentPositionPolicy.pageIndexForPercent(
						Integer.MAX_VALUE,
						50));
		assertEquals(
				Integer.MAX_VALUE - 1,
				DocumentPositionPolicy.pageIndexForPercent(
						Integer.MAX_VALUE,
						100));
	}

	@Test
	public void percentFormattingUsesOneDecimalAndClamps() {
		assertEquals("0.0%", DocumentPositionPolicy.formatPercent(-1));
		assertEquals("0.0%", DocumentPositionPolicy.formatPercent(0));
		assertEquals("12.3%", DocumentPositionPolicy.formatPercent(1234));
		assertEquals("99.9%", DocumentPositionPolicy.formatPercent(9999));
		assertEquals("100.0%", DocumentPositionPolicy.formatPercent(10000));
		assertEquals("100.0%", DocumentPositionPolicy.formatPercent(10001));
	}
}
