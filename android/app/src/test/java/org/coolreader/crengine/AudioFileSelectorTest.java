/*
 * CoolReader for Android
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 2
 * of the License, or (at your option) any later version.
 */

package org.coolreader.crengine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AudioFileSelectorTest {
	@Rule
	public final TemporaryFolder temporaryFolder =
			new TemporaryFolder();

	@Test
	public void legacyPrioritySelectsPreferredExistingSibling()
			throws Exception {
		File directory = temporaryFolder.newFolder("audio");
		File mp3 = new File(directory, "chapter.mp3");
		File wav = new File(directory, "chapter.WAV");
		assertTrue(mp3.createNewFile());
		assertTrue(wav.createNewFile());

		File selected = AudioFileSelector.legacy().findAlternative(
				new File(directory, "chapter.txt"));

		assertEquals(wav, selected);
	}

	@Test
	public void existingOriginalAndSingleFallbackArePreserved()
			throws Exception {
		File directory = temporaryFolder.newFolder("fallback");
		File original = new File(directory, "chapter.txt");
		assertTrue(original.createNewFile());
		assertSame(
				original,
				AudioFileSelector.legacy().findAlternative(original));

		assertTrue(original.delete());
		File fallback = new File(directory, "chapter.aac");
		assertTrue(fallback.createNewFile());
		assertEquals(
				fallback,
				AudioFileSelector.legacy().findAlternative(original));
		assertNull(AudioFileSelector.legacy().findAlternative(null));
	}

	@Test
	public void extensionPriorityIsCopiedAndCannotBeMutated() {
		List<String> source =
				new ArrayList<>(Arrays.asList("WAV", "mp3"));
		AudioFileSelector selector = new AudioFileSelector(source);
		source.set(0, "flac");

		assertEquals(Arrays.asList("wav", "mp3"), selector.extensions());
		try {
			selector.extensions().clear();
			fail("audio extension priority was mutable");
		} catch (UnsupportedOperationException expected) {
			// Expected.
		}
	}

	@Test
	public void invalidExtensionPriorityIsRejected() {
		assertRejected(null);
		assertRejected(new ArrayList<String>());
		assertRejected(Arrays.asList("wav", ""));
	}

	private static void assertRejected(List<String> extensions) {
		try {
			new AudioFileSelector(extensions);
			fail("invalid audio extension priority was accepted");
		} catch (IllegalArgumentException expected) {
			// Expected.
		}
	}
}
