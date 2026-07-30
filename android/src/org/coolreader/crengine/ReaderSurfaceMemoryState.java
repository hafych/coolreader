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

final class ReaderSurfaceMemoryState {
	private long bytes;

	synchronized Change resize(int width, int height) {
		return changeTo(
				BitmapMemoryAccounting.surfaceBytes(
						width, height));
	}

	synchronized Change clear() {
		return changeTo(0);
	}

	private Change changeTo(long replacementBytes) {
		if (bytes == replacementBytes)
			return null;
		Change change =
				new Change(bytes, replacementBytes);
		bytes = replacementBytes;
		return change;
	}

	static final class Change {
		private final long releasedBytes;
		private final long acquiredBytes;

		private Change(
				long releasedBytes,
				long acquiredBytes) {
			this.releasedBytes = releasedBytes;
			this.acquiredBytes = acquiredBytes;
		}

		long releasedBytes() {
			return releasedBytes;
		}

		long acquiredBytes() {
			return acquiredBytes;
		}
	}
}
