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
 * Owns the latest delayed dictionary query.
 */
public final class DictionaryLookupSession {
	private Request current;
	private boolean closed;

	public synchronized Request replace(String query) {
		if (closed)
			return null;
		current = new Request(query);
		return current;
	}

	public synchronized void cancel() {
		current = null;
	}

	public synchronized boolean isActive(Request request) {
		return !closed
				&& request != null
				&& current == request;
	}

	public synchronized boolean complete(Request request) {
		if (!isActive(request))
			return false;
		current = null;
		return true;
	}

	public synchronized boolean close() {
		if (closed)
			return false;
		closed = true;
		current = null;
		return true;
	}

	public synchronized boolean isClosed() {
		return closed;
	}

	public static String normalizeQuery(String text) {
		if (text == null || text.isEmpty())
			return null;
		int start = 0;
		while (start < text.length()) {
			int codePoint = text.codePointAt(start);
			if (Character.isLetterOrDigit(codePoint))
				break;
			start += Character.charCount(codePoint);
		}
		if (start >= text.length())
			return null;

		int scan = text.length();
		int end = scan;
		while (scan > start) {
			int codePoint = text.codePointBefore(scan);
			int codePointStart =
					scan - Character.charCount(codePoint);
			if (Character.isLetterOrDigit(codePoint))
				return text.substring(start, end);
			if (!isCombiningMark(codePoint))
				end = codePointStart;
			scan = codePointStart;
		}
		return null;
	}

	private static boolean isCombiningMark(int codePoint) {
		int type = Character.getType(codePoint);
		return type == Character.NON_SPACING_MARK
				|| type == Character.COMBINING_SPACING_MARK
				|| type == Character.ENCLOSING_MARK;
	}

	public static final class Request {
		private final String query;

		private Request(String query) {
			this.query = query;
		}

		public String getQuery() {
			return query;
		}
	}
}
