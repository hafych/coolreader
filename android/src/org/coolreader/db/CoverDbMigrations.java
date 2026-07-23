/*
 * CoolReader for Android
 * Copyright (C) 2026 CoolReader Next contributors
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 2
 * of the License, or (at your option) any later version.
 */

package org.coolreader.db;

final class CoverDbMigrations {
	static final int CURRENT_VERSION = 9;
	static final String[] CURRENT_SCHEMA = {
			"CREATE TABLE IF NOT EXISTS coverpages (" +
					"book_path VARCHAR NOT NULL PRIMARY KEY," +
					"imagedata BLOB NULL)"
	};

	interface Backend {
		void execute(String... statements);

		boolean tableExists(String tableName);

		boolean columnExists(String tableName, String columnName);

		void log(String message);
	}

	static void upgrade(Backend database, int currentVersion) {
		if (currentVersion > CURRENT_VERSION)
			throw new FutureSchemaException(currentVersion);
		if (currentVersion < CURRENT_VERSION) {
			database.log("Applying cover database migration 9 (current cover schema)");
			database.execute(CURRENT_SCHEMA);
			database.execute("DROP TABLE IF EXISTS coverpage");
		}
		verifyCurrentSchema(database);
	}

	static void verifyCurrentSchema(Backend database) {
		require(
				database.tableExists("coverpages"),
				"Required table is missing: coverpages");
		require(
				database.columnExists("coverpages", "book_path"),
				"Required column is missing: coverpages.book_path");
		require(
				database.columnExists("coverpages", "imagedata"),
				"Required column is missing: coverpages.imagedata");
	}

	static class MigrationException extends RuntimeException {
		MigrationException(String message) {
			super(message);
		}

		MigrationException(String message, Throwable cause) {
			super(message, cause);
		}
	}

	static final class FutureSchemaException extends MigrationException {
		FutureSchemaException(int version) {
			super("Unsupported cover database version " + version +
					" (maximum supported version is " + CURRENT_VERSION + ")");
		}
	}

	private static void require(boolean condition, String message) {
		if (!condition)
			throw new MigrationException(message);
	}

	private CoverDbMigrations() {
	}
}
