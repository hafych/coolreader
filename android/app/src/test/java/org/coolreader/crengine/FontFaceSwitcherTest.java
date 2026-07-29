package org.coolreader.crengine;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class FontFaceSwitcherTest {
	private static final String[] FACES = {
			"Alpha",
			"Beta",
			"Gamma"
	};

	@Test
	public void emptyAndMissingCatalogsAreNoOps() {
		assertNull(FontFaceSwitcher.select("Alpha", null, 1));
		assertNull(
				FontFaceSwitcher.select(
						"Alpha", new String[0], -1));
	}

	@Test
	public void singletonAndZeroDirectionRemainStable() {
		assertEquals(
				"Only",
				FontFaceSwitcher.select(
						"Only", new String[]{"Only"}, 1));
		assertEquals(
				"Beta",
				FontFaceSwitcher.select("Beta", FACES, 0));
	}

	@Test
	public void knownFaceMovesAndWrapsInBothDirections() {
		assertEquals(
				"Beta",
				FontFaceSwitcher.select("Alpha", FACES, 1));
		assertEquals(
				"Alpha",
				FontFaceSwitcher.select("Gamma", FACES, 1));
		assertEquals(
				"Gamma",
				FontFaceSwitcher.select("Alpha", FACES, -1));
		assertEquals(
				"Alpha",
				FontFaceSwitcher.select("Beta", FACES, -1));
	}

	@Test
	public void unknownFaceStartsAtDirectionalEdge() {
		assertEquals(
				"Alpha",
				FontFaceSwitcher.select("Missing", FACES, 1));
		assertEquals(
				"Gamma",
				FontFaceSwitcher.select("Missing", FACES, -1));
		assertEquals(
				"Alpha",
				FontFaceSwitcher.select(null, FACES, 0));
	}

	@Test
	public void directionMagnitudeCannotOverflowSelection() {
		assertEquals(
				"Beta",
				FontFaceSwitcher.select(
						"Alpha", FACES, Integer.MAX_VALUE));
		assertEquals(
				"Gamma",
				FontFaceSwitcher.select(
						"Alpha", FACES, Integer.MIN_VALUE));
	}
}
