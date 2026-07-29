/*
 * CoolReader for Android
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 2
 * of the License, or (at your option) any later version.
 */

package org.coolreader.crengine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Immutable filename-safe transliteration policy.
 */
final class FileNameTranscriber {
	private final List<SubstitutionTable> tables;

	private FileNameTranscriber(List<SubstitutionTable> tables) {
		this.tables = Collections.unmodifiableList(
				new ArrayList<>(tables));
	}

	static FileNameTranscriber legacy() {
		return new FileNameTranscriber(Arrays.asList(
				new SubstitutionTable(0x430, new String[]{
						"a", "b", "v", "g", "d", "e", "zh", "z",
						"i", "j", "k", "l", "m", "n", "o", "p",
						"r", "s", "t", "u", "f", "h", "c", "ch",
						"sh", "sch", "'", "y", "i", "e", "yu", "ya"
				}),
				new SubstitutionTable(0x410, new String[]{
						"A", "B", "V", "G", "D", "E", "Zh", "Z",
						"I", "J", "K", "L", "M", "N", "O", "P",
						"R", "S", "T", "U", "F", "H", "C", "Ch",
						"Sh", "Sch", "'", "Y", "I", "E", "Yu", "Ya"
				})));
	}

	String transcribe(String fileName) {
		if (fileName == null)
			throw new IllegalArgumentException("filename is required");
		StringBuilder result = new StringBuilder(fileName.length());
		for (char character : fileName.toCharArray()) {
			if (isAllowedAscii(character)) {
				result.append(character);
				continue;
			}
			String replacement = replacementFor(character);
			result.append(
					replacement != null ? replacement : "_");
		}
		return result.toString();
	}

	String transcribeWithLimit(String fileName, int maximumLength) {
		if (maximumLength < 0)
			throw new IllegalArgumentException(
					"filename limit must be non-negative");
		String result = transcribe(fileName);
		return result.length() > maximumLength
				? result.substring(0, maximumLength)
				: result;
	}

	private String replacementFor(char character) {
		for (SubstitutionTable table : tables) {
			String replacement = table.replacementFor(character);
			if (replacement != null)
				return replacement;
		}
		return null;
	}

	private static boolean isAllowedAscii(char character) {
		return character >= 'a' && character <= 'z'
				|| character >= 'A' && character <= 'Z'
				|| character >= '0' && character <= '9'
				|| character == '-'
				|| character == '_'
				|| character == '('
				|| character == ')';
	}

	private static final class SubstitutionTable {
		private final int startCharacter;
		private final String[] replacements;

		private SubstitutionTable(
				int startCharacter,
				String[] replacements) {
			this.startCharacter = startCharacter;
			this.replacements = replacements.clone();
		}

		private String replacementFor(char character) {
			int index = character - startCharacter;
			return index >= 0 && index < replacements.length
					? replacements[index]
					: null;
		}
	}
}
