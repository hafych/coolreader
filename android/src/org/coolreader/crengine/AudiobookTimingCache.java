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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class AudiobookTimingCache {
	List<Entry> read(File source) throws IOException {
		if (source == null)
			throw new IllegalArgumentException(
					"source must not be null");
		List<Entry> entries = new ArrayList<>();
		try (BufferedReader reader =
					 new BufferedReader(new FileReader(source))) {
			String line;
			int lineNumber = 0;
			while ((line = reader.readLine()) != null) {
				lineNumber++;
				try {
					entries.add(parse(line));
				} catch (IllegalArgumentException e) {
					throw new IOException(
							"invalid timing cache line "
									+ lineNumber,
							e);
				}
			}
		}
		return Collections.unmodifiableList(entries);
	}

	void write(File target, List<Entry> entries)
			throws IOException {
		if (target == null)
			throw new IllegalArgumentException(
					"target must not be null");
		if (entries == null)
			throw new IllegalArgumentException(
					"entries must not be null");
		List<Entry> snapshot = new ArrayList<>(entries);
		for (Entry entry : snapshot) {
			if (entry == null)
				throw new IllegalArgumentException(
						"entry must not be null");
		}
		try (FileWriter writer = new FileWriter(target)) {
			for (Entry entry : snapshot) {
				writer.write(entry.startPos());
				writer.write(',');
				writer.write(Double.toString(entry.startTime()));
				writer.write(',');
				writer.write(
						Double.toString(
								entry.startTimeInBook()));
				writer.write(',');
				writer.write(
						Double.toString(
								entry.totalBookDuration()));
				writer.write(',');
				writer.write(
						Boolean.toString(
								entry.isFirstSentenceInAudioFile()));
				writer.write(',');
				writer.write(entry.audioFileName());
				writer.write('\n');
			}
		}
	}

	private static Entry parse(String line) {
		String[] columns = line.split(",", 6);
		if (columns.length != 6)
			throw new IllegalArgumentException(
					"expected six columns");
		String booleanValue = columns[4];
		if (!"true".equalsIgnoreCase(booleanValue)
				&& !"false".equalsIgnoreCase(booleanValue)) {
			throw new IllegalArgumentException(
					"invalid boolean value");
		}
		try {
			return new Entry(
					columns[0],
					Double.parseDouble(columns[1]),
					Double.parseDouble(columns[2]),
					Double.parseDouble(columns[3]),
					Boolean.parseBoolean(booleanValue),
					columns[5]);
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException(
					"invalid numeric value", e);
		}
	}

	static final class Entry {
		private final String startPos;
		private final double startTime;
		private final double startTimeInBook;
		private final double totalBookDuration;
		private final boolean firstSentenceInAudioFile;
		private final String audioFileName;

		Entry(
				String startPos,
				double startTime,
				double startTimeInBook,
				double totalBookDuration,
				boolean firstSentenceInAudioFile,
				String audioFileName) {
			if (startPos == null || startPos.isEmpty())
				throw new IllegalArgumentException(
						"startPos must not be empty");
			requireTime(startTime, "startTime");
			requireTime(startTimeInBook, "startTimeInBook");
			requireTime(
					totalBookDuration, "totalBookDuration");
			if (audioFileName == null
					|| audioFileName.isEmpty()) {
				throw new IllegalArgumentException(
						"audioFileName must not be empty");
			}
			this.startPos = startPos;
			this.startTime = startTime;
			this.startTimeInBook = startTimeInBook;
			this.totalBookDuration = totalBookDuration;
			this.firstSentenceInAudioFile =
					firstSentenceInAudioFile;
			this.audioFileName = audioFileName;
		}

		String startPos() {
			return startPos;
		}

		double startTime() {
			return startTime;
		}

		double startTimeInBook() {
			return startTimeInBook;
		}

		double totalBookDuration() {
			return totalBookDuration;
		}

		boolean isFirstSentenceInAudioFile() {
			return firstSentenceInAudioFile;
		}

		String audioFileName() {
			return audioFileName;
		}

		private static void requireTime(
				double value, String name) {
			if (Double.isNaN(value)
					|| Double.isInfinite(value)
					|| value < 0) {
				throw new IllegalArgumentException(
						name
								+ " must be finite and non-negative");
			}
		}
	}
}
