package org.coolreader.crengine;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;

public class LibrarySourceFingerprintTest {
	@Test
	public void documentFingerprintUsesSizeArchiveSizeAndTimestamp() {
		String original = LibrarySourceFingerprint.forDocument(
				100, 0, 1_000, false);

		assertEquals(
				original,
				LibrarySourceFingerprint.forDocument(
						100, 0, 1_000, false));
		assertNotEquals(
				original,
				LibrarySourceFingerprint.forDocument(
						100, 0, 1_001, false));
		assertNotEquals(
				original,
				LibrarySourceFingerprint.forDocument(
						101, 0, 1_000, false));
	}

	@Test
	public void unknownTimestampNeverClaimsDocumentIsUnchanged() {
		assertNull(LibrarySourceFingerprint.forDocument(
				100, 0, 0, false));
	}

	@Test
	public void directoryFingerprintIsOrderIndependentAndDetectsChanges() {
		String first = LibrarySourceFingerprint.forDirectoryEntries(
				Arrays.asList(
						LibrarySourceFingerprint.fileEntry(
								"a.fb2", 10, 0, 100),
						LibrarySourceFingerprint.fileEntry(
								"b.epub", 20, 0, 200)));
		String reordered =
				LibrarySourceFingerprint.forDirectoryEntries(
						Arrays.asList(
								LibrarySourceFingerprint.fileEntry(
										"b.epub", 20, 0, 200),
								LibrarySourceFingerprint.fileEntry(
										"a.fb2", 10, 0, 100)));
		String changed =
				LibrarySourceFingerprint.forDirectoryEntries(
						Arrays.asList(
								LibrarySourceFingerprint.fileEntry(
										"a.fb2", 10, 0, 101),
								LibrarySourceFingerprint.fileEntry(
										"b.epub", 20, 0, 200)));

		assertEquals(first, reordered);
		assertNotEquals(first, changed);
	}
}
