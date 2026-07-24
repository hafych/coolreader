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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Validates document sources received from outside the application.
 *
 * Resolver-owned content URIs are probed later through ContentResolver.
 * Local sources are checked here before they reach ReaderView.
 */
public final class ExternalDocumentValidator {
	public DocumentSource validate(
			DocumentSource source,
			String mimeType) {
		if (source == null)
			return null;
		if (source.getKind() == DocumentSource.Kind.CONTENT_URI)
			return source;

		DocumentFormat declaredFormat =
				DocumentFormat.byMimeType(mimeType);
		if (!DocumentFormatDetector.requiresContentInspection(mimeType)) {
			return source.withMetadata(
					source.getDisplayName(),
					mimeType,
					source.getSize(),
					declaredFormat);
		}

		File probeFile = source.getKind()
				== DocumentSource.Kind.ARCHIVE_ENTRY
				? new File(source.getContainer().getLocalPath())
				: new File(source.getLocalPath());
		try (InputStream inputStream =
					new FileInputStream(probeFile)) {
			DocumentFormat detectedFormat =
					DocumentFormatDetector.resolve(
							inputStream,
							source.getDisplayName(),
							mimeType);
			if (detectedFormat == null)
				return null;
			return source.withMetadata(
					source.getDisplayName(),
					mimeType,
					probeFile.length(),
					detectedFormat);
		} catch (IOException e) {
			return null;
		}
	}
}
