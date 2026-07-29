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

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

/**
 * Owns temporary scroll-mode requests without changing the saved view mode.
 */
final class ReaderViewModeState {
	static final class Lease {
		private Lease() {
		}
	}

	static final class Transition {
		private final boolean pageMode;

		private Transition(boolean pageMode) {
			this.pageMode = pageMode;
		}

		boolean isPageMode() {
			return pageMode;
		}
	}

	static final class Acquisition {
		private final Lease lease;
		private final Transition transition;

		private Acquisition(Lease lease, Transition transition) {
			this.lease = lease;
			this.transition = transition;
		}

		Lease lease() {
			return lease;
		}

		Transition transition() {
			return transition;
		}
	}

	static final class Snapshot {
		private final boolean pageMode;

		private Snapshot(boolean pageMode) {
			this.pageMode = pageMode;
		}

		boolean isPageMode() {
			return pageMode;
		}
	}

	private final Set<Lease> scrollLeases =
			Collections.newSetFromMap(new IdentityHashMap<>());
	private boolean configuredPageMode = true;
	private boolean effectivePageMode = true;
	private boolean closed;

	synchronized void configure(boolean pageMode) {
		if (closed || configuredPageMode == pageMode)
			return;
		scrollLeases.clear();
		configuredPageMode = pageMode;
		effectivePageMode = pageMode;
	}

	synchronized Acquisition acquireScrollMode() {
		if (closed || !configuredPageMode)
			return null;
		Lease lease = new Lease();
		scrollLeases.add(lease);
		return new Acquisition(lease, moveTo(false));
	}

	synchronized Transition release(Lease lease) {
		if (closed || lease == null || !scrollLeases.remove(lease))
			return null;
		if (!scrollLeases.isEmpty())
			return null;
		return moveTo(configuredPageMode);
	}

	synchronized Transition reset() {
		if (closed)
			return null;
		scrollLeases.clear();
		return moveTo(configuredPageMode);
	}

	synchronized boolean isPageMode() {
		return effectivePageMode;
	}

	synchronized boolean isConfiguredPageMode() {
		return configuredPageMode;
	}

	synchronized Snapshot snapshot() {
		return new Snapshot(effectivePageMode);
	}

	synchronized void close() {
		closed = true;
		scrollLeases.clear();
	}

	private Transition moveTo(boolean pageMode) {
		if (effectivePageMode == pageMode)
			return null;
		effectivePageMode = pageMode;
		return new Transition(pageMode);
	}
}
