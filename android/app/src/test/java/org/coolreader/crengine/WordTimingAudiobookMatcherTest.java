package org.coolreader.crengine;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class WordTimingAudiobookMatcherTest {
	@Rule
	public final TemporaryFolder temporaryFolder =
			new TemporaryFolder();

	private final AudiobookTimingCache cache =
			new AudiobookTimingCache();

	@Test
	public void completeCachePublishesOneAtomicTimingSnapshot()
			throws Exception {
		SentenceInfo first = sentence("p1", "First");
		SentenceInfo second = sentence("p2", "Second");
		WordTimingAudiobookMatcher matcher =
				matcher(first, second);
		File source = temporaryFolder.newFile("timings.cache");
		cache.write(
				source,
				Arrays.asList(
						entry("p1", 1, 1, 20, true, "a.mp3"),
						entry(
								"p2",
								4,
								4,
								20,
								false,
								"a.mp3")));
		SentenceTiming oldFirst = first.sentenceTiming;
		SentenceTiming oldSecond = second.sentenceTiming;

		matcher.maybeReadSentenceTimingCache(source);

		assertTrue(matcher.isSentenceTimingReady());
		assertNotSame(oldFirst, first.sentenceTiming);
		assertNotSame(oldSecond, second.sentenceTiming);
		assertEquals(1, first.sentenceTiming.startTime, 0);
		assertEquals(4, second.sentenceTiming.startTime, 0);
		assertEquals(
				first.sentenceTiming.audioFile,
				second.sentenceTiming.audioFile);
		assertSame(second, first.nextSentence);
		assertNull(second.nextSentence);
	}

	@Test
	public void incompleteCacheCannotPartiallyReplaceTimings()
			throws Exception {
		SentenceInfo first = sentence("p1", "First");
		SentenceInfo second = sentence("p2", "Second");
		WordTimingAudiobookMatcher matcher =
				matcher(first, second);
		File source = temporaryFolder.newFile("incomplete.cache");
		cache.write(
				source,
				Collections.singletonList(
						entry("p1", 1, 1, 20, true, "a.mp3")));
		SentenceTiming oldFirst = first.sentenceTiming;
		SentenceTiming oldSecond = second.sentenceTiming;

		matcher.maybeReadSentenceTimingCache(source);

		assertFalse(matcher.isSentenceTimingReady());
		assertSame(oldFirst, first.sentenceTiming);
		assertSame(oldSecond, second.sentenceTiming);
	}

	@Test
	public void unknownOrDuplicatePositionRejectsWholeCache()
			throws Exception {
		for (java.util.List<AudiobookTimingCache.Entry> entries :
				Arrays.asList(
						Arrays.asList(
								entry(
										"p1",
										1,
										1,
										20,
										true,
										"a.mp3"),
								entry(
										"unknown",
										2,
										2,
										20,
										false,
										"a.mp3")),
						Arrays.asList(
								entry(
										"p1",
										1,
										1,
										20,
										true,
										"a.mp3"),
								entry(
										"p1",
										2,
										2,
										20,
										false,
										"a.mp3")))) {
			SentenceInfo first = sentence("p1", "First");
			SentenceInfo second = sentence("p2", "Second");
			WordTimingAudiobookMatcher matcher =
					matcher(first, second);
			File source = temporaryFolder.newFile();
			cache.write(source, entries);
			SentenceTiming oldFirst = first.sentenceTiming;
			SentenceTiming oldSecond = second.sentenceTiming;

			matcher.maybeReadSentenceTimingCache(source);

			assertFalse(matcher.isSentenceTimingReady());
			assertSame(oldFirst, first.sentenceTiming);
			assertSame(oldSecond, second.sentenceTiming);
		}
	}

	@Test
	public void publishedSnapshotCanBeWrittenAndReadAgain()
			throws Exception {
		SentenceInfo sentence = sentence("p1", "First");
		WordTimingAudiobookMatcher matcher = matcher(sentence);
		File source = temporaryFolder.newFile("source.cache");
		cache.write(
				source,
				Collections.singletonList(
						entry("p1", 2, 3, 20, true, "a.mp3")));
		matcher.maybeReadSentenceTimingCache(source);
		File target = temporaryFolder.newFile("target.cache");

		matcher.maybeWriteSentenceTimingCache(target);

		AudiobookTimingCache.Entry restored =
				cache.read(target).get(0);
		assertEquals("p1", restored.startPos());
		assertEquals(2, restored.startTime(), 0);
		assertEquals("a.mp3", restored.audioFileName());
	}

	private WordTimingAudiobookMatcher matcher(
			SentenceInfo... sentences) throws Exception {
		File wordTimings =
				temporaryFolder.newFile();
		return new WordTimingAudiobookMatcher(
				wordTimings, Arrays.asList(sentences));
	}

	private static SentenceInfo sentence(
			String startPos, String text) {
		SentenceInfo sentence = new SentenceInfo();
		sentence.startPos = startPos;
		sentence.text = text;
		return sentence;
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
