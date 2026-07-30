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

final class ReaderPageAnimationState {
	private final int disabledMode;
	private final int defaultMode;
	private final int minimumMode;
	private final int maximumMode;
	private final int enabledDurationMs;
	private volatile Snapshot snapshot;

	ReaderPageAnimationState(
			int disabledMode,
			int defaultMode,
			int minimumMode,
			int maximumMode,
			int enabledDurationMs) {
		if (minimumMode > maximumMode
				|| disabledMode < minimumMode
				|| disabledMode > maximumMode
				|| defaultMode < minimumMode
				|| defaultMode > maximumMode
				|| enabledDurationMs <= 0)
			throw new IllegalArgumentException(
					"invalid page animation configuration");
		this.disabledMode = disabledMode;
		this.defaultMode = defaultMode;
		this.minimumMode = minimumMode;
		this.maximumMode = maximumMode;
		this.enabledDurationMs = enabledDurationMs;
		snapshot = createSnapshot(defaultMode);
	}

	Snapshot configure(String value) {
		int mode;
		if (value == null) {
			mode = defaultMode;
		} else {
			try {
				mode = Integer.parseInt(value);
			} catch (NumberFormatException e) {
				mode = defaultMode;
			}
		}
		if (mode < minimumMode)
			mode = minimumMode;
		else if (mode > maximumMode)
			mode = maximumMode;
		Snapshot configured = createSnapshot(mode);
		snapshot = configured;
		return configured;
	}

	Snapshot snapshot() {
		return snapshot;
	}

	private Snapshot createSnapshot(int mode) {
		return new Snapshot(
				mode,
				mode == disabledMode
						? 0 : enabledDurationMs);
	}

	static final class Snapshot {
		private final int mode;
		private final int durationMs;

		private Snapshot(int mode, int durationMs) {
			this.mode = mode;
			this.durationMs = durationMs;
		}

		int mode() {
			return mode;
		}

		int durationMs() {
			return durationMs;
		}

		boolean isEnabled() {
			return durationMs > 0;
		}
	}
}
