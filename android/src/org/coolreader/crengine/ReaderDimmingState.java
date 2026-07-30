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

final class ReaderDimmingState {
	private static final int MIN_ALPHA = 32;
	private static final int MAX_ALPHA = 255;

	private volatile int alpha = MAX_ALPHA;

	int alpha() {
		return alpha;
	}

	synchronized boolean update(int candidate) {
		int normalized =
				Math.max(
						MIN_ALPHA,
						Math.min(MAX_ALPHA, candidate));
		if (alpha == normalized)
			return false;
		alpha = normalized;
		return true;
	}
}
