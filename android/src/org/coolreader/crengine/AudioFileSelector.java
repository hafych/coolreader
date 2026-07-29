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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Immutable extension priority used to find a sibling audio file.
 */
final class AudioFileSelector {
	private final List<String> extensions;

	AudioFileSelector(List<String> extensions) {
		if (extensions == null || extensions.isEmpty())
			throw new IllegalArgumentException(
					"audio extension priority must not be empty");
		List<String> copy = new ArrayList<>(extensions.size());
		for (String extension : extensions) {
			if (extension == null || extension.length() == 0)
				throw new IllegalArgumentException(
						"audio extension must not be empty");
			copy.add(extension.toLowerCase(Locale.ROOT));
		}
		this.extensions = Collections.unmodifiableList(copy);
	}

	static AudioFileSelector legacy() {
		return new AudioFileSelector(Arrays.asList(
				"flac", "wav", "m4a", "ogg", "mp3"));
	}

	List<String> extensions() {
		return extensions;
	}

	File findAlternative(File original) {
		if (original == null)
			return null;
		if (original.exists())
			return original;
		String pathWithoutExtension =
				original.toString().replaceAll("\\.\\w+$", "");
		File directory = original.getParentFile();
		if (directory == null || !directory.isDirectory())
			return null;
		File[] siblings = directory.listFiles();
		if (siblings == null)
			return null;

		Map<String, List<File>> filesByExtension = new HashMap<>();
		File firstCandidate = null;
		for (File sibling : siblings) {
			if (!sibling.toString().startsWith(
					pathWithoutExtension + ".")) {
				continue;
			}
			String extension = sibling.toString()
					.toLowerCase(Locale.ROOT)
					.replaceAll(".*\\.", "");
			List<File> matches = filesByExtension.get(extension);
			if (matches == null) {
				matches = new ArrayList<>();
				filesByExtension.put(extension, matches);
			}
			matches.add(sibling);
			if (firstCandidate == null)
				firstCandidate = sibling;
		}
		for (String extension : extensions) {
			List<File> matches = filesByExtension.get(extension);
			if (matches != null)
				return matches.get(0);
		}
		return firstCandidate;
	}
}
