/*
 * CoolReader for Android
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 2
 * of the License, or (at your option) any later version.
 */

package org.coolreader.crengine;

import java.io.IOException;

/**
 * Android ingress side of the native {@code parsebudget.h} contract.
 *
 * Codes and document-size limits are intentionally stable across the
 * Java/native boundary so callers can report a safe reason without exposing
 * file paths or document content.
 */
public final class ParseBudget {
	public static final long MAX_DOCUMENT_INPUT_BYTES = 512L * 1024L * 1024L;

	public enum Error {
		INPUT_BYTES(1001, "input-bytes"),
		TEXT_CHARACTERS(1002, "text-characters"),
		XML_DEPTH(1003, "xml-depth"),
		ARCHIVE_ENTRY_COUNT(1101, "archive-entry-count"),
		ARCHIVE_ENTRY_BYTES(1102, "archive-entry-bytes"),
		ARCHIVE_TOTAL_BYTES(1103, "archive-total-bytes"),
		ARCHIVE_COMPRESSION_RATIO(1104, "archive-compression-ratio"),
		ARCHIVE_PATH_DEPTH(1105, "archive-path-depth"),
		CONTAINER_DEPTH(1106, "container-depth"),
		ARCHIVE_PATH(1107, "archive-path"),
		ARCHIVE_DUPLICATE_ENTRY(1108, "archive-duplicate-entry"),
		IMAGE_DIMENSIONS(1201, "image-dimensions");

		private final int code;
		private final String wireName;

		Error(int code, String wireName) {
			this.code = code;
			this.wireName = wireName;
		}

		public int getCode() {
			return code;
		}

		public String getWireName() {
			return wireName;
		}

		public static Error fromCode(int code) {
			for (Error error : values()) {
				if (error.code == code)
					return error;
			}
			return null;
		}
	}

	public static final class LimitExceededException extends IOException {
		private final Error error;

		public LimitExceededException(Error error) {
			super("Parse budget exceeded: " + error.getWireName());
			this.error = error;
		}

		public Error getError() {
			return error;
		}
	}

	private ParseBudget() {
	}

	public static void requireDocumentBytes(long bytes) throws LimitExceededException {
		if (bytes < 0 || bytes > MAX_DOCUMENT_INPUT_BYTES)
			throw new LimitExceededException(Error.INPUT_BYTES);
	}
}
