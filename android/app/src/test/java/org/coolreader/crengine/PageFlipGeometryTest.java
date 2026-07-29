package org.coolreader.crengine;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class PageFlipGeometryTest {
	@Test
	public void indexIsClampedToBothTableEdges() {
		assertEquals(0, PageFlipGeometry.tableIndex(-1, 100, 1024));
		assertEquals(0, PageFlipGeometry.tableIndex(1, 0, 1024));
		assertEquals(0, PageFlipGeometry.tableIndex(1, 100, 0));
		assertEquals(1024, PageFlipGeometry.tableIndex(100, 100, 1024));
		assertEquals(1024, PageFlipGeometry.tableIndex(101, 100, 1024));
	}

	@Test
	public void indexUsesWidenedIntermediateArithmetic() {
		assertEquals(512, PageFlipGeometry.tableIndex(50, 100, 1024));
		assertEquals(
				1023,
				PageFlipGeometry.tableIndex(
						Integer.MAX_VALUE - 1,
						Integer.MAX_VALUE,
						1024));
	}
}
