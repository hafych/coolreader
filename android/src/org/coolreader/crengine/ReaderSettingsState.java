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

final class ReaderSettingsState {
	private volatile Snapshot snapshot;

	ReaderSettingsState(Properties initial) {
		snapshot = new Snapshot(initial);
	}

	synchronized Snapshot replace(Properties replacement) {
		Snapshot published = new Snapshot(replacement);
		snapshot = published;
		return published;
	}

	Snapshot snapshot() {
		return snapshot;
	}

	Properties copy() {
		return snapshot.copy();
	}

	String getProperty(String name) {
		return snapshot.getProperty(name);
	}

	String getProperty(String name, String defaultValue) {
		return snapshot.getProperty(name, defaultValue);
	}

	boolean getBool(String name, boolean defaultValue) {
		return snapshot.getBool(name, defaultValue);
	}

	int getInt(String name, int defaultValue) {
		return snapshot.getInt(name, defaultValue);
	}

	int getColor(String name, int defaultValue) {
		return snapshot.getColor(name, defaultValue);
	}

	static final class Snapshot {
		private final Properties values;

		private Snapshot(Properties values) {
			if (values == null)
				throw new IllegalArgumentException(
						"settings must not be null");
			this.values = new Properties(values);
		}

		Properties copy() {
			return new Properties(values);
		}

		String getProperty(String name) {
			return values.getProperty(name);
		}

		String getProperty(
				String name, String defaultValue) {
			return values.getProperty(
					name, defaultValue);
		}

		boolean getBool(
				String name, boolean defaultValue) {
			return values.getBool(name, defaultValue);
		}

		int getInt(String name, int defaultValue) {
			return values.getInt(name, defaultValue);
		}

		int getColor(String name, int defaultValue) {
			return values.getColor(name, defaultValue);
		}
	}
}
