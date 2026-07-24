package org.coolreader.crengine;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DocumentSourceTest {
	@Test
	public void legacyArchiveLocationBecomesTypedArchiveEntry() {
		DocumentSource source =
				DocumentSource.fromLegacyLocation("/books/library.zip@/folder/book.fb2");

		assertEquals(DocumentSource.Kind.ARCHIVE_ENTRY, source.getKind());
		assertEquals("/books/library.zip", source.getContainer().getIdentity());
		assertEquals("folder/book.fb2", source.getArchiveEntry());
		assertEquals(
				"/books/library.zip@/folder/book.fb2", source.getIdentity());
		assertEquals(DocumentFormat.FB2, source.getFormat());
		assertTrue(source.isDurable());
	}

	@Test
	public void contentUriDurabilityTracksPersistedGrant() {
		DocumentSource temporary =
				DocumentSource.contentUri("content://provider/books/7", false);
		DocumentSource persisted =
				DocumentSource.contentUri("content://provider/books/7", true);

		assertFalse(temporary.isDurable());
		assertTrue(persisted.isDurable());
		assertEquals(DocumentSource.Kind.CONTENT_URI, persisted.getKind());
	}

	@Test
	public void legacyContentLocationRemainsAResolverOwnedIdentity() {
		DocumentSource source = DocumentSource.fromLegacyLocation(
				"content://provider/books/7");
		assertEquals(DocumentSource.Kind.CONTENT_URI, source.getKind());
		assertEquals("content://provider/books/7", source.getIdentity());
		assertTrue(DocumentSource.isContentUri(source.getIdentity()));
	}

	@Test
	public void temporaryImportKeepsOriginButUsesPrivateIdentity() {
		DocumentSource source = DocumentSource.temporaryImport(
				"/private/imports/book.epub",
				"content://provider/books/7",
				"book.epub",
				"application/octet-stream",
				123,
				DocumentFormat.EPUB);

		assertEquals(DocumentSource.Kind.TEMPORARY_IMPORT, source.getKind());
		assertEquals("/private/imports/book.epub", source.getIdentity());
		assertEquals("content://provider/books/7", source.getOriginLocator());
		assertTrue(source.isDurable());
	}

	@Test
	public void normalizingTemporaryImportKeepsItsTypeAndOrigin() {
		DocumentSource source = DocumentSource.temporaryImport(
				"/private/imports/../imports/book.epub",
				"content://provider/books/7",
				"book.epub",
				"application/epub+zip",
				123,
				DocumentFormat.EPUB);

		DocumentSource normalized =
				source.withLocalPath("/private/imports/book.epub");

		assertEquals(DocumentSource.Kind.TEMPORARY_IMPORT, normalized.getKind());
		assertEquals("/private/imports/book.epub", normalized.getLocalPath());
		assertEquals("content://provider/books/7", normalized.getOriginLocator());
		assertEquals(DocumentFormat.EPUB, normalized.getFormat());
	}

	@Test
	public void normalizingArchiveKeepsEntryMetadata() {
		DocumentSource source = DocumentSource.archiveEntry(
				DocumentSource.file("/books/../books/library.zip"),
				"folder/book.fb2")
				.withMetadata(
						"book.fb2", "application/fb2", 42, DocumentFormat.FB2);

		DocumentSource normalized = source.withLocalPath(
				"/books/library.zip@/folder/book.fb2");

		assertEquals(DocumentSource.Kind.ARCHIVE_ENTRY, normalized.getKind());
		assertEquals(
				"/books/library.zip@/folder/book.fb2",
				normalized.getLocalPath());
		assertEquals("book.fb2", normalized.getDisplayName());
		assertEquals(42, normalized.getSize());
		assertEquals(DocumentFormat.FB2, normalized.getFormat());
	}
}
