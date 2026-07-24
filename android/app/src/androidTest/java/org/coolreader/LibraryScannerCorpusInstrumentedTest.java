package org.coolreader;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Debug;
import android.os.SystemClock;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.coolreader.crengine.DocumentFormat;
import org.coolreader.crengine.FileInfo;
import org.coolreader.crengine.Scanner;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class LibraryScannerCorpusInstrumentedTest {
	private static final int BOOK_COUNT = 20_000;
	private static final long SCAN_TIMEOUT_MS = 300_000;
	private static final long MAX_DEBUG_PSS_KIB = 768L * 1024;

	@Test
	public void scansTwentyThousandBooksAndReportsTimeAndMemory()
			throws Exception {
		Instrumentation instrumentation =
				InstrumentationRegistry.getInstrumentation();
		Context target = instrumentation.getTargetContext();
		File corpus = new File(
				target.getCacheDir(),
				"scanner-corpus-" + System.nanoTime());
		assertTrue(corpus.mkdir());
		createCorpus(corpus);

		CoolReader activity = (CoolReader) launchMainActivity(
				target, instrumentation);
		Scanner.ScanControl initialControl =
				new Scanner.ScanControl();
		Scanner.ScanControl unchangedControl =
				new Scanner.ScanControl();
		try {
			AtomicReference<Scanner> scanner =
					new AtomicReference<>();
			waitUntil(() -> {
				scanner.set(activity.getServiceDependencies()
						.getScanner());
				return scanner.get() != null
						&& activity.getDB() != null;
			});

			FileInfo initialDirectory = new FileInfo(corpus);
			Measurement initial = scanAndMeasure(
					instrumentation, activity, scanner.get(),
					initialDirectory, initialControl);
			assertCorpus(initialDirectory);

			FileInfo unchangedDirectory = new FileInfo(corpus);
			Measurement unchanged = scanAndMeasure(
					instrumentation, activity, scanner.get(),
					unchangedDirectory, unchangedControl);
			assertCorpus(unchangedDirectory);

			assertTrue(initial.elapsedMs < SCAN_TIMEOUT_MS);
			assertTrue(unchanged.elapsedMs < SCAN_TIMEOUT_MS);
			long peakPssKiB = Math.max(
					initial.peakPssKiB, unchanged.peakPssKiB);
			long peakJavaHeapBytes = Math.max(
					initial.peakJavaHeapBytes,
					unchanged.peakJavaHeapBytes);
			assertTrue(
					"debug corpus PSS exceeded safety ceiling: "
							+ peakPssKiB + " KiB",
					peakPssKiB < MAX_DEBUG_PSS_KIB);

			Bundle metrics = new Bundle();
			metrics.putInt("corpus_book_count", BOOK_COUNT);
			metrics.putLong(
					"initial_scan_elapsed_ms",
					initial.elapsedMs);
			metrics.putLong(
					"unchanged_scan_elapsed_ms",
					unchanged.elapsedMs);
			metrics.putLong("peak_pss_kib", peakPssKiB);
			metrics.putLong(
					"peak_java_heap_bytes",
					peakJavaHeapBytes);
			instrumentation.sendStatus(2, metrics);
			instrumentation.addResults(metrics);
		} finally {
			initialControl.stop();
			unchangedControl.stop();
			finishActivity(activity, instrumentation);
			deleteRecursively(corpus);
		}
	}

	private static Measurement scanAndMeasure(
			Instrumentation instrumentation,
			CoolReader activity,
			Scanner scanner,
			FileInfo directory,
			Scanner.ScanControl control) throws Exception {
		CountDownLatch completed = new CountDownLatch(1);
		instrumentation.runOnMainSync(
				() -> scanner.scanDirectory(
						activity.getDB(), directory, null,
						scanControl -> completed.countDown(),
						false, control));
		long started = SystemClock.elapsedRealtime();
		long peakPssKiB = currentPssKiB();
		long peakJavaHeapBytes = currentJavaHeapBytes();
		while (!completed.await(100, TimeUnit.MILLISECONDS)) {
			peakPssKiB = Math.max(
					peakPssKiB, currentPssKiB());
			peakJavaHeapBytes = Math.max(
					peakJavaHeapBytes,
					currentJavaHeapBytes());
			if (SystemClock.elapsedRealtime() - started
					>= SCAN_TIMEOUT_MS) {
				control.stop();
				throw new AssertionError(
						"20,000-book scan exceeded "
								+ SCAN_TIMEOUT_MS + " ms");
			}
		}
		peakPssKiB = Math.max(peakPssKiB, currentPssKiB());
		peakJavaHeapBytes = Math.max(
				peakJavaHeapBytes, currentJavaHeapBytes());
		assertFalse(control.isStopped());
		return new Measurement(
				SystemClock.elapsedRealtime() - started,
				peakPssKiB, peakJavaHeapBytes);
	}

	private static void assertCorpus(FileInfo directory) {
		assertTrue(directory.isScanned);
		assertEquals(BOOK_COUNT, directory.fileCount());
		assertBook(directory.getFile(0));
		assertBook(directory.getFile(BOOK_COUNT / 2));
		assertBook(directory.getFile(BOOK_COUNT - 1));
	}

	private static void assertBook(FileInfo book) {
		assertEquals(DocumentFormat.FB2, book.format);
		assertTrue(book.crc32 != 0);
		assertNotNull(book.scanFingerprint);
	}

	private static void createCorpus(File directory) throws Exception {
		for (int index = 0; index < BOOK_COUNT; index++) {
			File book = new File(
					directory,
					String.format(
							Locale.ROOT,
							"book-%05d.fb2", index));
			String content =
					"<?xml version=\"1.0\" encoding=\"utf-8\"?>"
					+ "<FictionBook xmlns=\"http://www.gribuser.ru/"
					+ "xml/fictionbook/2.0\"><description><title-info>"
					+ "<genre>fiction</genre><author><first-name>Corpus"
					+ "</first-name><last-name>Fixture</last-name></author>"
					+ "<book-title>Synthetic book "
					+ index
					+ "</book-title><lang>en</lang></title-info>"
					+ "</description><body><section><p>Generated corpus "
					+ index
					+ ".</p></section></body></FictionBook>";
			try (FileOutputStream output =
					new FileOutputStream(book)) {
				output.write(content.getBytes(
						StandardCharsets.UTF_8));
			}
		}
	}

	private static long currentPssKiB() {
		Debug.MemoryInfo memory = new Debug.MemoryInfo();
		Debug.getMemoryInfo(memory);
		return memory.getTotalPss();
	}

	private static long currentJavaHeapBytes() {
		Runtime runtime = Runtime.getRuntime();
		return runtime.totalMemory() - runtime.freeMemory();
	}

	private static Activity launchMainActivity(
			Context target, Instrumentation instrumentation) {
		Intent launch = target.getPackageManager()
				.getLaunchIntentForPackage(target.getPackageName());
		assertNotNull(launch);
		launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
				| Intent.FLAG_ACTIVITY_CLEAR_TASK);
		Activity activity = instrumentation.startActivitySync(launch);
		assertTrue(activity instanceof CoolReader);
		return activity;
	}

	private static void waitUntil(Condition condition)
			throws Exception {
		long deadline =
				SystemClock.uptimeMillis() + 30_000;
		while (SystemClock.uptimeMillis() < deadline) {
			if (condition.evaluate())
				return;
			SystemClock.sleep(100);
		}
		throw new AssertionError(
				"CoolReader services did not become ready");
	}

	private static void finishActivity(
			Activity activity, Instrumentation instrumentation) {
		instrumentation.runOnMainSync(activity::finish);
		instrumentation.waitForIdleSync();
	}

	private static void deleteRecursively(File file) {
		if (file.isDirectory()) {
			File[] children = file.listFiles();
			assertNotNull(children);
			for (File child : children)
				deleteRecursively(child);
		}
		assertTrue(file.delete() || !file.exists());
	}

	private interface Condition {
		boolean evaluate() throws Exception;
	}

	private static final class Measurement {
		final long elapsedMs;
		final long peakPssKiB;
		final long peakJavaHeapBytes;

		Measurement(long elapsedMs, long peakPssKiB,
				long peakJavaHeapBytes) {
			this.elapsedMs = elapsedMs;
			this.peakPssKiB = peakPssKiB;
			this.peakJavaHeapBytes = peakJavaHeapBytes;
		}
	}
}
