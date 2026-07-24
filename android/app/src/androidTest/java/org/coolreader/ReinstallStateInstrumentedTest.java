package org.coolreader;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.coolreader.test.TestDocumentProvider;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class ReinstallStateInstrumentedTest {
	@Test
	public void reinstallDoesNotAssumeOldGrantOrRestorePrivateState() {
		Bundle arguments =
				InstrumentationRegistry.getArguments();
		Assume.assumeTrue("true".equals(
				arguments.getString("cleanReinstall")));
		Context target =
				InstrumentationRegistry.getInstrumentation().getTargetContext();

		boolean staleGrant = false;
		for (android.content.UriPermission permission
				: target.getContentResolver().getPersistedUriPermissions()) {
			if (TestDocumentProvider.PERSISTED_BOOK.equals(permission.getUri()))
				staleGrant = true;
		}
		assertFalse(staleGrant);
		assertNull(target.getSharedPreferences("CR3LastBook", 0)
				.getString("LastLocation", null));
		assertFalse(new File(
				target.getFilesDir(), "cr3db.sqlite").exists());

		Intent launch = target.getPackageManager()
				.getLaunchIntentForPackage(target.getPackageName());
		assertNotNull(launch);
		launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
				| Intent.FLAG_ACTIVITY_CLEAR_TASK);
		Instrumentation instrumentation =
				InstrumentationRegistry.getInstrumentation();
		Activity launched = instrumentation.startActivitySync(launch);
		assertTrue(launched instanceof CoolReader);
		assertFalse(((CoolReader) launched).isBookOpened());
		instrumentation.runOnMainSync(launched::finish);
		instrumentation.waitForIdleSync();
	}
}
