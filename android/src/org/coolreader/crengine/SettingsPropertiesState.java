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
 * Owns the Activity settings Properties snapshot for one generation.
 *
 * Publications clone the candidate so later mutations cannot rewrite a
 * published generation. Typed reads never escape the backing map. Destroy
 * permanently closes the owner.
 */
final class SettingsPropertiesState {
	private volatile Properties snapshot = new Properties();
	private boolean closed;

	/**
	 * Publishes a defensive copy of {@code candidate}. Returns the previous
	 * snapshot for notify paths.
	 */
	synchronized Properties replace(Properties candidate) {
		if (closed)
			return snapshot;
		if (candidate == null)
			throw new IllegalArgumentException(
					"settings must not be null");
		Properties previous = snapshot;
		snapshot = new Properties(candidate);
		return previous;
	}

	/**
	 * Returns a defensive copy for external consumers.
	 */
	Properties copy() {
		return new Properties(snapshot);
	}

	/**
	 * Returns the live snapshot. Callers must not mutate it; SettingsManager
	 * uses this only as a short-lived baseline for clone-on-write updates.
	 */
	Properties snapshot() {
		return snapshot;
	}

	String getProperty(String name) {
		return snapshot.getProperty(name);
	}

	String getProperty(String name, String defaultValue) {
		return snapshot.getProperty(name, defaultValue);
	}

	int getInt(String name, int defaultValue) {
		return snapshot.getInt(name, defaultValue);
	}

	boolean getBool(String name, boolean defaultValue) {
		return snapshot.getBool(name, defaultValue);
	}

	synchronized boolean close() {
		if (closed)
			return false;
		closed = true;
		snapshot = new Properties();
		return true;
	}

	synchronized boolean isClosed() {
		return closed;
	}
}
