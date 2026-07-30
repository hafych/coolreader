/*
 * CoolReader for Android
 * Copyright (C) 2026 CoolReader Next contributors
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 2
 * of the License, or (at your option) any later version.
 */

package org.coolreader;

import java.util.concurrent.atomic.AtomicBoolean;

final class ActivityTerminationState {
	private final AtomicBoolean active =
			new AtomicBoolean(true);

	boolean isActive() {
		return active.get();
	}

	boolean isClosed() {
		return !isActive();
	}

	boolean close() {
		return active.compareAndSet(true, false);
	}
}
