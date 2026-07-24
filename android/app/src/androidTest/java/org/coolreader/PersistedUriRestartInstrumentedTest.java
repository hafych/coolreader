package org.coolreader;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.UriPermission;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.os.SystemClock;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import androidx.test.runner.lifecycle.Stage;

import org.coolreader.crengine.BookInfo;
import org.coolreader.crengine.ReaderView;
import org.coolreader.test.TestDocumentProvider;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class PersistedUriRestartInstrumentedTest {
	private static final long TIMEOUT_MS = 30_000;
	private static final String PREFS_NAME = "CR3LastBook";
	private static final String PREF_LAST_LOCATION = "LastLocation";
	private static final String EXPECTED_LOCATION =
			"@book:" + "content://org.coolreader.test.documents/persisted.fb2";

	@Test
	public void phase1_establishPersistedGrantAndPosition() throws Exception {
		Context target = targetContext();
		clearPreviousState(target);
		startGrantingActivity(true, "persisted");

		waitUntil(() -> hasPersistedReadGrant(
				target, TestDocumentProvider.PERSISTED_BOOK));
		CoolReader activity = waitForOpenedReader();
		assertTrue(activity.isBookOpened());
		finishActivity(activity);

		waitUntil(() -> EXPECTED_LOCATION.equals(
				target.getSharedPreferences(PREFS_NAME, 0)
						.getString(PREF_LAST_LOCATION, null)));
		waitUntil(() -> positionBookmarkCount(target) > 0);
		waitUntil(() -> stableIdentityCount(target) == 1);
	}

	@Test
	public void phase2_restoreAfterProcessRestartWithoutNewGrant()
			throws Exception {
		Context target = targetContext();
		assertTrue(hasPersistedReadGrant(
				target, TestDocumentProvider.PERSISTED_BOOK));
		assertEquals(
				EXPECTED_LOCATION,
				target.getSharedPreferences(PREFS_NAME, 0)
						.getString(PREF_LAST_LOCATION, null));
		waitUntil(() -> positionBookmarkCount(target) > 0);
		assertEquals(1, stableIdentityCount(target));
		long bookmarksBefore = positionBookmarkCount(target);

		Intent launch = target.getPackageManager()
				.getLaunchIntentForPackage(target.getPackageName());
		assertNotNull(launch);
		launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
				| Intent.FLAG_ACTIVITY_CLEAR_TASK);
		Instrumentation instrumentation =
				InstrumentationRegistry.getInstrumentation();
		Activity launched = instrumentation.startActivitySync(launch);
		assertTrue(launched instanceof CoolReader);
		CoolReader activity = (CoolReader) launched;
		waitUntil(activity::isBookOpened);

		ReaderView readerView = readerView(activity);
		BookInfo restored = readerView.getBookInfo();
		assertNotNull(restored);
		assertNotNull(restored.getLastPosition());
		finishActivity(activity);

		waitUntil(() -> positionBookmarkCount(target) == bookmarksBefore);
	}

	private static Context targetContext() {
		return InstrumentationRegistry.getInstrumentation().getTargetContext();
	}

	private static void clearPreviousState(Context target) {
		for (UriPermission permission
				: target.getContentResolver().getPersistedUriPermissions()) {
			if (TestDocumentProvider.AUTHORITY.equals(
					permission.getUri().getAuthority())) {
				target.getContentResolver().releasePersistableUriPermission(
						permission.getUri(),
						permission.isReadPermission()
								? Intent.FLAG_GRANT_READ_URI_PERMISSION : 0);
			}
		}
		SharedPreferences preferences =
				target.getSharedPreferences(PREFS_NAME, 0);
		preferences.edit().clear().commit();
		File filesDir = target.getFilesDir();
		File[] files = filesDir.listFiles(
				(dir, name) -> name.startsWith("cr3db.sqlite")
						|| name.startsWith("cr3db_cover.sqlite"));
		if (files != null) {
			for (File file : files) {
				if (file.exists() && !file.delete())
					throw new AssertionError(
							"Cannot delete stale test database " + file);
			}
		}
	}

	private static void startGrantingActivity(
			boolean persistable, String document) throws Exception {
		String command =
				"am start -W -n org.coolreader.test/"
						+ "org.coolreader.test.GrantingActivity"
						+ " --ez persistable " + persistable
						+ " --es document " + document;
		ParcelFileDescriptor output =
				InstrumentationRegistry.getInstrumentation()
						.getUiAutomation().executeShellCommand(command);
		String result;
		try (FileInputStream input =
					 new FileInputStream(output.getFileDescriptor())) {
			ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			byte[] buffer = new byte[1024];
			int count;
			while ((count = input.read(buffer)) >= 0)
				bytes.write(buffer, 0, count);
			result = new String(bytes.toByteArray(), StandardCharsets.UTF_8);
		} finally {
			output.close();
		}
		assertTrue("Granting activity failed: " + result,
				result.contains("Status: ok")
						|| result.contains("Status: warning"));
	}

	private static boolean hasPersistedReadGrant(Context target, Uri uri) {
		for (UriPermission permission
				: target.getContentResolver().getPersistedUriPermissions()) {
			if (uri.equals(permission.getUri())
					&& permission.isReadPermission())
				return true;
		}
		return false;
	}

	private static CoolReader waitForOpenedReader() throws Exception {
		AtomicReference<CoolReader> result = new AtomicReference<>();
		waitUntil(() -> {
			Instrumentation instrumentation =
					InstrumentationRegistry.getInstrumentation();
			instrumentation.runOnMainSync(() -> {
				Collection<Activity> activities =
						ActivityLifecycleMonitorRegistry.getInstance()
								.getActivitiesInStage(Stage.RESUMED);
				for (Activity activity : activities) {
					if (activity instanceof CoolReader) {
						result.set((CoolReader) activity);
						break;
					}
				}
			});
			CoolReader activity = result.get();
			return activity != null && activity.isBookOpened();
		});
		return result.get();
	}

	private static ReaderView readerView(CoolReader activity)
			throws Exception {
		Field field = CoolReader.class.getDeclaredField("mReaderView");
		field.setAccessible(true);
		return (ReaderView) field.get(activity);
	}

	private static void finishActivity(Activity activity) throws Exception {
		Instrumentation instrumentation =
				InstrumentationRegistry.getInstrumentation();
		instrumentation.runOnMainSync(activity::finish);
		waitUntil(activity::isFinishing);
		instrumentation.waitForIdleSync();
	}

	private static long queryLong(
			Context target, String query, String argument) {
		File database = new File(target.getFilesDir(), "cr3db.sqlite");
		if (!database.isFile())
			return 0;
		try (SQLiteDatabase db = SQLiteDatabase.openDatabase(
				database.getAbsolutePath(), null,
				SQLiteDatabase.OPEN_READWRITE);
			 Cursor cursor = db.rawQuery(
					 query, new String[] {argument})) {
			return cursor.moveToFirst() ? cursor.getLong(0) : 0;
		} catch (RuntimeException e) {
			return 0;
		}
	}

	private static long positionBookmarkCount(Context target) {
		return queryLong(
				target, "SELECT count(*) FROM bookmark bm "
						+ "JOIN book b ON b.id=bm.book_fk "
						+ "WHERE b.pathname=? AND bm.type=0",
				TestDocumentProvider.PERSISTED_BOOK.toString());
	}

	private static long stableIdentityCount(Context target) {
		return queryLong(
				target, "SELECT count(*) FROM book " +
						"WHERE pathname=? AND book_key LIKE 'bk1:%' " +
						"AND source_type='CONTENT_URI' " +
						"AND source_locator=pathname",
				TestDocumentProvider.PERSISTED_BOOK.toString());
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
				new AssertionError("Condition was not met within " + TIMEOUT_MS + " ms");
		if (lastFailure != null)
			timeout.initCause(lastFailure);
		throw timeout;
	}

	private interface Condition {
		boolean evaluate() throws Exception;
	}
}
