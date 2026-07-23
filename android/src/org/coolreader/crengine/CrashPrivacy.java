/*
 * CoolReader for Android
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 2
 * of the License, or (at your option) any later version.
 */

package org.coolreader.crengine;

import android.os.Process;

public final class CrashPrivacy {
	private static boolean installed;

	private CrashPrivacy() {
	}

	public static synchronized void install() {
		if (installed)
			return;
		final Thread.UncaughtExceptionHandler previous =
				Thread.getDefaultUncaughtExceptionHandler();
		Thread.setDefaultUncaughtExceptionHandler((thread, error) -> {
			Throwable safe = LogRedactor.sanitizeThrowable(error);
			if (previous != null) {
				String originalName = thread.getName();
				try {
					thread.setName("cr3-crashed-thread");
					previous.uncaughtException(thread, safe);
				} finally {
					thread.setName(originalName);
				}
			} else {
				Log.e("cr3", "Uncaught exception", safe);
				Process.killProcess(Process.myPid());
				System.exit(10);
			}
		});
		installed = true;
	}
}
