package org.coolreader.crengine;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class FeedTimestampParserTest {
	@Test
	public void utcTimestampIgnoresDefaultTimezone() {
		TimeZone original = TimeZone.getDefault();
		try {
			TimeZone.setDefault(TimeZone.getTimeZone("GMT+09:00"));
			assertEquals(
					1_306_837_702_000L,
					FeedTimestampParser.parse(
							"2011-05-31T10:28:22Z"));
		} finally {
			TimeZone.setDefault(original);
		}
	}

	@Test
	public void colonAndCompactOffsetsAreEquivalent() {
		assertEquals(
				1_306_823_302_000L,
				FeedTimestampParser.parse(
						"2011-05-31T10:28:22+04:00"));
		assertEquals(
				1_306_823_302_000L,
				FeedTimestampParser.parse(
						"2011-05-31T10:28:22+0400"));
		assertEquals(
				1_306_850_302_000L,
				FeedTimestampParser.parse(
						"2011-05-31T10:28:22-03:30"));
	}

	@Test
	public void malformedTimestampIsRejected() {
		assertEquals(0, FeedTimestampParser.parse(null));
		assertEquals(0, FeedTimestampParser.parse(""));
		assertEquals(
				0,
				FeedTimestampParser.parse(
						"2011-02-30T10:28:22Z"));
		assertEquals(
				0,
				FeedTimestampParser.parse(
						"2011-05-31T10:28:22+24:00"));
		assertEquals(
				0,
				FeedTimestampParser.parse(
						"2011-05-31T10:28:22Ztrailing"));
	}

	@Test
	public void concurrentParsingHasNoSharedFormatterState()
			throws Exception {
		ExecutorService executor = Executors.newFixedThreadPool(4);
		try {
			List<Callable<Boolean>> tasks = new ArrayList<>();
			for (int thread = 0; thread < 8; thread++) {
				tasks.add(() -> {
					for (int i = 0; i < 250; i++) {
						if (FeedTimestampParser.parse(
								"2011-05-31T10:28:22+04:00")
								!= 1_306_823_302_000L)
							return false;
					}
					return true;
				});
			}
			for (Future<Boolean> result : executor.invokeAll(tasks))
				assertTrue(result.get());
		} finally {
			executor.shutdownNow();
			assertTrue(executor.awaitTermination(
					2, TimeUnit.SECONDS));
		}
	}
}
