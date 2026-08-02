package org.coolreader.crengine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class CoverpageRenderOptionsTest {
	@Test
	public void sizeAndFontUntilClose() {
		CoverpageRenderOptions options = new CoverpageRenderOptions();
		assertEquals(110, options.getMaxWidth());
		assertEquals(140, options.getMaxHeight());
		assertEquals("Droid Sans", options.getFontFace());

		assertFalse(options.setSize(110, 140));
		assertTrue(options.setSize(200, 300));
		assertEquals(200, options.getMaxWidth());
		assertEquals(300, options.getMaxHeight());

		assertFalse(options.setFontFace("Droid Sans"));
		assertTrue(options.setFontFace("Roboto"));
		assertEquals("Roboto", options.getFontFace());
		assertTrue(options.setFontFace(null));
		assertEquals("Droid Sans", options.getFontFace());

		assertTrue(options.close());
		assertTrue(options.isClosed());
		assertFalse(options.setSize(1, 1));
		assertFalse(options.setFontFace("X"));
		assertEquals(200, options.getMaxWidth());
		assertEquals("Droid Sans", options.getFontFace());
		assertFalse(options.close());
	}
}
