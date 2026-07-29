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

final class TtsDocumentSnapshot {
	private final String authors;
	private final String title;
	private final String language;
	private final String path;

	private TtsDocumentSnapshot(
			String authors,
			String title,
			String language,
			String path) {
		this.authors = authors;
		this.title = title;
		this.language = language;
		this.path = path;
	}

	static TtsDocumentSnapshot capture(BookInfo bookInfo) {
		if (bookInfo == null || bookInfo.getFileInfo() == null)
			return null;
		FileInfo fileInfo = bookInfo.getFileInfo();
		return fromValues(
				fileInfo.authors,
				fileInfo.title,
				fileInfo.language,
				fileInfo.getPathName());
	}

	static TtsDocumentSnapshot fromValues(
			String authors,
			String title,
			String language,
			String path) {
		return new TtsDocumentSnapshot(
				authors, title, language, path);
	}

	String getAuthors() {
		return authors;
	}

	String getTitle() {
		return title;
	}

	String getLanguage() {
		return language;
	}

	String getPath() {
		return path;
	}
}
