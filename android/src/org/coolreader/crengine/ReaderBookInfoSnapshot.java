/*
 * CoolReader for Android
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 2
 * of the License, or (at your option) any later version.
 */

package org.coolreader.crengine;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Immutable metadata captured before asynchronous reader book-info work.
 */
final class ReaderBookInfoSnapshot {
	private final List<String> systemAndFileItems;
	private final List<String> bookItems;

	private ReaderBookInfoSnapshot(
			List<String> systemAndFileItems,
			List<String> bookItems) {
		this.systemAndFileItems = immutableCopy(
				systemAndFileItems);
		this.bookItems = immutableCopy(bookItems);
	}

	static ReaderBookInfoSnapshot capture(
			String version,
			int batteryLevel,
			String formattedTime,
			BookInfo bookInfo) {
		if (bookInfo == null || bookInfo.getFileInfo() == null)
			return null;
		FileInfo fileInfo = bookInfo.getFileInfo();
		return fromValues(
				version,
				batteryLevel,
				formattedTime,
				fileInfo.pathname,
				fileInfo.size,
				fileInfo.arcname,
				fileInfo.arcsize,
				fileInfo.format,
				fileInfo.authors,
				fileInfo.title,
				fileInfo.series,
				fileInfo.seriesNumber,
				fileInfo.language,
				fileInfo.genres);
	}

	static ReaderBookInfoSnapshot fromValues(
			String version,
			int batteryLevel,
			String formattedTime,
			String pathname,
			long size,
			String archiveName,
			long archiveSize,
			DocumentFormat format,
			String authors,
			String title,
			String series,
			int seriesNumber,
			String language,
			String genres) {
		List<String> systemAndFileItems = new ArrayList<>();
		systemAndFileItems.add("section=section.system");
		addValue(
				systemAndFileItems,
				"system.version",
				version != null ? "Cool Reader " + version : null);
		systemAndFileItems.add(
				"system.battery=" + batteryLevel + "%");
		addValue(
				systemAndFileItems,
				"system.time",
				formattedTime);

		systemAndFileItems.add("section=section.file");
		addFilePath(
				systemAndFileItems,
				"file.name",
				"file.path",
				pathname);
		systemAndFileItems.add("file.size=" + size);
		addFilePath(
				systemAndFileItems,
				"file.arcname",
				"file.arcpath",
				archiveName);
		if (archiveName != null && archiveName.length() > 0)
			systemAndFileItems.add(
					"file.arcsize=" + archiveSize);
		if (format != null)
			systemAndFileItems.add(
					"file.format=" + format.name());

		List<String> bookItems = new ArrayList<>();
		bookItems.add("section=section.book");
		addValue(bookItems, "book.authors", authors);
		addValue(bookItems, "book.title", title);
		String seriesLabel = series;
		if (seriesLabel != null && seriesNumber > 0)
			seriesLabel = seriesLabel + " #" + seriesNumber;
		addValue(bookItems, "book.series", seriesLabel);
		addValue(bookItems, "book.language", language);
		if (format == DocumentFormat.FB2)
			addValue(bookItems, "book.genres", genres);
		return new ReaderBookInfoSnapshot(
				systemAndFileItems, bookItems);
	}

	List<String> buildItems(
			Bookmark bookmark,
			PositionProperties position) {
		List<String> items =
				new ArrayList<>(systemAndFileItems);
		if (bookmark != null && position != null) {
			items.add("section=section.position");
			if (position.pageMode != 0) {
				items.add(
						"position.page="
								+ DocumentPositionPolicy.displayPageNumber(
										position.pageNumber,
										position.pageCount)
								+ " / "
								+ position.pageCount);
			}
			items.add(
					"position.percent="
							+ DocumentPositionPolicy.formatPercent(
									position.getPercent()));
			String chapter = bookmark.getTitleText();
			if (chapter != null && chapter.length() > 100)
				chapter = chapter.substring(0, 100) + "...";
			addValue(items, "position.chapter", chapter);
		}
		items.addAll(bookItems);
		return immutableCopy(items);
	}

	private static void addFilePath(
			List<String> items,
			String nameKey,
			String parentKey,
			String path) {
		if (path == null || path.length() == 0)
			return;
		File file = new File(path);
		addValue(items, nameKey, file.getName());
		addValue(items, parentKey, file.getParent());
	}

	private static void addValue(
			List<String> items,
			String key,
			String value) {
		if (value != null && value.length() > 0)
			items.add(key + "=" + value);
	}

	private static List<String> immutableCopy(
			List<String> source) {
		return Collections.unmodifiableList(
				new ArrayList<>(source));
	}
}
