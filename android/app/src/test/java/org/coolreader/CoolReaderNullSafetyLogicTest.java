package org.coolreader;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Source-policy locks for CoolReader null-safety logical bugs found when
 * owners can be closed or not yet installed (async home, cold-start intents,
 * destroy-generation service graph).
 */
public class CoolReaderNullSafetyLogicTest {
	@Test
	public void directoryUpdatedDoesNotChainNullHomeFrame()
			throws Exception {
		String method = extractMethod(
				readCoolReaderSource(), "directoryUpdated");
		assertTrue(method.contains("homeUi.frame()"));
		assertTrue(
				method.contains("home == null")
						|| method.contains("home != null"));
		assertFalse(
				"must not chain homeUi.frame().refresh*",
				method.contains("homeUi.frame().refreshOnlineCatalogs()")
						|| method.contains(
						"homeUi.frame().refreshRecentBooks()"));
	}

	@Test
	public void processIntentDoesNotChainNullReaderViewForCommands()
			throws Exception {
		String method = extractMethod(
				readCoolReaderSource(), "processIntent");
		assertTrue(
				method.contains("readerUi.view()"));
		assertFalse(
				"must not chain readerUi.view().onCommand",
				method.contains("readerUi.view().onCommand"));
	}

	@Test
	public void onStopCloseBookGuardsNullView() throws Exception {
		String method = extractMethod(
				readCoolReaderSource(), "onStop");
		if (!method.contains("CLOSE_BOOK_ON_STOP"))
			return;
		assertFalse(
				"CLOSE_BOOK_ON_STOP must not chain readerUi.view().close()",
				method.contains("readerUi.view().close()"));
	}

	@Test
	public void showAboutDialogRequiresEngine() throws Exception {
		String method = extractMethod(
				readCoolReaderSource(), "showAboutDialog");
		assertTrue(method.contains("serviceGraph.engine()"));
		assertTrue(
				method.contains("engine == null")
						|| method.contains("engine ==null"));
	}

	@Test
	public void runInReaderCapturesServiceDepsBeforeConstruct()
			throws Exception {
		String method = extractMethod(
				readCoolReaderSource(), "runInReader");
		assertTrue(
				"must capture Engine before ReaderView construct",
				method.contains("Engine engine = serviceGraph.engine()"));
		assertTrue(
				"must capture Scanner before construct",
				method.contains("Scanner scanner = serviceGraph.scanner()"));
		assertFalse(
				"must not pass serviceGraph.engine() inline to ReaderView",
				method.contains("new ReaderView(\n")
						&& method.contains("serviceGraph.engine(),"));
	}

	private static String extractMethod(String source, String name) {
		String needle = name + "(";
		int from = 0;
		String best = null;
		while (true) {
			int idx = source.indexOf(needle, from);
			if (idx < 0)
				break;
			int lineStart = source.lastIndexOf('\n', idx) + 1;
			String prefix = source.substring(lineStart, idx).trim();
			if (prefix.isEmpty()
					|| prefix.endsWith(";")
					|| prefix.contains("=")
					|| prefix.contains(".")) {
				from = idx + needle.length();
				continue;
			}
			int brace = source.indexOf('{', idx);
			if (brace < 0)
				fail(name + " body not found");
			int end = findMatchingBrace(source, brace);
			String body = source.substring(idx, end + 1);
			// Prefer the overload with the largest body for runInReader.
			if (best == null || body.length() > best.length())
				best = body;
			from = end + 1;
		}
		if (best == null)
			fail(name + " not found");
		return best;
	}

	private static String readCoolReaderSource() throws IOException {
		Path[] candidates = new Path[] {
				Paths.get("src/org/coolreader/CoolReader.java"),
				Paths.get("../src/org/coolreader/CoolReader.java"),
				Paths.get("android/src/org/coolreader/CoolReader.java"),
		};
		for (Path path : candidates) {
			if (Files.isRegularFile(path))
				return new String(
						Files.readAllBytes(path),
						StandardCharsets.UTF_8);
		}
		fail("CoolReader.java not found");
		return "";
	}

	private static int findMatchingBrace(String source, int openIndex) {
		int depth = 0;
		for (int i = openIndex; i < source.length(); i++) {
			char c = source.charAt(i);
			if (c == '{')
				depth++;
			else if (c == '}') {
				depth--;
				if (depth == 0)
					return i;
			}
		}
		return source.length() - 1;
	}
}
