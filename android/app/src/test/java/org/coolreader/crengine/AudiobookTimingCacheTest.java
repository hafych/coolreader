package org.coolreader.crengine;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class AudiobookTimingCacheTest {
	@Rule
	public final TemporaryFolder temporaryFolder =
			new TemporaryFolder();

	private final AudiobookTimingCache cache =
			new AudiobookTimingCache();

	@Test
	public void entriesRoundTripWithoutMutableListEscape()
			throws Exception {
		File target = temporaryFolder.newFile("timings.cache");
		cache.write(
				target,
				Arrays.asList(
						entry("p1", 1, 2, 30, true, "a.mp3"),
						entry(
								"p2",
								3.5,
								4.5,
								30,
								false,
								"part,2.mp3")));

		List<AudiobookTimingCache.Entry> restored =
				cache.read(target);

		assertEquals(2, restored.size());
		assertEquals("p1", restored.get(0).startPos());
		assertEquals(1, restored.get(0).startTime(), 0);
		assertTrue(
				restored.get(0).isFirstSentenceInAudioFile());
		assertEquals(
				"part,2.mp3",
				restored.get(1).audioFileName());
		try {
			restored.clear();
			fail("cache result must be immutable");
		} catch (UnsupportedOperationException expected) {
			// Expected.
		}
	}

	@Test
	public void malformedValuesAreRejectedWithLineNumber()
			throws Exception {
		for (String line : new String[]{
				"p1,NaN,0,1,true,a.mp3\n",
				"p1,-1,0,1,true,a.mp3\n",
				"p1,0,0,1,maybe,a.mp3\n",
				"p1,0,0,1,true\n"}) {
			File source =
					temporaryFolder.newFile(
							"bad-" + Math.abs(line.hashCode()));
			Files.write(
					source.toPath(),
					line.getBytes(StandardCharsets.UTF_8));
			try {
				cache.read(source);
				fail("malformed cache must be rejected");
			} catch (java.io.IOException expected) {
				assertTrue(
						expected.getMessage()
								.contains("line 1"));
			}
		}
	}

	@Test
	public void invalidSnapshotCannotTruncateExistingCache()
			throws Exception {
		File target = temporaryFolder.newFile("timings.cache");
		cache.write(
				target,
				Collections.singletonList(
						entry("p1", 0, 0, 1, true, "a.mp3")));

		try {
			cache.write(
					target,
					Arrays.asList(
							entry(
									"p2",
									0,
									0,
									1,
									false,
									"b.mp3"),
							null));
			fail("null entry must be rejected");
		} catch (IllegalArgumentException expected) {
			assertEquals(
					"entry must not be null",
					expected.getMessage());
		}

		assertEquals("p1", cache.read(target).get(0).startPos());
	}

	private static AudiobookTimingCache.Entry entry(
			String startPos,
			double startTime,
			double startTimeInBook,
			double totalBookDuration,
			boolean first,
			String audioFileName) {
		return new AudiobookTimingCache.Entry(
				startPos,
				startTime,
				startTimeInBook,
				totalBookDuration,
				first,
				audioFileName);
	}
}
