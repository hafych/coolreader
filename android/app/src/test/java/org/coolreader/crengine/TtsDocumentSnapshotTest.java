package org.coolreader.crengine;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class TtsDocumentSnapshotTest {
	@Test
	public void missingBookCannotProduceTtsSnapshot() {
		assertNull(TtsDocumentSnapshot.capture(null));
		assertNull(TtsDocumentSnapshot.capture(
				new BookInfo((FileInfo) null)));
	}

	@Test
	public void snapshotOwnsDocumentMetadataAndPath() {
		TtsDocumentSnapshot snapshot =
				TtsDocumentSnapshot.fromValues(
						"Author", "Title", "uk",
						"/books/example.fb2");

		assertEquals("Author", snapshot.getAuthors());
		assertEquals("Title", snapshot.getTitle());
		assertEquals("uk", snapshot.getLanguage());
		assertEquals(
				"/books/example.fb2", snapshot.getPath());
	}

	@Test
	public void nullableMetadataIsPreservedWithoutGlobalFallback() {
		TtsDocumentSnapshot snapshot =
				TtsDocumentSnapshot.fromValues(
						null, null, null, null);

		assertNull(snapshot.getAuthors());
		assertNull(snapshot.getTitle());
		assertNull(snapshot.getLanguage());
		assertNull(snapshot.getPath());
	}
}
