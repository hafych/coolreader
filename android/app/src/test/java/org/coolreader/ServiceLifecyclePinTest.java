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
 * Locks CoolReader async service-generation pinning.
 *
 * After Activity/service destroy, late callbacks must not NPE on
 * {@code serviceGraph.lifecycle()} (null after close) or on
 * {@code scanner()}/{@code history()} nulls. Paths capture lifecycle only
 * via {@code pinServiceLifecycle()}.
 */
public class ServiceLifecyclePinTest {
	@Test
	public void pinServiceLifecycleGuardsActivityAndGraph()
			throws Exception {
		String method = extractMethod(
				readCoolReaderSource(), "pinServiceLifecycle");
		assertTrue(
				method.contains("activityLifecycle.isClosed()"));
		assertTrue(
				method.contains("serviceGraph.isActive()"));
		assertTrue(
				method.contains("return serviceGraph.lifecycle()"));
		assertTrue(
				method.contains("return null"));
	}

	@Test
	public void destroySensitivePathsUsePinNotRawLifecycle()
			throws Exception {
		String source = readCoolReaderSource();
		for (String methodName : new String[] {
				"showOptionsDialog",
				"finishDeletedBook",
				"removeRecentBook",
				"askDeleteCatalog",
				"askDeleteFolder",
				"deleteFolder",
				"createLogcatFile",
				"startLogcatExport",
		}) {
			String method = extractMethod(source, methodName);
			assertTrue(
					methodName + " must pin service lifecycle",
					method.contains("pinServiceLifecycle()"));
			assertFalse(
					methodName
							+ " must not raw-call serviceGraph.lifecycle()",
					method.contains(
							"serviceGraph.lifecycle()"));
		}
	}

	@Test
	public void folderDeletionEffectsNullChecksHistoryAfterGraphClose()
			throws Exception {
		String method = extractMethod(
				readCoolReaderSource(),
				"applyFolderDeletionEffects");
		// Lifecycle can outlive serviceGraph.close(); history may be null.
		assertTrue(
				method.contains("serviceGraph.history()"));
		assertTrue(
				method.contains("history == null")
						|| method.contains("history ==null"));
		assertFalse(
				"must not chain serviceGraph.history().removeBookInfo",
				method.contains(
						"serviceGraph.history().removeBookInfo"));
	}

	@Test
	public void statusUpdateAndHomeInstallTolerateClosedOwners()
			throws Exception {
		String source = readCoolReaderSource();
		String status = extractMethod(
				source, "updateCurrentPositionStatus");
		assertTrue(
				"status update must tolerate null frame after close",
				status.contains("readerUi.frame()")
						&& status.contains("frame == null"));
		assertFalse(
				"must not chain readerUi.frame().getStatusBar()",
				status.contains(
						"readerUi.frame().getStatusBar()"));

		// Home install failure must onClose the unowned CRRootView.
		int homeBlock = source.indexOf(
				"if (!homeUi.isPresent())");
		assertTrue(homeBlock >= 0);
		int brace = source.indexOf('{', homeBlock);
		int end = findMatchingBrace(source, brace);
		String block = source.substring(brace, end + 1);
		assertTrue(
				block.contains("homeUi.install(createdHome)"));
		assertTrue(
				"failed home install must onClose orphan",
				block.contains("createdHome.onClose()"));
	}

	private static String extractMethod(String source, String name) {
		// Match method declarations only: same-line return type / modifiers.
		// Call sites like "finishDeletedBook(\n  ...)" have empty prefix.
		String needle = name + "(";
		int from = 0;
		while (true) {
			int idx = source.indexOf(needle, from);
			if (idx < 0)
				fail(name + " not found");
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
			return source.substring(idx, end + 1);
		}
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
