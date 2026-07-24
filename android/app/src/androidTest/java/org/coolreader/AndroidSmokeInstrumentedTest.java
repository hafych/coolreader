package org.coolreader;

import android.app.Activity;
import android.app.Instrumentation;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.os.SystemClock;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.coolreader.crengine.BookInfo;
import org.coolreader.crengine.DocumentFormat;
import org.coolreader.crengine.Engine;
import org.coolreader.crengine.FileInfo;
import org.coolreader.crengine.ReaderView;
import org.coolreader.crengine.Scanner;
import org.coolreader.crengine.Services;
import org.coolreader.crengine.ScanStopReason;
import org.coolreader.tts.OnTTSStatusListener;
import org.coolreader.tts.TTSControlBinder;
import org.coolreader.tts.TTSControlService;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class AndroidSmokeInstrumentedTest {
	private static final long TIMEOUT_MS = 30_000;
	private static final int TTS_NOTIFICATION_ID = 1;

	@Test
	public void ordinaryFileOpensFromGenericMimeIntent() throws Exception {
		Context target = targetContext();
		File document = new File(
				target.getCacheDir(), "instrumentation-local-file.fb2");
		try (FileOutputStream output = new FileOutputStream(document)) {
			output.write(testBookBytes());
			output.getFD().sync();
		}

		Instrumentation instrumentation =
				InstrumentationRegistry.getInstrumentation();
		Intent open = new Intent(Intent.ACTION_VIEW);
		open.setClass(target, CoolReader.class);
		open.setDataAndType(
				Uri.fromFile(document), "application/octet-stream");
		open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
				| Intent.FLAG_ACTIVITY_CLEAR_TASK);
		Activity launched = instrumentation.startActivitySync(open);
		assertTrue(launched instanceof CoolReader);
		CoolReader activity = (CoolReader) launched;
		waitUntil(activity::isBookOpened);

		ReaderView readerView = readerView(activity);
		BookInfo bookInfo = readerView.getBookInfo();
		assertNotNull(bookInfo);
		assertNotNull(bookInfo.getFileInfo());
		assertEquals(
				document.getCanonicalPath(),
				new File(bookInfo.getFileInfo().pathname).getCanonicalPath());
		assertNotNull(bookInfo.getFileInfo().bookKey);
		assertTrue(bookInfo.getFileInfo().bookKey.startsWith("bk1:"));
		assertEquals("FILE", bookInfo.getFileInfo().sourceType);
		assertNotNull(bookInfo.getFileInfo().contentHash);
		assertEquals(64, bookInfo.getFileInfo().contentHash.length());

		finishActivity(activity);
		assertTrue(document.delete());
	}

	@Test
	public void archiveEntryMetadataDoesNotRequireServiceLocator()
			throws Exception {
		Context target = targetContext();
		File archive = new File(
				target.getCacheDir(), "archive-entry-metadata.zip");
		byte[] body = "archive entry".getBytes(StandardCharsets.UTF_8);
		try {
			try (ZipOutputStream output = new ZipOutputStream(
					new FileOutputStream(archive))) {
				output.putNextEntry(new ZipEntry("folder/book.fb2"));
				output.write(body);
				output.closeEntry();
			}

			FileInfo info = new FileInfo(
					archive.getAbsolutePath()
							+ FileInfo.ARC_SEPARATOR
							+ "folder/book.fb2");
			assertTrue(info.isArchive);
			assertEquals(archive.getAbsolutePath(), info.arcname);
			assertEquals("folder/book.fb2", info.pathname);
			assertEquals("book.fb2", info.filename);
			assertEquals(body.length, info.size);
			assertEquals(DocumentFormat.FB2, info.format);
		} finally {
			assertTrue(archive.delete() || !archive.exists());
		}
	}

	@Test
	public void metadataScanProcessesMoreThanOneBoundedBatch()
			throws Exception {
		Context target = targetContext();
		File directory = new File(
				target.getCacheDir(),
				"metadata-batches-" + System.nanoTime());
		assertTrue(directory.mkdir());
		final int fileCount = 130;
		File firstDocument = null;
		for (int i = 0; i < fileCount; i++) {
			File document = new File(
					directory, "book-" + i + ".txt");
			if (i == 0)
				firstDocument = document;
			try (FileOutputStream output =
					new FileOutputStream(document)) {
				output.write(("Book " + i + "\nMetadata batch test.")
						.getBytes(StandardCharsets.UTF_8));
			}
		}
		File nested = new File(directory, "nested");
		File deeplyNested = new File(nested, "deeper");
		assertTrue(nested.mkdir());
		assertTrue(deeplyNested.mkdir());
		for (int i = 0; i < 3; i++) {
			File document = new File(
					deeplyNested, "nested-book-" + i + ".txt");
			try (FileOutputStream output =
					new FileOutputStream(document)) {
				output.write(("Nested book " + i)
						.getBytes(StandardCharsets.UTF_8));
			}
		}

		CoolReader activity = (CoolReader) launchMainActivity(
				target, InstrumentationRegistry.getInstrumentation());
		Scanner.ScanControl control = new Scanner.ScanControl();
		try {
			AtomicReference<Scanner> scanner = new AtomicReference<>();
			waitUntil(() -> {
				scanner.set(Services.getScanner());
				return activity.getDB() != null;
			});
			FileInfo baseDir = new FileInfo(directory);
			CountDownLatch completed = new CountDownLatch(1);
			InstrumentationRegistry.getInstrumentation().runOnMainSync(
					() -> scanner.get().scanDirectory(
							activity.getDB(), baseDir, null,
							scanControl -> completed.countDown(),
							true, control));

			assertTrue(completed.await(
					TIMEOUT_MS, TimeUnit.MILLISECONDS));
			assertFalse(control.isStopped());
			assertTrue(baseDir.isScanned);
			assertEquals(fileCount, baseDir.fileCount());
			for (int i = 0; i < baseDir.fileCount(); i++) {
				FileInfo file = baseDir.getFile(i);
				assertEquals(DocumentFormat.TXT, file.format);
				assertTrue(file.crc32 != 0);
			}
			assertEquals(1, baseDir.dirCount());
			FileInfo nestedInfo = baseDir.getDir(0);
			assertTrue(nestedInfo.isScanned);
			assertEquals(1, nestedInfo.dirCount());
			FileInfo deeplyNestedInfo = nestedInfo.getDir(0);
			assertTrue(deeplyNestedInfo.isScanned);
			assertEquals(3, deeplyNestedInfo.fileCount());
			for (int i = 0;
					i < deeplyNestedInfo.fileCount(); i++)
				assertTrue(
						deeplyNestedInfo.getFile(i).crc32 != 0);

			assertNotNull(firstDocument);
			FileInfo initialFirst = baseDir.findItemByPathName(
					firstDocument.getAbsolutePath());
			assertNotNull(initialFirst);
			assertNotNull(initialFirst.scanFingerprint);
			long initialCrc = initialFirst.crc32;
			String initialFingerprint =
					initialFirst.scanFingerprint;

			AtomicReference<String> directoryFingerprint =
					new AtomicReference<>();
			CountDownLatch fingerprintLoaded =
					new CountDownLatch(1);
			File finalFirstDocument = firstDocument;
			InstrumentationRegistry.getInstrumentation().runOnMainSync(
					() -> activity.getDB()
							.loadDirectoryFingerprint(
									directory.getAbsolutePath(),
									value -> {
										directoryFingerprint.set(value);
										fingerprintLoaded.countDown();
									}));
			assertTrue(fingerprintLoaded.await(
					TIMEOUT_MS, TimeUnit.MILLISECONDS));
			assertNotNull(directoryFingerprint.get());

			FileInfo unchangedBaseDir =
					new FileInfo(directory);
			Scanner.ScanControl unchangedControl =
					new Scanner.ScanControl();
			CountDownLatch unchangedCompleted =
					new CountDownLatch(1);
			InstrumentationRegistry.getInstrumentation().runOnMainSync(
					() -> scanner.get().scanDirectory(
							activity.getDB(), unchangedBaseDir, null,
							scanControl ->
									unchangedCompleted.countDown(),
							true, unchangedControl));
			assertTrue(unchangedCompleted.await(
					TIMEOUT_MS, TimeUnit.MILLISECONDS));
			FileInfo unchangedFirst =
					unchangedBaseDir.findItemByPathName(
							finalFirstDocument.getAbsolutePath());
			assertNotNull(unchangedFirst);
			assertEquals(initialCrc, unchangedFirst.crc32);
			assertEquals(
					initialFingerprint,
					unchangedFirst.scanFingerprint);

			long previousTimestamp =
					finalFirstDocument.lastModified();
			try (FileOutputStream output =
					new FileOutputStream(finalFirstDocument)) {
				output.write(
						"Cook 0\nMetadata batch test."
								.getBytes(StandardCharsets.UTF_8));
				output.getFD().sync();
			}
			assertTrue(finalFirstDocument.setLastModified(
					Math.max(
							System.currentTimeMillis(),
							previousTimestamp + 2_000)));
			FileInfo changedBaseDir = new FileInfo(directory);
			Scanner.ScanControl changedControl =
					new Scanner.ScanControl();
			CountDownLatch changedCompleted =
					new CountDownLatch(1);
			InstrumentationRegistry.getInstrumentation().runOnMainSync(
					() -> scanner.get().scanDirectory(
							activity.getDB(), changedBaseDir, null,
							scanControl ->
									changedCompleted.countDown(),
							true, changedControl));
			assertTrue(changedCompleted.await(
					TIMEOUT_MS, TimeUnit.MILLISECONDS));
			FileInfo changedFirst =
					changedBaseDir.findItemByPathName(
							finalFirstDocument.getAbsolutePath());
			assertNotNull(changedFirst);
			assertNotEquals(
					initialFingerprint,
					changedFirst.scanFingerprint);
			assertNotEquals(initialCrc, changedFirst.crc32);

			Scanner.ScanControl cancelled =
					new Scanner.ScanControl();
			cancelled.stop();
			FileInfo cancelledBaseDir = new FileInfo(directory);
			CountDownLatch cancellationCompleted =
					new CountDownLatch(1);
			InstrumentationRegistry.getInstrumentation().runOnMainSync(
					() -> scanner.get().scanDirectory(
							activity.getDB(), cancelledBaseDir, null,
							scanControl ->
									cancellationCompleted.countDown(),
							false, cancelled));
			assertTrue(cancellationCompleted.await(
					TIMEOUT_MS, TimeUnit.MILLISECONDS));
			assertTrue(cancelled.isStopped());
			assertFalse(cancelledBaseDir.isScanned);

			Scanner.ScanControl entryLimited =
					new Scanner.ScanControl(64, 256);
			FileInfo entryLimitedBaseDir =
					new FileInfo(directory);
			CountDownLatch entryLimitCompleted =
					new CountDownLatch(1);
			InstrumentationRegistry.getInstrumentation().runOnMainSync(
					() -> scanner.get().scanDirectory(
							activity.getDB(), entryLimitedBaseDir, null,
							scanControl ->
									entryLimitCompleted.countDown(),
							false, entryLimited));
			assertTrue(entryLimitCompleted.await(
					TIMEOUT_MS, TimeUnit.MILLISECONDS));
			assertEquals(
					ScanStopReason.ENTRY_LIMIT,
					entryLimited.getStopReason());
			assertEquals(
					64, entryLimited.getDiscoveredEntryCount());
			assertFalse(entryLimitedBaseDir.isScanned);

			Scanner.ScanControl depthLimited =
					new Scanner.ScanControl(1_000, 2);
			FileInfo depthLimitedBaseDir =
					new FileInfo(directory);
			CountDownLatch depthLimitCompleted =
					new CountDownLatch(1);
			InstrumentationRegistry.getInstrumentation().runOnMainSync(
					() -> scanner.get().scanDirectory(
							activity.getDB(), depthLimitedBaseDir, null,
							scanControl ->
									depthLimitCompleted.countDown(),
							true, depthLimited));
			assertTrue(depthLimitCompleted.await(
					TIMEOUT_MS, TimeUnit.MILLISECONDS));
			assertEquals(
					ScanStopReason.DEPTH_LIMIT,
					depthLimited.getStopReason());
			assertFalse(depthLimitedBaseDir.isScanned);
		} finally {
			control.stop();
			finishActivity(activity);
			deleteRecursively(directory);
		}
	}

	@Test
	public void scopedStorageUsesOnlyPrivateFilesystemRoots() throws Exception {
		Context target = targetContext();
		PackageInfo packageInfo = target.getPackageManager().getPackageInfo(
				target.getPackageName(), PackageManager.GET_PERMISSIONS);
		String[] requestedPermissions = packageInfo.requestedPermissions;
		if (requestedPermissions != null) {
			for (String permission : requestedPermissions) {
				assertFalse(android.Manifest.permission.READ_EXTERNAL_STORAGE
						.equals(permission));
				assertFalse(android.Manifest.permission.WRITE_EXTERNAL_STORAGE
						.equals(permission));
				assertFalse("android.permission.MANAGE_EXTERNAL_STORAGE"
						.equals(permission));
			}
		}

		Activity activity = launchMainActivity(
				target, InstrumentationRegistry.getInstrumentation());
		try {
			assertTrue(Engine.getMountedRootsMap().isEmpty());
			Engine engine = Services.getEngine();
			for (String path : engine.getAppPrivateDirs().keySet()) {
				File directory = new File(path);
				assertTrue(isUnder(directory, target.getFilesDir())
						|| isUnder(directory, target.getCacheDir()));
			}
			assertTrue(new File(target.getCacheDir(), "engine").isDirectory());
		} finally {
			finishActivity(activity);
		}
	}

	@Test
	public void ttsNotificationActionsReachPrivateServiceReceiver()
			throws Exception {
		Context target = targetContext();
		Instrumentation instrumentation =
				InstrumentationRegistry.getInstrumentation();
		Activity activity = launchMainActivity(target, instrumentation);

		AtomicReference<TTSControlBinder> binderRef = new AtomicReference<>();
		CountDownLatch connected = new CountDownLatch(1);
		ServiceConnection connection = new ServiceConnection() {
			@Override
			public void onServiceConnected(
					ComponentName name, IBinder service) {
				binderRef.set((TTSControlBinder) service);
				connected.countDown();
			}

			@Override
			public void onServiceDisconnected(ComponentName name) {
				binderRef.set(null);
			}
		};
		Intent serviceIntent = new Intent(target, TTSControlService.class);
		assertTrue(target.bindService(
				serviceIntent, connection, Context.BIND_AUTO_CREATE));
		try {
			assertTrue(connected.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
			TTSControlBinder binder = binderRef.get();
			assertNotNull(binder);
			SmokeTtsListener listener = new SmokeTtsListener();
			binder.setStatusListener(listener);

			Intent prepare = new Intent(
					TTSControlService.TTS_CONTROL_ACTION_PREPARE,
					Uri.EMPTY, target, TTSControlService.class);
			prepare.putExtra("bookTitle", "Instrumentation smoke");
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
				target.startForegroundService(prepare);
			else
				target.startService(prepare);

			NotificationManager manager =
					target.getSystemService(NotificationManager.class);
			assertNotNull(manager);
			Notification notification = waitForTtsNotification(manager);
			assertNotNull(notification.actions);
			assertEquals(4, notification.actions.length);

			assertTrue(activity.moveTaskToBack(true));
			instrumentation.waitForIdleSync();
			sendAndAwait(notification.actions[0].actionIntent,
					listener.currentRequested);
			sendAndAwait(notification.actions[1].actionIntent,
					listener.previousRequested);
			sendAndAwait(notification.actions[2].actionIntent,
					listener.nextRequested);
			sendAndAwait(notification.actions[3].actionIntent,
					listener.stopRequested);
			waitUntil(() -> findTtsNotification(manager) == null);
			binder.setStatusListener(null);
		} finally {
			target.unbindService(connection);
			target.stopService(serviceIntent);
			finishActivity(activity);
		}
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

	private static void sendAndAwait(
			PendingIntent action, CountDownLatch callback)
			throws PendingIntent.CanceledException, InterruptedException {
		assertNotNull(action);
		assertEquals("org.coolreader", action.getCreatorPackage());
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
			assertTrue(action.isImmutable());
		action.send();
		assertTrue(callback.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
	}

	private static Notification waitForTtsNotification(
			NotificationManager manager) throws Exception {
		AtomicReference<Notification> result = new AtomicReference<>();
		waitUntil(() -> {
			result.set(findTtsNotification(manager));
			return result.get() != null;
		});
		return result.get();
	}

	private static Notification findTtsNotification(
			NotificationManager manager) {
		for (android.service.notification.StatusBarNotification active
				: manager.getActiveNotifications()) {
			if (active.getId() == TTS_NOTIFICATION_ID)
				return active.getNotification();
		}
		return null;
	}

	private static Context targetContext() {
		return InstrumentationRegistry.getInstrumentation().getTargetContext();
	}

	private static boolean isUnder(File path, File root) throws Exception {
		String canonicalPath = path.getCanonicalPath();
		String canonicalRoot = root.getCanonicalPath();
		return canonicalPath.equals(canonicalRoot)
				|| canonicalPath.startsWith(canonicalRoot + File.separator);
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

	private static ReaderView readerView(CoolReader activity)
			throws Exception {
		Field field = CoolReader.class.getDeclaredField("mReaderView");
		field.setAccessible(true);
		return (ReaderView) field.get(activity);
	}

	private static void finishActivity(Activity activity) {
		Instrumentation instrumentation =
				InstrumentationRegistry.getInstrumentation();
		instrumentation.runOnMainSync(activity::finish);
		instrumentation.waitForIdleSync();
	}

	private static byte[] testBookBytes() {
		return ("<?xml version=\"1.0\" encoding=\"utf-8\"?>"
				+ "<FictionBook xmlns=\"http://www.gribuser.ru/xml/fictionbook/2.0\">"
				+ "<description><title-info><genre>fiction</genre>"
				+ "<author><first-name>Smoke</first-name>"
				+ "<last-name>Test</last-name></author>"
				+ "<book-title>Local file smoke test</book-title>"
				+ "<lang>en</lang></title-info></description>"
				+ "<body><section><title><p>Test</p></title>"
				+ "<p>Content opened from an ordinary file.</p>"
				+ "</section></body></FictionBook>")
				.getBytes(StandardCharsets.UTF_8);
	}

	private static void waitUntil(Condition condition) throws Exception {
		long deadline = SystemClock.uptimeMillis() + TIMEOUT_MS;
		Throwable lastFailure = null;
		while (SystemClock.uptimeMillis() < deadline) {
			try {
				if (condition.evaluate())
					return;
			} catch (Throwable failure) {
				lastFailure = failure;
			}
			SystemClock.sleep(100);
		}
		AssertionError timeout =
				new AssertionError(
						"Condition was not met within " + TIMEOUT_MS + " ms");
		if (lastFailure != null)
			timeout.initCause(lastFailure);
		throw timeout;
	}

	private interface Condition {
		boolean evaluate() throws Exception;
	}

	private static class SmokeTtsListener implements OnTTSStatusListener {
		final CountDownLatch currentRequested = new CountDownLatch(1);
		final CountDownLatch nextRequested = new CountDownLatch(1);
		final CountDownLatch previousRequested = new CountDownLatch(1);
		final CountDownLatch stopRequested = new CountDownLatch(1);

		@Override
		public void onUtteranceStart() {
		}

		@Override
		public void onUtteranceDone() {
		}

		@Override
		public void onError(int errorCode) {
		}

		@Override
		public void onStateChanged(TTSControlService.State state) {
		}

		@Override
		public void onVolumeChanged(int currentVolume, int maxVolume) {
		}

		@Override
		public void onAudioFocusLost() {
		}

		@Override
		public void onAudioFocusRestored() {
		}

		@Override
		public void onCurrentSentenceRequested(TTSControlBinder binder) {
			currentRequested.countDown();
		}

		@Override
		public void onNextSentenceRequested(TTSControlBinder binder) {
			nextRequested.countDown();
		}

		@Override
		public void onPreviousSentenceRequested(TTSControlBinder binder) {
			previousRequested.countDown();
		}

		@Override
		public void onStopRequested(TTSControlBinder binder) {
			stopRequested.countDown();
		}
	}
}
