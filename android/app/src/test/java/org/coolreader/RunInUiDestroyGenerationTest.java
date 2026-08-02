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
 * Locks destroy-generation guards on CoolReader runInReader / runInBrowser.
 *
 * After Activity/service close, late CRDB callbacks must not construct a
 * ReaderView (native create already posted in ctor) or throw on install
 * rejection — they must destroy the orphan and return.
 */
public class RunInUiDestroyGenerationTest {
	@Test
	public void runInReaderGuardsDestroyGenerationAndDestroysOrphan()
			throws Exception {
		String method = extractMethod(
				readCoolReaderSource(), "runInReader");
		assertTrue(
				"runInReader must bail when Activity is closed",
				method.contains("activityLifecycle.isClosed()"));
		assertTrue(
				"runInReader must bail when service graph is inactive",
				method.contains("serviceGraph.isActive()"));
		assertTrue(
				"failed install must destroy the unowned ReaderView",
				method.contains("createdReader.destroy()"));
		assertFalse(
				"must not throw when install rejects closed owner",
				method.contains("reader UI already installed"));
	}

	@Test
	public void runInBrowserGuardsDestroyGenerationAndClosesOrphan()
			throws Exception {
		String method = extractMethod(
				readCoolReaderSource(), "runInBrowser");
		assertTrue(
				"runInBrowser must bail when Activity is closed",
				method.contains("activityLifecycle.isClosed()"));
		assertTrue(
				"runInBrowser must bail when service graph is inactive",
				method.contains("serviceGraph.isActive()"));
		assertTrue(
				"failed install must onClose the unowned FileBrowser",
				method.contains("createdBrowser.onClose()"));
	}

	@Test
	public void homeCreationUsesServiceGraphIsActiveNotNullLifecycle()
			throws Exception {
		String source = readCoolReaderSource();
		int onStart = source.indexOf("void onStart()");
		assertTrue(onStart >= 0);
		// Slice onStart body through first waitForCRDBService home block.
		int homeBlock = source.indexOf(
				"if (!homeUi.isPresent())", onStart);
		assertTrue(homeBlock >= 0);
		int brace = source.indexOf('{', homeBlock);
		int end = findMatchingBrace(source, brace);
		String block = source.substring(brace, end + 1);
		assertTrue(
				"home must use serviceGraph.isActive() (lifecycle() null after close)",
				block.contains("serviceGraph.isActive()"));
		assertFalse(
				"home must not NPE on serviceGraph.lifecycle().isActive() after close",
				block.contains(
						"serviceGraph.lifecycle().isActive()"));
		assertTrue(
				"failed home install must onClose orphan root",
				block.contains("createdHome.onClose()"));
	}

	private static String extractMethod(String source, String name) {
		// Prefer the overload with body that contains waitForCRDBService.
		int from = 0;
		while (true) {
			int idx = source.indexOf(name + "(", from);
			if (idx < 0)
				fail(name + " not found");
			// Walk back to method start keyword-ish
			int brace = source.indexOf('{', idx);
			if (brace < 0)
				fail(name + " body not found");
			int end = findMatchingBrace(source, brace);
			String body = source.substring(idx, end + 1);
			if (body.contains("waitForCRDBService"))
				return body;
			from = end + 1;
		}
	}

	private static String readCoolReaderSource() throws IOException {
		Path[] candidates = new Path[] {
				Paths.get("src/org/coolreader/CoolReader.java"),
				Paths.get("../src/org/coolreader/CoolReader.java"),
				Paths.get("android/src/org/coolreader/CoolReader.java"),
				Paths.get(
						System.getProperty("user.dir", "."),
						"src/org/coolreader/CoolReader.java"),
				Paths.get(
						System.getProperty("user.dir", "."),
						"../src/org/coolreader/CoolReader.java"),
		};
		for (Path path : candidates) {
			if (Files.isRegularFile(path))
				return new String(
						Files.readAllBytes(path),
						StandardCharsets.UTF_8);
		}
		fail("CoolReader.java not found from "
				+ Paths.get("").toAbsolutePath());
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
