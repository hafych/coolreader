package org.coolreader.crengine;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ReaderDocumentOptionsTest {
	@Test
	public void missingBookCannotProduceDocumentOptions() {
		assertNull(ReaderDocumentOptions.capture(null));
		assertNull(ReaderDocumentOptions.capture(
				new BookInfo((FileInfo) null)));
	}

	@Test
	public void snapshotOwnsFlagsMetadataAndFormatCapabilities() {
		ReaderDocumentOptions options =
				ReaderDocumentOptions.fromValues(
						false, true, true,
						20260101, 37,
						DocumentFormat.EPUB, "uk");

		assertFalse(options.isTextAutoformatEnabled());
		assertTrue(options.isDocumentStylesEnabled());
		assertTrue(options.isDocumentFontsEnabled());
		assertEquals(20260101, options.getDomVersion());
		assertEquals(37, options.getBlockRenderingFlags());
		assertFalse(options.isTextFormat());
		assertTrue(options.isEpubFormat());
		assertTrue(options.isFormatWithEmbeddedStyles());
		assertEquals("uk", options.getLanguage());
	}

	@Test
	public void textAndStyleCapabilitiesFollowDocumentFormat() {
		ReaderDocumentOptions textOptions =
				ReaderDocumentOptions.fromValues(
						true, true, false,
						0, 0, DocumentFormat.PDB, null);
		assertTrue(textOptions.isTextFormat());
		assertFalse(textOptions.isEpubFormat());
		assertFalse(textOptions.isFormatWithEmbeddedStyles());

		ReaderDocumentOptions htmlOptions =
				ReaderDocumentOptions.fromValues(
						true, true, false,
						0, 0, DocumentFormat.HTML, null);
		assertTrue(htmlOptions.isTextFormat());
		assertFalse(htmlOptions.isEpubFormat());
		assertTrue(htmlOptions.isFormatWithEmbeddedStyles());
	}
}
