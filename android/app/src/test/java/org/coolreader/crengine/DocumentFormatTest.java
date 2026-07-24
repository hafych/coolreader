package org.coolreader.crengine;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class DocumentFormatTest {
	@Test
	public void androidIntentMimeAliasesResolveToEngineFormats() {
		assertEquals(
				DocumentFormat.FB2,
				DocumentFormat.byMimeType("application/x-fictionbook+xml"));
		assertEquals(
				DocumentFormat.RTF,
				DocumentFormat.byMimeType("application/rtf; charset=utf-8"));
		assertEquals(
				DocumentFormat.DOCX,
				DocumentFormat.byMimeType(
						"application/vnd.openxmlformats-officedocument."
								+ "wordprocessingml.document"));
		assertEquals(
				DocumentFormat.PDB,
				DocumentFormat.byMimeType("application/x-mobipocket-ebook"));
	}

	@Test
	public void genericMimeTypesNeverResolveByDeclaration() {
		assertNull(DocumentFormat.byMimeType("application/octet-stream"));
		assertTrue(DocumentFormat.isGenericMimeType(
				" application/octet-stream; charset=binary "));
		assertTrue(DocumentFormat.isGenericMimeType("application/zip"));
		assertTrue(DocumentFormat.isGenericMimeType(null));
	}

	@Test
	public void missingFilenameIsNotTreatedAsSupported() {
		assertNull(DocumentFormat.byExtension(null));
		assertNull(DocumentFormat.getSupportedExtension(null));
	}
}
