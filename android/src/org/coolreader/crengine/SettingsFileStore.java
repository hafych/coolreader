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
import java.io.FileOutputStream;
import java.io.IOException;

final class SettingsFileStore {
	void save(File target, Properties settings) throws IOException {
		if (target == null)
			throw new IllegalArgumentException(
					"target must not be null");
		if (settings == null)
			throw new IllegalArgumentException(
					"settings must not be null");
		try (FileOutputStream output =
					 new FileOutputStream(target)) {
			settings.store(output, "Cool Reader 3 settings");
		}
	}
}
