/*
 * CoolReader for Android
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 2
 * of the License, or (at your option) any later version.
 */

package org.coolreader.crengine;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class DocumentFileCacheTest {
	@Rule
	public TemporaryFolder temporaryFolder = new TemporaryFolder();

	@Test
	public void pruneDeletesOldestFileDeterministically() throws Exception {
		File directory = temporaryFolder.newFolder("oldest");
		File first = createFile(directory, "a.cache", 4, 1000);
		File second = createFile(directory, "b.cache", 4, 1000);
		File newest = createFile(directory, "c.cache", 4, 3000);

		DocumentFileCache.pruneCacheDirectory(directory, 8, 2, null);

		assertFalse(first.exists());
		assertTrue(second.exists());
		assertTrue(newest.exists());
	}

	@Test
	public void pruneNeverDeletesProtectedFile() throws Exception {
		File directory = temporaryFolder.newFolder("protected");
		File protectedFile = createFile(directory, "protected.cache", 4, 1000);
		File other = createFile(directory, "other.cache", 4, 2000);

		DocumentFileCache.pruneCacheDirectory(directory, 4, 1, protectedFile);

		assertTrue(protectedFile.exists());
		assertFalse(other.exists());
	}

	@Test
	public void oversizedStreamLeavesNoPartialFile() throws Exception {
		File directory = temporaryFolder.newFolder("stream");
		File destination = new File(directory, "book.cache");

		try {
			DocumentFileCache.copyStreamToFile(
					new ByteArrayInputStream(new byte[] {1, 2, 3, 4}), destination, 3);
			fail("Expected the cache byte limit to reject the stream");
		} catch (java.io.IOException expected) {
			// Expected.
		}

		assertFalse(destination.exists());
	}

	private File createFile(File directory, String name, int size, long modified)
			throws Exception {
		File file = new File(directory, name);
		try (FileOutputStream output = new FileOutputStream(file)) {
			output.write(new byte[size]);
		}
		assertTrue(file.setLastModified(modified));
		return file;
	}
}
