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

import java.util.concurrent.atomic.AtomicReference;

final class ActivityLifecycleState {
	private enum Phase {
		PAUSED,
		RESUMED,
		CLOSED
	}

	private final AtomicReference<Phase> phase =
			new AtomicReference<>(Phase.PAUSED);

	boolean isActive() {
		return phase.get() != Phase.CLOSED;
	}

	boolean isClosed() {
		return !isActive();
	}

	boolean isResumed() {
		return phase.get() == Phase.RESUMED;
	}

	boolean resume() {
		return phase.compareAndSet(
				Phase.PAUSED,
				Phase.RESUMED);
	}

	boolean pause() {
		return phase.compareAndSet(
				Phase.RESUMED,
				Phase.PAUSED);
	}

	boolean close() {
		return phase.getAndSet(Phase.CLOSED)
				!= Phase.CLOSED;
	}
}
