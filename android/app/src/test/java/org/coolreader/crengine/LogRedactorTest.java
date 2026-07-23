package org.coolreader.crengine;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class LogRedactorTest {
	@Test
	public void leavesNonSensitiveDiagnosticsUseful() {
		assertEquals("Rendered 42 pages in 120 ms",
				LogRedactor.redact("Rendered 42 pages in 120 ms"));
	}

	@Test
	public void removesCredentialsUrlsQueriesAndPaths() {
		String input = "GET https://alice:url-secret@example.test/private/book.epub"
				+ "?access_token=query-secret#fragment "
				+ "Authorization: Bearer bearer-secret "
				+ "password=password-secret token=token-secret "
				+ "\"api_key\":\"json-secret\" "
				+ "file=/storage/emulated/0/Books/private-title.fb2 "
				+ "windows=C:\\Users\\alice\\Books\\private-title.epub "
				+ "relative=cache/private.sqlite";

		String safe = LogRedactor.redact(input);

		assertNoCanaries(safe);
		assertTrue(safe.contains("[redacted]"));
		assertTrue(safe.contains("[uri redacted]"));
		assertTrue(safe.contains("[path redacted]"));
	}

	@Test
	public void removesPrivateRelativeBookNames() {
		String safe = LogRedactor.redact(
				"Failed to open My_Private_Diary.epub");
		assertFalse(safe.contains("My_Private_Diary"));
		assertTrue(safe.contains("[file redacted]"));
	}

	@Test
	public void artifactFilteringPreservesLineBoundaries() {
		String safe = LogRedactor.redactArtifact(
				"first token=line-one-secret\n"
						+ "second /data/user/0/org.coolreader/private.db\n");
		assertEquals(2, safe.chars().filter(value -> value == '\n').count());
		assertFalse(safe.contains("line-one-secret"));
		assertFalse(safe.contains("/data/user"));
	}

	@Test
	public void throwableDescriptionDropsMessagesAndFileNames() {
		IllegalStateException cause =
				new IllegalStateException("token=cause-secret");
		Exception error = new Exception(
				"Failed at /Users/alice/private/book.fb2?token=exception-secret",
				cause);
		error.setStackTrace(new StackTraceElement[] {
				new StackTraceElement("org.coolreader.Reader",
						"load", "/Users/alice/private/Reader.java", 42)
		});

		String safe = LogRedactor.describeThrowable(error);

		assertNoCanaries(safe);
		assertTrue(safe.contains("java.lang.Exception"));
		assertTrue(safe.contains("org.coolreader.Reader.load(line 42)"));
	}

	@Test
	public void sanitizedCrashIsDetachedFromOriginalThrowableData() {
		Exception original = new Exception(
				"password=crash-secret",
				new IllegalArgumentException(
						"https://example.test/book?token=cause-query-secret"));
		original.setStackTrace(new StackTraceElement[] {
				new StackTraceElement("org.coolreader.Reader", "open",
						"/data/user/0/org.coolreader/private.epub", 7)
		});

		Throwable safe = LogRedactor.sanitizeThrowable(original);

		assertNotNull(safe);
		assertFalse(safe == original);
		assertNoCanaries(collectThrowable(safe));
		assertTrue(safe.getStackTrace()[0].getFileName() == null);
	}

	private static String collectThrowable(Throwable error) {
		StringBuilder result = new StringBuilder();
		for (Throwable current = error; current != null;
				current = current.getCause()) {
			result.append(current.getMessage()).append('\n');
			for (StackTraceElement frame : current.getStackTrace())
				result.append(frame.toString()).append('\n');
		}
		return result.toString();
	}

	private static void assertNoCanaries(String safe) {
		String[] canaries = {
				"url-secret", "query-secret", "fragment", "bearer-secret",
				"password-secret", "token-secret", "json-secret",
				"private-title", "cache/private.sqlite",
				"/storage/emulated", "C:\\Users", "cause-secret",
				"exception-secret", "/Users/alice", "crash-secret",
				"cause-query-secret", "/data/user"
		};
		for (String canary : canaries)
			assertFalse("Leaked canary: " + canary, safe.contains(canary));
	}
}
