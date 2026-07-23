/*
 * CoolReader for Android
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 2
 * of the License, or (at your option) any later version.
 */

package org.coolreader.crengine;

import java.util.IdentityHashMap;
import java.util.regex.Pattern;

/**
 * Removes user-controlled secrets and locations before diagnostics leave the
 * process. Keep this class free of Android dependencies so the contract can be
 * covered by ordinary unit tests.
 */
public final class LogRedactor {
	private static final String REDACTED = "[redacted]";
	private static final int MAX_LOG_LINE_CHARS = 4000;
	private static final int MAX_THROWABLE_DEPTH = 8;
	private static final int MAX_STACK_FRAMES = 64;

	private static final Pattern URI = Pattern.compile(
			"(?i)\\b(?:https?|ftp|file|content)://[^\\s<>\"']+");
	private static final Pattern AUTHORIZATION = Pattern.compile(
			"(?i)\\b(?:basic|bearer)\\s+[A-Za-z0-9._~+/=-]+");
	private static final Pattern SENSITIVE_ASSIGNMENT = Pattern.compile(
			"(?i)(?<![A-Za-z0-9_])[\"']?(?:pass(?:word|wd)?|pwd|"
					+ "token|access[_-]?token|"
					+ "refresh[_-]?token|authorization|auth|api[_-]?key|"
					+ "secret|session|cookie)[\"']?(?:\\s*[:=]\\s*|\\s+)"
					+ "(?:\"[^\"]*\"|'[^']*'|[^\\s,;&]+)");
	private static final Pattern QUERY_OR_FRAGMENT = Pattern.compile(
			"[?#][^\\s<>\"']+");
	private static final Pattern WINDOWS_PATH = Pattern.compile(
			"(?i)(?<![A-Za-z0-9._-])[A-Z]:\\\\[^\\r\\n,;\"']+");
	private static final Pattern UNIX_PATH = Pattern.compile(
			"(?<![A-Za-z0-9._-])/[^\\r\\n,;\"']+");
	private static final Pattern RELATIVE_PATH = Pattern.compile(
			"(?<![A-Za-z0-9._-])[^\\s,;\"']*[\\\\/][^\\r\\n,;\"']+");
	private static final Pattern PRIVATE_FILENAME = Pattern.compile(
			"(?i)(?<![A-Za-z0-9._-])[^\\s,;\"']+\\."
					+ "(?:azw3?|chm|djvu|docx?|epub|fb2|fb3|html?|mobi|"
					+ "odt|pdb|pdf|pml|prc|rtf|tcr|txt|xhtml|zip)\\b");
	private static final Pattern SAFE_TYPE = Pattern.compile(
			"[^A-Za-z0-9_.$]");

	private LogRedactor() {
	}

	public static String redact(String message) {
		if (message == null)
			return "";
		String safe = message.replace('\r', ' ').replace('\n', ' ');
		safe = URI.matcher(safe).replaceAll("[uri redacted]");
		safe = AUTHORIZATION.matcher(safe).replaceAll(REDACTED);
		safe = SENSITIVE_ASSIGNMENT.matcher(safe).replaceAll(REDACTED);
		safe = QUERY_OR_FRAGMENT.matcher(safe).replaceAll(REDACTED);
		safe = WINDOWS_PATH.matcher(safe).replaceAll("[path redacted]");
		safe = UNIX_PATH.matcher(safe).replaceAll("[path redacted]");
		safe = RELATIVE_PATH.matcher(safe).replaceAll("[path redacted]");
		safe = PRIVATE_FILENAME.matcher(safe).replaceAll("[file redacted]");
		safe = replaceControlCharacters(safe);
		if (safe.length() > MAX_LOG_LINE_CHARS)
			safe = safe.substring(0, MAX_LOG_LINE_CHARS) + " [truncated]";
		return safe;
	}

	/**
	 * Redacts every line independently while preserving line boundaries in an
	 * explicitly exported diagnostic file.
	 */
	public static String redactArtifact(String artifact) {
		if (artifact == null || artifact.length() == 0)
			return "";
		StringBuilder safe = new StringBuilder(artifact.length());
		int start = 0;
		for (int i = 0; i <= artifact.length(); i++) {
			if (i == artifact.length() || artifact.charAt(i) == '\n') {
				safe.append(redact(artifact.substring(start, i)));
				if (i < artifact.length())
					safe.append('\n');
				start = i + 1;
			}
		}
		return safe.toString();
	}

	public static String describeThrowable(Throwable error) {
		if (error == null)
			return "";
		StringBuilder description = new StringBuilder();
		appendThrowable(description, error,
				new IdentityHashMap<Throwable, Boolean>(), 0, "");
		return description.toString();
	}

	/**
	 * Produces a detached throwable for Android's uncaught-exception handler.
	 * It contains no original message, file name, suppressed object or cause.
	 */
	public static Throwable sanitizeThrowable(Throwable error) {
		if (error == null)
			return new SanitizedCrashException("unknown", null);
		return copyThrowable(error, new IdentityHashMap<Throwable, Boolean>(), 0);
	}

	private static Throwable copyThrowable(Throwable error,
			IdentityHashMap<Throwable, Boolean> seen, int depth) {
		if (error == null || depth >= MAX_THROWABLE_DEPTH)
			return null;
		if (seen.put(error, Boolean.TRUE) != null)
			return new SanitizedCrashException("cycle", null);
		Throwable safeCause = copyThrowable(error.getCause(), seen, depth + 1);
		SanitizedCrashException safe = new SanitizedCrashException(
				safeType(error), safeCause);
		safe.setStackTrace(copyStack(error.getStackTrace()));
		return safe;
	}

	private static StackTraceElement[] copyStack(StackTraceElement[] original) {
		if (original == null)
			return new StackTraceElement[0];
		int count = Math.min(original.length, MAX_STACK_FRAMES);
		StackTraceElement[] safe = new StackTraceElement[count];
		for (int i = 0; i < count; i++) {
			StackTraceElement frame = original[i];
			safe[i] = new StackTraceElement(
					safeIdentifier(frame.getClassName()),
					safeIdentifier(frame.getMethodName()),
					null,
					frame.getLineNumber());
		}
		return safe;
	}

	private static void appendThrowable(StringBuilder output, Throwable error,
			IdentityHashMap<Throwable, Boolean> seen, int depth, String prefix) {
		if (error == null || depth >= MAX_THROWABLE_DEPTH)
			return;
		if (seen.put(error, Boolean.TRUE) != null) {
			output.append(prefix).append("[exception cycle]");
			return;
		}
		if (output.length() > 0)
			output.append('\n');
		output.append(prefix).append(safeType(error));
		StackTraceElement[] frames = error.getStackTrace();
		int count = Math.min(frames == null ? 0 : frames.length,
				MAX_STACK_FRAMES);
		for (int i = 0; i < count; i++) {
			StackTraceElement frame = frames[i];
			output.append("\n\tat ")
					.append(safeIdentifier(frame.getClassName()))
					.append('.')
					.append(safeIdentifier(frame.getMethodName()))
					.append("(line ")
					.append(frame.getLineNumber())
					.append(')');
		}
		appendThrowable(output, error.getCause(), seen, depth + 1, "caused by ");
	}

	private static String safeType(Throwable error) {
		return safeIdentifier(error.getClass().getName());
	}

	private static String safeIdentifier(String value) {
		if (value == null)
			return "unknown";
		String safe = SAFE_TYPE.matcher(value).replaceAll("_");
		return safe.length() == 0 ? "unknown" : safe;
	}

	private static String replaceControlCharacters(String value) {
		StringBuilder safe = null;
		for (int i = 0; i < value.length(); i++) {
			char c = value.charAt(i);
			if (c < 0x20 || c == 0x7f) {
				if (safe == null)
					safe = new StringBuilder(value);
				safe.setCharAt(i, ' ');
			}
		}
		return safe == null ? value : safe.toString();
	}

	private static final class SanitizedCrashException
			extends RuntimeException {
		SanitizedCrashException(String originalType, Throwable cause) {
			super("Sanitized uncaught exception: "
					+ originalType, cause);
		}

		@Override
		public synchronized Throwable fillInStackTrace() {
			return this;
		}
	}
}
