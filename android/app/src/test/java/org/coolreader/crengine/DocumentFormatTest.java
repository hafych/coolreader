package org.coolreader.crengine;

import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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

	@Test
	public void extensionAndMimeArraysAreIndependentSnapshots() {
		String[] extensions = DocumentFormat.FB2.getExtensions();
		String[] mimeFormats = DocumentFormat.FB2.getMimeFormats();
		extensions[0] = ".changed";
		mimeFormats[0] = "application/changed";

		assertArrayEquals(
				new String[]{".fb2", ".fb2.zip"},
				DocumentFormat.FB2.getExtensions());
		assertEquals(
				"application/fb2",
				DocumentFormat.FB2.getMimeFormat());
		assertEquals(
				".fb2",
				DocumentFormat.FB2.getPrimaryExtension());
		assertEquals(
				DocumentFormat.FB2,
				DocumentFormat.byExtension("book.fb2"));
		assertEquals(
				DocumentFormat.FB2,
				DocumentFormat.byMimeType("application/fb2"));
	}

	@Test
	public void emptyFormatHasNoPrimaryExtensionOrMimeType() {
		assertNull(DocumentFormat.NONE.getPrimaryExtension());
		assertNull(DocumentFormat.NONE.getMimeFormat());
	}

	@Test
	public void instanceMetadataFieldsArePrivateFinal() {
		for (Field field : DocumentFormat.class.getDeclaredFields()) {
			if (Modifier.isStatic(field.getModifiers()))
				continue;
			assertTrue(Modifier.isPrivate(field.getModifiers()));
			assertTrue(Modifier.isFinal(field.getModifiers()));
			if (field.getType().isArray())
				assertFalse(Modifier.isStatic(field.getModifiers()));
		}
	}
}
