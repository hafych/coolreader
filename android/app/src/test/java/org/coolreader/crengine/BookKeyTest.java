package org.coolreader.crengine;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;

public class BookKeyTest {
	private static final String HASH_A =
			"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
					+ "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";

	@Test
	public void unhashedFilesRemainDistinctByLocator() {
		BookKey first = BookKey.fromDocumentSource(
				DocumentSource.file("/books/first/book.fb2")
						.withMetadata("book.fb2", null, 42, DocumentFormat.FB2));
		BookKey second = BookKey.fromDocumentSource(
				DocumentSource.file("/books/second/book.fb2")
						.withMetadata("book.fb2", null, 42, DocumentFormat.FB2));

		assertNotEquals(first.getValue(), second.getValue());
		assertNull(first.getContentHash());
	}

	@Test
	public void strongHashMakesFileKeyStableAcrossMove() {
		BookKey first = BookKey.fromDocumentSource(
				DocumentSource.file("/books/first/book.fb2")
						.withMetadata("book.fb2", null, 42, DocumentFormat.FB2))
				.withContentHash(HASH_A);
		BookKey moved = BookKey.fromDocumentSource(
				DocumentSource.file("/new-root/book.fb2")
						.withMetadata("book.fb2", null, 42, DocumentFormat.FB2))
				.withContentHash(HASH_A);

		assertEquals(first.getValue(), moved.getValue());
		assertNotEquals(first.getSourceLocator(), moved.getSourceLocator());
	}

	@Test
	public void archiveEntriesStayDistinctWithSameContainerHash() {
		DocumentSource archive =
				DocumentSource.file("/books/library.zip");
		BookKey first = BookKey.fromDocumentSource(
				DocumentSource.archiveEntry(archive, "one.fb2")
						.withMetadata("one.fb2", null, 42, DocumentFormat.FB2))
				.withContentHash(HASH_A);
		BookKey second = BookKey.fromDocumentSource(
				DocumentSource.archiveEntry(archive, "two.fb2")
						.withMetadata("two.fb2", null, 42, DocumentFormat.FB2))
				.withContentHash(HASH_A);

		assertNotEquals(first.getValue(), second.getValue());
	}

	@Test
	public void contentUriKeyIncludesDocumentIdentityUntilHashed() {
		BookKey first = BookKey.fromDocumentSource(
				DocumentSource.contentUri(
						"content://provider/document/one", true)
						.withMetadata("book.fb2", null, 42, DocumentFormat.FB2));
		BookKey second = BookKey.fromDocumentSource(
				DocumentSource.contentUri(
						"content://provider/document/two", true)
						.withMetadata("book.fb2", null, 42, DocumentFormat.FB2));

		assertNotEquals(first.getValue(), second.getValue());
		assertEquals(
				BookKey.SourceType.CONTENT_URI, first.getSourceType());
	}

	@Test
	public void legacyIdentityUpgradesWithoutChangingStoredSource() {
		BookKey upgraded = BookKey.restore(
						"legacy:7", BookKey.SourceType.FILE.name(),
						"/old-root/book.fb2", null, 42, null)
				.withContentHash(HASH_A);

		assertEquals("/old-root/book.fb2", upgraded.getSourceLocator());
		assertEquals(HASH_A, upgraded.getContentHash());
		assertEquals(
				upgraded.getValue(),
				BookKey.fromDocumentSource(
						DocumentSource.file("/moved/book.fb2")
								.withMetadata(
										"book.fb2", null, 42,
										DocumentFormat.FB2))
						.withContentHash(HASH_A)
						.getValue());
	}

	@Test
	public void hasherIsBoundedAndDeterministic() throws Exception {
		byte[] bytes = "stable-book".getBytes(StandardCharsets.UTF_8);
		String first = StrongDocumentHasher.sha256(
				new ByteArrayInputStream(bytes), bytes.length);
		String second = StrongDocumentHasher.sha256(
				new ByteArrayInputStream(bytes), bytes.length);

		assertEquals(first, second);
		assertEquals(64, first.length());
	}

	@Test(expected = ParseBudget.LimitExceededException.class)
	public void hasherRejectsInputBeyondLimit() throws Exception {
		byte[] bytes = "too-large".getBytes(StandardCharsets.UTF_8);
		StrongDocumentHasher.sha256(
				new ByteArrayInputStream(bytes), bytes.length - 1);
	}
}
