/*
 * CoolReader for Android
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 2
 * of the License, or (at your option) any later version.
 */

package org.coolreader.crengine;

/**
 * Privacy-preserving replacement for direct android.util.Log calls.
 */
public final class Log {
	public static final int VERBOSE = android.util.Log.VERBOSE;
	public static final int DEBUG = android.util.Log.DEBUG;
	public static final int INFO = android.util.Log.INFO;
	public static final int WARN = android.util.Log.WARN;
	public static final int ERROR = android.util.Log.ERROR;
	public static final int ASSERT = android.util.Log.ASSERT;

	private static final int LEGACY_MAX_TAG_LENGTH = 23;

	private Log() {
	}

	public static int v(String tag, String message) {
		return write(VERBOSE, tag, message, null);
	}

	public static int v(String tag, String message, Throwable error) {
		return write(VERBOSE, tag, message, error);
	}

	public static int d(String tag, String message) {
		return write(DEBUG, tag, message, null);
	}

	public static int d(String tag, String message, Throwable error) {
		return write(DEBUG, tag, message, error);
	}

	public static int i(String tag, String message) {
		return write(INFO, tag, message, null);
	}

	public static int i(String tag, String message, Throwable error) {
		return write(INFO, tag, message, error);
	}

	public static int w(String tag, String message) {
		return write(WARN, tag, message, null);
	}

	public static int w(String tag, String message, Throwable error) {
		return write(WARN, tag, message, error);
	}

	public static int e(String tag, String message) {
		return write(ERROR, tag, message, null);
	}

	public static int e(String tag, String message, Throwable error) {
		return write(ERROR, tag, message, error);
	}

	private static int write(int priority, String tag, String message,
			Throwable error) {
		String safe = LogRedactor.redact(message);
		if (error != null) {
			String details = LogRedactor.describeThrowable(error);
			if (details.length() > 0)
				safe = safe + '\n' + details;
		}
		return android.util.Log.println(priority, safeTag(tag), safe);
	}

	private static String safeTag(String tag) {
		String safe = LogRedactor.redact(tag);
		if (safe.length() == 0)
			safe = "cr3";
		if (safe.length() > LEGACY_MAX_TAG_LENGTH)
			safe = safe.substring(0, LEGACY_MAX_TAG_LENGTH);
		return safe;
	}
}
