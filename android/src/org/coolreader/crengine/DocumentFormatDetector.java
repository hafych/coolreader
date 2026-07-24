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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Bounded content probe used for untrusted or generic MIME types.
 */
public final class DocumentFormatDetector {
	static final int MAX_PROBE_BYTES = 1024 * 1024;
	private static final int MAX_ZIP_ENTRIES = 256;
	private static final int MAX_MIMETYPE_BYTES = 128;

	private DocumentFormatDetector() {
	}

	public static boolean requiresContentInspection(String mimeType) {
		return DocumentFormat.isGenericMimeType(mimeType)
				|| DocumentFormat.byMimeType(mimeType) == null;
	}

	public static DocumentFormat resolve(
			InputStream inputStream, String displayName, String mimeType)
			throws IOException {
		DocumentFormat declared = DocumentFormat.byMimeType(mimeType);
		if (!requiresContentInspection(mimeType))
			return declared;
		if (inputStream == null)
			return null;
		byte[] sample = readProbe(inputStream);
		return detect(sample, displayName);
	}

	static DocumentFormat detect(byte[] sample, String displayName) {
		if (sample == null || sample.length == 0)
			return null;
		DocumentFormat archiveFormat = detectZip(sample, displayName);
		if (archiveFormat != null)
			return archiveFormat;
		if (startsWith(sample, new byte[] {'{', '\\', 'r', 't', 'f'}))
			return DocumentFormat.RTF;
		if (startsWith(sample, new byte[] {'I', 'T', 'S', 'F'}))
			return DocumentFormat.CHM;
		if (startsWith(sample, new byte[] {
				(byte) 0xd0, (byte) 0xcf, 0x11, (byte) 0xe0,
				(byte) 0xa1, (byte) 0xb1, 0x1a, (byte) 0xe1
		}))
			return DocumentFormat.DOC;
		if (containsAt(sample, 60, "BOOKMOBI")
				|| containsAt(sample, 60, "TEXtREAd"))
			return DocumentFormat.PDB;
		if (startsWith(sample, "!!8-Bit!!".getBytes(StandardCharsets.US_ASCII)))
			return DocumentFormat.TXT;

		if (!looksLikeText(sample))
			return null;
		String text = normalizedTextPrefix(sample);
		if (text.contains("<fictionbook")
				|| text.contains(":fictionbook"))
			return DocumentFormat.FB2;
		if (text.contains("<html")
				|| text.contains("<!doctype html")
				|| text.contains("<xhtml"))
			return DocumentFormat.HTML;

		DocumentFormat extensionFormat = DocumentFormat.byExtension(displayName);
		if (extensionFormat == DocumentFormat.TXT)
			return DocumentFormat.TXT;
		return null;
	}

	private static byte[] readProbe(InputStream inputStream) throws IOException {
		ByteArrayOutputStream output =
				new ByteArrayOutputStream(Math.min(64 * 1024, MAX_PROBE_BYTES));
		byte[] buffer = new byte[16 * 1024];
		int remaining = MAX_PROBE_BYTES;
		while (remaining > 0) {
			int count = inputStream.read(buffer, 0, Math.min(buffer.length, remaining));
			if (count < 0)
				break;
			if (count == 0)
				continue;
			output.write(buffer, 0, count);
			remaining -= count;
		}
		return output.toByteArray();
	}

	private static DocumentFormat detectZip(byte[] sample, String displayName) {
		if (sample.length < 4
				|| sample[0] != 'P' || sample[1] != 'K'
				|| (sample[2] != 3 && sample[2] != 5 && sample[2] != 7)
				|| (sample[3] != 4 && sample[3] != 6 && sample[3] != 8))
			return null;

		boolean epubContainer = false;
		boolean docxContentTypes = false;
		boolean docxDocument = false;
		boolean fb3Description = false;
		boolean fb2Entry = false;
		String packageMime = null;
		try (ZipInputStream zip =
					 new ZipInputStream(new ByteArrayInputStream(sample))) {
			for (int count = 0; count < MAX_ZIP_ENTRIES; count++) {
				ZipEntry entry = zip.getNextEntry();
				if (entry == null)
					break;
				String name = entry.getName();
				if (name == null)
					continue;
				String lowerName = name.toLowerCase(Locale.ROOT);
				if ("mimetype".equals(lowerName)) {
					byte[] value = readZipValue(zip, MAX_MIMETYPE_BYTES);
					packageMime = new String(value, StandardCharsets.US_ASCII).trim();
				} else if ("meta-inf/container.xml".equals(lowerName)) {
					epubContainer = true;
				} else if ("[content_types].xml".equals(lowerName)) {
					docxContentTypes = true;
				} else if ("word/document.xml".equals(lowerName)) {
					docxDocument = true;
				} else if ("fb3/description.xml".equals(lowerName)
						|| lowerName.endsWith("/fb3/description.xml")) {
					fb3Description = true;
				} else if (lowerName.endsWith(".fb2")) {
					fb2Entry = true;
				}
			}
		} catch (IOException ignored) {
			// A bounded prefix can end mid-entry. Markers found before that
			// point remain valid evidence.
		}

		if ("application/epub+zip".equalsIgnoreCase(packageMime)
				|| epubContainer)
			return DocumentFormat.EPUB;
		if ("application/vnd.oasis.opendocument.text".equalsIgnoreCase(packageMime))
			return DocumentFormat.ODT;
		if (docxContentTypes && docxDocument)
			return DocumentFormat.DOCX;
		if (fb3Description)
			return DocumentFormat.FB3;
		if (fb2Entry)
			return DocumentFormat.FB2;

		DocumentFormat extensionFormat = DocumentFormat.byExtension(displayName);
		if (extensionFormat == DocumentFormat.FB2 && fb2Entry)
			return DocumentFormat.FB2;
		return null;
	}

	private static byte[] readZipValue(ZipInputStream input, int maxBytes)
			throws IOException {
		ByteArrayOutputStream output = new ByteArrayOutputStream(maxBytes);
		byte[] buffer = new byte[64];
		int remaining = maxBytes;
		while (remaining > 0) {
			int count = input.read(buffer, 0, Math.min(buffer.length, remaining));
			if (count <= 0)
				break;
			output.write(buffer, 0, count);
			remaining -= count;
		}
		return output.toByteArray();
	}

	private static boolean looksLikeText(byte[] sample) {
		int zeros = 0;
		int controls = 0;
		int considered = Math.min(sample.length, 64 * 1024);
		for (int i = 0; i < considered; i++) {
			int value = sample[i] & 0xff;
			if (value == 0)
				zeros++;
			else if (value < 0x20
					&& value != '\n' && value != '\r'
					&& value != '\t' && value != '\f')
				controls++;
		}
		return zeros == 0 && controls * 50 <= Math.max(1, considered);
	}

	private static String normalizedTextPrefix(byte[] sample) {
		int length = Math.min(sample.length, 64 * 1024);
		return new String(sample, 0, length, StandardCharsets.UTF_8)
				.toLowerCase(Locale.ROOT)
				.replace("\u0000", "");
	}

	private static boolean startsWith(byte[] source, byte[] prefix) {
		if (source.length < prefix.length)
			return false;
		for (int i = 0; i < prefix.length; i++) {
			if (source[i] != prefix[i])
				return false;
		}
		return true;
	}

	private static boolean containsAt(byte[] source, int offset, String value) {
		byte[] expected = value.getBytes(StandardCharsets.US_ASCII);
		if (offset < 0 || source.length < offset + expected.length)
			return false;
		for (int i = 0; i < expected.length; i++) {
			if (source[offset + i] != expected[i])
				return false;
		}
		return true;
	}
}
