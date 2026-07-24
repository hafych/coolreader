/*
 * CoolReader for Android
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 2
 * of the License, or (at your option) any later version.
 */

package org.coolreader.crengine;

final class EngineLibraryMetadataExtractor
		implements LibraryMetadataExtractor {
	private final Engine engine;

	EngineLibraryMetadataExtractor(Engine engine) {
		if (engine == null)
			throw new IllegalArgumentException(
					"engine must not be null");
		this.engine = engine;
	}

	@Override
	public boolean extractProperties(FileInfo file) {
		return engine.scanBookProperties(file);
	}

	@Override
	public boolean updateFileFingerprint(FileInfo file) {
		return Engine.updateFileCRC32(file);
	}
}
