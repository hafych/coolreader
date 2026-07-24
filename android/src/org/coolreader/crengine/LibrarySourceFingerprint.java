/*
 * CoolReader for Android
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 2
 * of the License, or (at your option) any later version.
 */

package org.coolreader.crengine;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class LibrarySourceFingerprint {
	private static final String DOCUMENT_VERSION = "document-v1";
	private static final String DIRECTORY_VERSION = "directory-v1";

	private LibrarySourceFingerprint() {
	}

	static String forDocument(FileInfo file) {
		if (file == null || file.isDirectory)
			throw new IllegalArgumentException(
					"document must be a file");
		return forDocument(
				file.size, file.arcsize, file.createTime,
				file.isArchive);
	}

	static String forDocument(long size, long archiveSize,
			long modifiedTime, boolean archive) {
		if (modifiedTime <= 0)
			return null;
		MessageDigest digest = newDigest();
		update(digest, DOCUMENT_VERSION);
		update(digest, archive ? "archive" : "file");
		update(digest, Long.toString(size));
		update(digest, Long.toString(archiveSize));
		update(digest, Long.toString(modifiedTime));
		return DOCUMENT_VERSION + ":" + toHex(digest.digest());
	}

	static String forDirectory(FileInfo directory) {
		if (directory == null || !directory.isDirectory)
			throw new IllegalArgumentException(
					"directory must be a directory");
		ArrayList<String> entries = new ArrayList<>(
				directory.itemCount());
		for (int i = 0; i < directory.fileCount(); i++) {
			FileInfo file = directory.getFile(i);
			entries.add(fileEntry(
					file.filename, file.size,
					file.arcsize, file.createTime));
		}
		for (int i = 0; i < directory.dirCount(); i++) {
			FileInfo child = directory.getDir(i);
			entries.add(directoryEntry(
					child.filename, child.createTime));
		}
		return forDirectoryEntries(entries);
	}

	static String fileEntry(
			String name, long size, long archiveSize,
			long modifiedTime) {
		return "F\u0000" + safe(name)
				+ "\u0000" + size
				+ "\u0000" + archiveSize
				+ "\u0000" + modifiedTime;
	}

	static String directoryEntry(
			String name, long modifiedTime) {
		return "D\u0000" + safe(name)
				+ "\u0000" + modifiedTime;
	}

	static String forDirectoryEntries(List<String> sourceEntries) {
		ArrayList<String> entries =
				new ArrayList<>(sourceEntries);
		Collections.sort(entries);
		MessageDigest digest = newDigest();
		update(digest, DIRECTORY_VERSION);
		for (String entry : entries)
			update(digest, entry);
		return DIRECTORY_VERSION + ":" + toHex(digest.digest());
	}

	private static MessageDigest newDigest() {
		try {
			return MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(
					"SHA-256 is unavailable", e);
		}
	}

	private static void update(MessageDigest digest, String value) {
		digest.update(value.getBytes(StandardCharsets.UTF_8));
		digest.update((byte) 0);
	}

	private static String safe(String value) {
		return value != null ? value : "";
	}

	private static String toHex(byte[] bytes) {
		char[] hex = new char[bytes.length * 2];
		final char[] digits = "0123456789abcdef".toCharArray();
		for (int i = 0; i < bytes.length; i++) {
			int value = bytes[i] & 0xff;
			hex[i * 2] = digits[value >>> 4];
			hex[i * 2 + 1] = digits[value & 0x0f];
		}
		return new String(hex);
	}
}
