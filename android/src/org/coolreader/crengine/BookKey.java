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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import java.util.Objects;

/**
 * Stable, versioned book identity.
 *
 * The source fields remain available for diagnostics and future migrations.
 * Once a strong content hash is known, relocatable sources no longer depend on
 * their current pathname. Archive entries remain distinct through entryName.
 */
public final class BookKey {
	public enum SourceType {
		FILE,
		CONTENT_URI,
		ARCHIVE_ENTRY,
		TEMPORARY_IMPORT
	}

	private static final String KEY_PREFIX = "bk1:";

	private final String value;
	private final SourceType sourceType;
	private final String sourceLocator;
	private final String entryName;
	private final long size;
	private final String contentHash;

	private BookKey(
			String value, SourceType sourceType, String sourceLocator,
			String entryName, long size, String contentHash) {
		this.value = requireText(value, "value");
		this.sourceType = Objects.requireNonNull(sourceType, "sourceType");
		this.sourceLocator = requireText(sourceLocator, "sourceLocator");
		this.entryName = emptyToNull(entryName);
		this.size = size;
		this.contentHash = normalizeHash(contentHash);
	}

	public static BookKey fromDocumentSource(DocumentSource source) {
		Objects.requireNonNull(source, "source");
		SourceType type;
		String locator;
		String entry = null;
		switch (source.getKind()) {
			case CONTENT_URI:
				type = SourceType.CONTENT_URI;
				locator = source.getLocator();
				break;
			case ARCHIVE_ENTRY:
				type = SourceType.ARCHIVE_ENTRY;
				locator = source.getContainer().getLocator();
				entry = source.getArchiveEntry();
				break;
			case TEMPORARY_IMPORT:
				type = SourceType.TEMPORARY_IMPORT;
				locator = source.getOriginLocator() != null
						? source.getOriginLocator() : source.getLocator();
				break;
			case FILE:
			default:
				type = SourceType.FILE;
				locator = source.getLocator();
				break;
		}
		return create(type, locator, entry, source.getSize(), null);
	}

	public static BookKey fromFileInfo(FileInfo fileInfo) {
		Objects.requireNonNull(fileInfo, "fileInfo");
		if (fileInfo.bookKey != null
				&& fileInfo.sourceType != null
				&& fileInfo.sourceLocator != null) {
			return restore(
					fileInfo.bookKey, fileInfo.sourceType,
					fileInfo.sourceLocator, fileInfo.archiveEntry,
					fileInfo.size, fileInfo.contentHash);
		}

		String identity = fileInfo.getPathName();
		if (DocumentSource.isContentUri(identity)) {
			return create(
					SourceType.CONTENT_URI, identity, null,
					fileInfo.size, fileInfo.contentHash);
		}
		if (fileInfo.arcname != null) {
			return create(
					SourceType.ARCHIVE_ENTRY,
					fileInfo.arcname, fileInfo.pathname,
					fileInfo.size, fileInfo.contentHash);
		}
		return create(
				SourceType.FILE, identity, null,
				fileInfo.size, fileInfo.contentHash);
	}

	public static BookKey restore(
			String value, String sourceType, String sourceLocator,
			String entryName, long size, String contentHash) {
		return new BookKey(
				value, SourceType.valueOf(requireText(
						sourceType, "sourceType").toUpperCase(Locale.ROOT)),
				sourceLocator, entryName, size, contentHash);
	}

	public BookKey withContentHash(String sha256) {
		return create(
				sourceType, sourceLocator, entryName, size,
				normalizeHash(sha256));
	}

	public void applyTo(FileInfo fileInfo) {
		Objects.requireNonNull(fileInfo, "fileInfo");
		fileInfo.bookKey = value;
		fileInfo.sourceType = sourceType.name();
		fileInfo.sourceLocator = sourceLocator;
		fileInfo.archiveEntry = entryName;
		fileInfo.contentHash = contentHash;
	}

	public String getValue() {
		return value;
	}

	public SourceType getSourceType() {
		return sourceType;
	}

	public String getSourceLocator() {
		return sourceLocator;
	}

	public String getEntryName() {
		return entryName;
	}

	public long getSize() {
		return size;
	}

	public String getContentHash() {
		return contentHash;
	}

	private static BookKey create(
			SourceType type, String locator, String entry,
			long size, String contentHash) {
		String normalizedHash = normalizeHash(contentHash);
		String identityLocator = normalizedHash != null ? "" : locator;
		String canonical = field("version", "1")
				+ field("source", type.name())
				+ field("locator", identityLocator)
				+ field("entry", emptyToNull(entry))
				+ field("size", Long.toString(size))
				+ field("sha256", normalizedHash);
		return new BookKey(
				KEY_PREFIX + sha256(canonical), type, locator,
				entry, size, normalizedHash);
	}

	private static String field(String name, String value) {
		String safeValue = value != null ? value : "";
		return name.length() + ":" + name
				+ safeValue.length() + ":" + safeValue;
	}

	private static String sha256(String value) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			return toHex(digest.digest(
					value.getBytes(StandardCharsets.UTF_8)));
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256 is unavailable", e);
		}
	}

	static String toHex(byte[] bytes) {
		StringBuilder result = new StringBuilder(bytes.length * 2);
		for (byte value : bytes)
			result.append(String.format(Locale.ROOT, "%02x", value & 0xff));
		return result.toString();
	}

	private static String normalizeHash(String value) {
		String normalized = emptyToNull(value);
		if (normalized == null)
			return null;
		normalized = normalized.toLowerCase(Locale.ROOT);
		if (!normalized.matches("[0-9a-f]{64}"))
			throw new IllegalArgumentException(
					"contentHash must be a SHA-256 hex value");
		return normalized;
	}

	private static String emptyToNull(String value) {
		return value == null || value.length() == 0 ? null : value;
	}

	private static String requireText(String value, String name) {
		if (value == null || value.length() == 0)
			throw new IllegalArgumentException(name + " must not be empty");
		return value;
	}
}
