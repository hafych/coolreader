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

final class ReaderDocumentOptions {
	private final boolean textAutoformatEnabled;
	private final boolean documentStylesEnabled;
	private final boolean documentFontsEnabled;
	private final int domVersion;
	private final int blockRenderingFlags;
	private final boolean textFormat;
	private final boolean epubFormat;
	private final boolean formatWithEmbeddedStyles;
	private final String language;

	private ReaderDocumentOptions(
			boolean textAutoformatEnabled,
			boolean documentStylesEnabled,
			boolean documentFontsEnabled,
			int domVersion,
			int blockRenderingFlags,
			boolean textFormat,
			boolean epubFormat,
			boolean formatWithEmbeddedStyles,
			String language) {
		this.textAutoformatEnabled = textAutoformatEnabled;
		this.documentStylesEnabled = documentStylesEnabled;
		this.documentFontsEnabled = documentFontsEnabled;
		this.domVersion = domVersion;
		this.blockRenderingFlags = blockRenderingFlags;
		this.textFormat = textFormat;
		this.epubFormat = epubFormat;
		this.formatWithEmbeddedStyles =
				formatWithEmbeddedStyles;
		this.language = language;
	}

	static ReaderDocumentOptions capture(BookInfo bookInfo) {
		if (bookInfo == null || bookInfo.getFileInfo() == null)
			return null;
		FileInfo fileInfo = bookInfo.getFileInfo();
		return fromValues(
				!fileInfo.getFlag(
						FileInfo.DONT_REFLOW_TXT_FILES_FLAG),
				!fileInfo.getFlag(
						FileInfo.DONT_USE_DOCUMENT_STYLES_FLAG),
				fileInfo.getFlag(
						FileInfo.USE_DOCUMENT_FONTS_FLAG),
				fileInfo.domVersion,
				fileInfo.blockRenderingFlags,
				fileInfo.format,
				fileInfo.language);
	}

	static ReaderDocumentOptions fromValues(
			boolean textAutoformatEnabled,
			boolean documentStylesEnabled,
			boolean documentFontsEnabled,
			int domVersion,
			int blockRenderingFlags,
			DocumentFormat format,
			String language) {
		boolean textFormat =
				format == DocumentFormat.TXT
						|| format == DocumentFormat.HTML
						|| format == DocumentFormat.PDB;
		boolean epubFormat = format == DocumentFormat.EPUB;
		boolean embeddedStyles =
				epubFormat
						|| format == DocumentFormat.HTML
						|| format == DocumentFormat.CHM
						|| format == DocumentFormat.FB2
						|| format == DocumentFormat.FB3;
		return new ReaderDocumentOptions(
				textAutoformatEnabled,
				documentStylesEnabled,
				documentFontsEnabled,
				domVersion,
				blockRenderingFlags,
				textFormat,
				epubFormat,
				embeddedStyles,
				language);
	}

	boolean isTextAutoformatEnabled() {
		return textAutoformatEnabled;
	}

	boolean isDocumentStylesEnabled() {
		return documentStylesEnabled;
	}

	boolean isDocumentFontsEnabled() {
		return documentFontsEnabled;
	}

	int getDomVersion() {
		return domVersion;
	}

	int getBlockRenderingFlags() {
		return blockRenderingFlags;
	}

	boolean isTextFormat() {
		return textFormat;
	}

	boolean isEpubFormat() {
		return epubFormat;
	}

	boolean isFormatWithEmbeddedStyles() {
		return formatWithEmbeddedStyles;
	}

	String getLanguage() {
		return language;
	}
}
