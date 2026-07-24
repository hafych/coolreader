/*
 * CoolReader for Android
 * Copyright (C) 2026 CoolReader Next contributors
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 2
 * of the License, or (at your option) any later version.
 */

package org.coolreader.crengine;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class StrongDocumentHasher {
	private StrongDocumentHasher() {
	}

	public static String sha256(File file) throws IOException {
		if (file == null || !file.isFile())
			throw new IOException("Document hash input is not a regular file");
		ParseBudget.requireDocumentBytes(file.length());
		try (InputStream input = new FileInputStream(file)) {
			return sha256(input, ParseBudget.MAX_DOCUMENT_INPUT_BYTES);
		}
	}

	public static String sha256(InputStream input, long maxBytes)
			throws IOException {
		if (input == null)
			throw new IOException("Document hash input is missing");
		if (maxBytes < 0)
			throw new IOException("Negative document hash limit");
		MessageDigest digest;
		try {
			digest = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256 is unavailable", e);
		}
		byte[] buffer = new byte[64 * 1024];
		long total = 0;
		int count;
		while ((count = input.read(buffer)) != -1) {
			if (count > maxBytes - total)
				throw new ParseBudget.LimitExceededException(
						ParseBudget.Error.INPUT_BYTES);
			digest.update(buffer, 0, count);
			total += count;
		}
		return BookKey.toHex(digest.digest());
	}
}
