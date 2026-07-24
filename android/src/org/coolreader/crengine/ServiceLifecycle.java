/*
 * CoolReader for Android
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 2
 * of the License, or (at your option) any later version.
 */

package org.coolreader.crengine;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Per-generation cancellation token for components using the app service graph.
 */
public final class ServiceLifecycle {
	private final long generation;
	private final AtomicBoolean active = new AtomicBoolean(true);

	ServiceLifecycle(long generation) {
		this.generation = generation;
	}

	public long getGeneration() {
		return generation;
	}

	public boolean isActive() {
		return active.get();
	}

	void close() {
		active.set(false);
	}
}
