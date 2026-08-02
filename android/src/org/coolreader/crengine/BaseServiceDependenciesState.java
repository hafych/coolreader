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

/**
 * Owns the BaseActivity service-dependencies snapshot for one generation.
 *
 * Dependencies are installed once from {@code Services.startServices} and
 * permanently closed by stop/destroy so late work cannot observe a
 * torn-down graph through raw parallel fields.
 */
final class BaseServiceDependenciesState {
	private ServiceDependencies dependencies;
	private boolean closed;

	/**
	 * Installs the snapshot while open and empty. Rejects null, second
	 * install, and any install after close.
	 */
	synchronized boolean install(ServiceDependencies next) {
		if (closed || next == null || dependencies != null)
			return false;
		dependencies = next;
		return true;
	}

	synchronized ServiceDependencies get() {
		return closed ? null : dependencies;
	}

	synchronized boolean isPresent() {
		return !closed && dependencies != null;
	}

	/**
	 * Permanently closes the owner and drops the snapshot reference.
	 */
	synchronized boolean close() {
		if (closed)
			return false;
		closed = true;
		dependencies = null;
		return true;
	}

	synchronized boolean isClosed() {
		return closed;
	}
}
