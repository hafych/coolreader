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
import java.util.Objects;

/**
 * Typed description of where a document came from.
 *
 * FileInfo remains the engine/library metadata object. DocumentSource is the
 * boundary object used while deciding how a document may be opened and whether
 * its location is durable.
 */
public final class DocumentSource {
	public enum Kind {
		FILE,
		CONTENT_URI,
		ARCHIVE_ENTRY,
		TEMPORARY_IMPORT
	}

	private final Kind kind;
	private final String locator;
	private final DocumentSource container;
	private final String archiveEntry;
	private final String originLocator;
	private final boolean persistedReadPermission;
	private final String displayName;
	private final String mimeType;
	private final long size;
	private final DocumentFormat format;

	private DocumentSource(Kind kind, String locator, DocumentSource container,
						   String archiveEntry, String originLocator,
						   boolean persistedReadPermission, String displayName,
						   String mimeType, long size, DocumentFormat format) {
		this.kind = Objects.requireNonNull(kind, "kind");
		this.locator = requireText(locator, "locator");
		this.container = container;
		this.archiveEntry = archiveEntry;
		this.originLocator = originLocator;
		this.persistedReadPermission = persistedReadPermission;
		this.displayName = displayName;
		this.mimeType = mimeType;
		this.size = size;
		this.format = format;
	}

	public static DocumentSource fromLegacyLocation(String location) {
		requireText(location, "location");
		if (isContentUri(location))
			return contentUri(location, false);
		String[] archiveParts = FileInfo.splitArcName(location);
		if (archiveParts[1] != null)
			return archiveEntry(file(archiveParts[1]), archiveParts[0]);
		return file(location);
	}

	public static DocumentSource file(String path) {
		String checkedPath = requireText(path, "path");
		return new DocumentSource(
				Kind.FILE, checkedPath, null, null, null, true,
				new File(checkedPath).getName(), null, -1,
				DocumentFormat.byExtension(checkedPath));
	}

	public static DocumentSource contentUri(String uri,
											boolean persistedReadPermission) {
		if (!isContentUri(uri))
			throw new IllegalArgumentException("Not a content URI");
		return new DocumentSource(
				Kind.CONTENT_URI, requireText(uri, "uri"), null, null, null,
				persistedReadPermission, null, null, -1, null);
	}

	public static DocumentSource archiveEntry(DocumentSource container,
											  String entryName) {
		Objects.requireNonNull(container, "container");
		if (container.kind == Kind.ARCHIVE_ENTRY)
			throw new IllegalArgumentException("Nested archive sources are unsupported");
		String checkedEntry = requireText(entryName, "entryName");
		return new DocumentSource(
				Kind.ARCHIVE_ENTRY, container.getIdentity(), container,
				checkedEntry, null, container.isDurable(), new File(checkedEntry).getName(),
				null, -1, DocumentFormat.byExtension(checkedEntry));
	}

	public static DocumentSource temporaryImport(
			String privatePath, String originUri, String displayName,
			String mimeType, long size, DocumentFormat format) {
		return new DocumentSource(
				Kind.TEMPORARY_IMPORT, requireText(privatePath, "privatePath"),
				null, null, requireText(originUri, "originUri"), true,
				displayName, mimeType, size, format);
	}

	public static DocumentSource fromFileInfo(FileInfo fileInfo) {
		Objects.requireNonNull(fileInfo, "fileInfo");
		String identity = requireText(fileInfo.getPathName(), "fileInfo pathname");
		DocumentSource source = fromLegacyLocation(identity);
		return source.withMetadata(
				fileInfo.filename, null, fileInfo.size, fileInfo.format);
	}

	public static boolean isContentUri(String location) {
		if (location == null)
			return false;
		int separator = location.indexOf(':');
		return separator == "content".length()
				&& "content".regionMatches(
						true, 0, location, 0, "content".length())
				&& location.length() > separator + 2
				&& location.charAt(separator + 1) == '/'
				&& location.charAt(separator + 2) == '/';
	}

	public DocumentSource withMetadata(
			String displayName, String mimeType, long size, DocumentFormat format) {
		return new DocumentSource(
				kind, locator, container, archiveEntry, originLocator,
				persistedReadPermission, displayName, mimeType, size, format);
	}

	public DocumentSource withLocalPath(String localPath) {
		String checkedPath = requireText(localPath, "localPath");
		if (kind == Kind.CONTENT_URI)
			throw new IllegalStateException("Content URI has no local path");
		if (kind == Kind.ARCHIVE_ENTRY) {
			DocumentSource normalized = fromLegacyLocation(checkedPath);
			if (normalized.kind != Kind.ARCHIVE_ENTRY)
				throw new IllegalArgumentException(
						"Normalized archive path has no entry");
			return normalized.withMetadata(
					displayName, mimeType, size, format);
		}
		return new DocumentSource(
				kind, checkedPath, null, null, originLocator,
				persistedReadPermission, displayName, mimeType, size, format);
	}

	public Kind getKind() {
		return kind;
	}

	public String getLocator() {
		return locator;
	}

	public DocumentSource getContainer() {
		return container;
	}

	public String getArchiveEntry() {
		return archiveEntry;
	}

	public String getOriginLocator() {
		return originLocator;
	}

	public boolean hasPersistedReadPermission() {
		return persistedReadPermission;
	}

	public String getDisplayName() {
		return displayName;
	}

	public String getMimeType() {
		return mimeType;
	}

	public long getSize() {
		return size;
	}

	public DocumentFormat getFormat() {
		return format;
	}

	public boolean isDurable() {
		switch (kind) {
			case CONTENT_URI:
				return persistedReadPermission;
			case ARCHIVE_ENTRY:
				return container != null && container.isDurable();
			default:
				return true;
		}
	}

	public String getIdentity() {
		if (kind == Kind.ARCHIVE_ENTRY)
			return container.getIdentity() + FileInfo.ARC_SEPARATOR + archiveEntry;
		return locator;
	}

	public String getLocalPath() {
		switch (kind) {
			case FILE:
			case TEMPORARY_IMPORT:
				return locator;
			case ARCHIVE_ENTRY:
				if (container == null || container.kind != Kind.FILE)
					throw new IllegalStateException(
							"Archive container is not a local file");
				return getIdentity();
			default:
				throw new IllegalStateException("Content URI has no local path");
		}
	}

	private static String requireText(String value, String name) {
		if (value == null || value.trim().length() == 0)
			throw new IllegalArgumentException(name + " must not be empty");
		return value;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other)
			return true;
		if (!(other instanceof DocumentSource))
			return false;
		DocumentSource source = (DocumentSource) other;
		return persistedReadPermission == source.persistedReadPermission
				&& size == source.size
				&& kind == source.kind
				&& Objects.equals(locator, source.locator)
				&& Objects.equals(container, source.container)
				&& Objects.equals(archiveEntry, source.archiveEntry)
				&& Objects.equals(originLocator, source.originLocator)
				&& Objects.equals(displayName, source.displayName)
				&& Objects.equals(mimeType, source.mimeType)
				&& format == source.format;
	}

	@Override
	public int hashCode() {
		return Objects.hash(
				kind, locator, container, archiveEntry, originLocator,
				persistedReadPermission, displayName, mimeType, size, format);
	}
}
