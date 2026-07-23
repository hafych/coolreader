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

import org.coolreader.crengine.DocumentFormat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class MainDbMigrations {
	static final int CURRENT_VERSION = 36;
	static final int[] SUPPORTED_LEGACY_VERSIONS = {
			1, 4, 6, 13, 15, 16, 21, 26, 28, 29, 30, 31, 33, 34, 35
	};

	private static final String[] ENSURE_BASE_SCHEMA = {
			"CREATE TABLE IF NOT EXISTS author (" +
					"id INTEGER PRIMARY KEY AUTOINCREMENT," +
					"name VARCHAR NOT NULL COLLATE NOCASE)",
			"CREATE INDEX IF NOT EXISTS author_name_index ON author (name)",
			"CREATE TABLE IF NOT EXISTS series (" +
					"id INTEGER PRIMARY KEY AUTOINCREMENT," +
					"name VARCHAR NOT NULL COLLATE NOCASE)",
			"CREATE INDEX IF NOT EXISTS series_name_index ON series (name)",
			"CREATE TABLE IF NOT EXISTS folder (" +
					"id INTEGER PRIMARY KEY AUTOINCREMENT," +
					"name VARCHAR NOT NULL)",
			"CREATE INDEX IF NOT EXISTS folder_name_index ON folder (name)",
			"CREATE TABLE IF NOT EXISTS book (" +
					"id INTEGER PRIMARY KEY AUTOINCREMENT," +
					"pathname VARCHAR NOT NULL," +
					"folder_fk INTEGER REFERENCES folder (id)," +
					"filename VARCHAR NOT NULL," +
					"arcname VARCHAR," +
					"title VARCHAR COLLATE NOCASE," +
					"series_fk INTEGER REFERENCES series (id)," +
					"series_number INTEGER," +
					"format INTEGER," +
					"filesize INTEGER," +
					"arcsize INTEGER," +
					"create_time INTEGER," +
					"last_access_time INTEGER," +
					"flags INTEGER DEFAULT 0," +
					"language VARCHAR DEFAULT NULL," +
					"description TEXT DEFAULT NULL," +
					"crc32 INTEGER DEFAULT NULL," +
					"domVersion INTEGER DEFAULT 0," +
					"rendFlags INTEGER DEFAULT 0)",
			"CREATE INDEX IF NOT EXISTS book_folder_index ON book (folder_fk)",
			"CREATE INDEX IF NOT EXISTS book_filename_index ON book (filename)",
			"CREATE UNIQUE INDEX IF NOT EXISTS book_pathname_index ON book (pathname)",
			"CREATE INDEX IF NOT EXISTS book_title_index ON book (title)",
			"CREATE INDEX IF NOT EXISTS " +
					"book_last_access_time_index ON book (last_access_time)",
			"CREATE TABLE IF NOT EXISTS book_author (" +
					"book_fk INTEGER NOT NULL REFERENCES book (id)," +
					"author_fk INTEGER NOT NULL REFERENCES author (id)," +
					"PRIMARY KEY (book_fk, author_fk))",
			"CREATE UNIQUE INDEX IF NOT EXISTS " +
					"author_book_index ON book_author (author_fk, book_fk)",
			"CREATE TABLE IF NOT EXISTS bookmark (" +
					"id INTEGER PRIMARY KEY AUTOINCREMENT," +
					"book_fk INTEGER NOT NULL REFERENCES book (id)," +
					"type INTEGER NOT NULL DEFAULT 0," +
					"percent INTEGER DEFAULT 0," +
					"shortcut INTEGER DEFAULT 0," +
					"time_stamp INTEGER DEFAULT 0," +
					"start_pos VARCHAR NOT NULL," +
					"end_pos VARCHAR," +
					"title_text VARCHAR," +
					"pos_text VARCHAR," +
					"comment_text VARCHAR," +
					"time_elapsed INTEGER DEFAULT 0)",
			"CREATE INDEX IF NOT EXISTS bookmark_book_index ON bookmark (book_fk)",
			"CREATE TABLE IF NOT EXISTS favorite_folders (" +
					"id INTEGER PRIMARY KEY AUTOINCREMENT," +
					"path VARCHAR NOT NULL," +
					"position INTEGER NOT NULL DEFAULT 0)"
	};

	interface Backend {
		void execute(String... statements);

		boolean tableExists(String tableName);

		boolean columnExists(String tableName, String columnName);

		boolean indexExists(String indexName);

		long queryLong(String query);

		List<BookFormatRow> readBookFormats();

		void updateBookFormats(Map<Long, Long> updates);

		void log(String message);
	}

	static final class BookFormatRow {
		final long id;
		final String pathname;
		final long format;

		BookFormatRow(long id, String pathname, long format) {
			this.id = id;
			this.pathname = pathname;
			this.format = format;
		}
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
			super("Unsupported database version " + version +
					" (maximum supported version is " + CURRENT_VERSION + ")");
		}
	}

	private interface Operation {
		void run();
	}

	static void upgrade(Backend database, int currentVersion) {
		if (currentVersion > CURRENT_VERSION)
			throw new FutureSchemaException(currentVersion);
		if (currentVersion == CURRENT_VERSION) {
			verifyCurrentSchema(database);
			return;
		}

		database.execute(ENSURE_BASE_SCHEMA);
		database.execute(GenreSchema.CREATE_V33);
		database.execute(GenreSchema.CREATE_HIERARCHY_TABLE);
		database.execute(OpdsCatalogSchema.CREATE_CURRENT_TABLE);

		apply(database, currentVersion, 1, "bookmark-shortcut",
				() -> addColumnIfMissing(
						database, "bookmark", "shortcut",
						"ALTER TABLE bookmark ADD COLUMN shortcut INTEGER DEFAULT 0"),
				() -> requireColumn(database, "bookmark", "shortcut"));
		apply(database, currentVersion, 4, "book-flags",
				() -> addColumnIfMissing(
						database, "book", "flags",
						"ALTER TABLE book ADD COLUMN flags INTEGER DEFAULT 0"),
				() -> requireColumn(database, "book", "flags"));
		apply(database, currentVersion, 6, "opds-catalog",
				() -> database.execute(OpdsCatalogSchema.CREATE_CURRENT_TABLE),
				() -> {
					requireTable(database, "opds_catalog");
					requireColumn(database, "opds_catalog", "id");
					requireColumn(database, "opds_catalog", "name");
					requireColumn(database, "opds_catalog", "url");
				});
		apply(database, currentVersion, 13, "book-language",
				() -> addColumnIfMissing(
						database, "book", "language",
						"ALTER TABLE book ADD COLUMN language VARCHAR DEFAULT NULL"),
				() -> requireColumn(database, "book", "language"));
		apply(database, currentVersion, 15, "opds-last-usage",
				() -> addColumnIfMissing(
						database, "opds_catalog", "last_usage",
						"ALTER TABLE opds_catalog ADD COLUMN last_usage INTEGER DEFAULT 0"),
				() -> requireColumn(database, "opds_catalog", "last_usage"));
		database.execute(OpdsCatalogSchema.CREATE_INDEXES);
		apply(database, currentVersion, 16, "bookmark-elapsed-time",
				() -> addColumnIfMissing(
						database, "bookmark", "time_elapsed",
						"ALTER TABLE bookmark ADD COLUMN time_elapsed INTEGER DEFAULT 0"),
				() -> requireColumn(database, "bookmark", "time_elapsed"));
		apply(database, currentVersion, 21, "favorite-folders",
				() -> database.execute(
						"CREATE TABLE IF NOT EXISTS favorite_folders (" +
								"id INTEGER PRIMARY KEY AUTOINCREMENT," +
								"path VARCHAR NOT NULL," +
								"position INTEGER NOT NULL DEFAULT 0)"),
				() -> requireTable(database, "favorite_folders"));
		apply(database, currentVersion, 26, "search-history",
				() -> database.execute(
						"CREATE TABLE IF NOT EXISTS search_history (" +
								"id INTEGER PRIMARY KEY AUTOINCREMENT," +
								"book_fk INTEGER NOT NULL REFERENCES book (id)," +
								"search_text VARCHAR)",
						"CREATE INDEX IF NOT EXISTS " +
								"search_history_index ON search_history (book_fk)"),
				() -> {
					requireTable(database, "search_history");
					requireIndex(database, "search_history_index");
				});
		apply(database, currentVersion, 28, "document-render-fields",
				() -> {
					addColumnIfMissing(
							database, "book", "crc32",
							"ALTER TABLE book ADD COLUMN crc32 INTEGER DEFAULT NULL");
					addColumnIfMissing(
							database, "book", "domVersion",
							"ALTER TABLE book ADD COLUMN domVersion INTEGER DEFAULT 0");
					addColumnIfMissing(
							database, "book", "rendFlags",
							"ALTER TABLE book ADD COLUMN rendFlags INTEGER DEFAULT 0");
				},
				() -> {
					requireColumn(database, "book", "crc32");
					requireColumn(database, "book", "domVersion");
					requireColumn(database, "book", "rendFlags");
				});
		apply(database, currentVersion, 29, "document-format-ordinals",
				() -> migrateBookFormats(database),
				() -> verifyBookFormats(database));
		apply(database, currentVersion, 30, "document-dom-version",
				() -> database.execute(
						"UPDATE book SET domVersion=20200824 " +
								"WHERE domVersion=20200223"),
				() -> require(
						database.queryLong(
								"SELECT count(*) FROM book " +
										"WHERE domVersion=20200223") == 0,
						"Legacy document DOM versions remain after migration"));
		apply(database, currentVersion, 31, "book-description",
				() -> addColumnIfMissing(
						database, "book", "description",
						"ALTER TABLE book ADD COLUMN description TEXT DEFAULT NULL"),
				() -> requireColumn(database, "book", "description"));
		apply(database, currentVersion, 33, "genre-metadata",
				() -> database.execute(GenreSchema.CREATE_V33),
				() -> verifyGenreMetadataSchema(database));
		apply(database, currentVersion, 34, "normalized-genres",
				() -> normalizeGenres(database),
				() -> verifyNormalizedGenreSchema(database));
		apply(database, currentVersion, 35, "compatibility-repair",
				() -> {
					addColumnIfMissing(
							database, "book", "arcsize",
							"ALTER TABLE book ADD COLUMN arcsize INTEGER");
					addColumnIfMissing(
							database, "bookmark", "time_elapsed",
							"ALTER TABLE bookmark ADD COLUMN " +
									"time_elapsed INTEGER DEFAULT 0");
				},
				() -> {
					requireColumn(database, "book", "arcsize");
					requireColumn(database, "bookmark", "time_elapsed");
				});
		apply(database, currentVersion, 36, "remove-opds-credentials",
				() -> removeLegacyOpdsCredentialColumns(database),
				() -> verifyOpdsCatalogSchema(database));

		verifyCurrentSchema(database);
	}

	static void verifyCurrentSchema(Backend database) {
		String[] requiredTables = {
				"author", "series", "folder", "book", "book_author", "bookmark",
				"metadata", "genre_group", "genre", "genre_hier", "book_genre",
				"opds_catalog", "favorite_folders", "search_history"
		};
		for (String tableName : requiredTables)
			requireTable(database, tableName);

		String[] requiredBookColumns = {
				"id", "pathname", "folder_fk", "filename", "arcname", "title",
				"series_fk", "series_number", "format", "filesize", "arcsize",
				"create_time", "last_access_time", "flags", "language",
				"description", "crc32", "domVersion", "rendFlags"
		};
		for (String columnName : requiredBookColumns)
			requireColumn(database, "book", columnName);

		String[] requiredBookmarkColumns = {
				"id", "book_fk", "type", "percent", "shortcut", "time_stamp",
				"start_pos", "end_pos", "title_text", "pos_text", "comment_text",
				"time_elapsed"
		};
		for (String columnName : requiredBookmarkColumns)
			requireColumn(database, "bookmark", columnName);

		requireColumn(database, "genre", "id");
		requireColumn(database, "genre", "code");
		requireNoColumn(database, "genre", "parent");
		verifyOpdsCatalogSchema(database);

		String[] requiredIndexes = {
				"author_name_index", "series_name_index", "folder_name_index",
				"book_folder_index", "book_filename_index", "book_pathname_index",
				"book_title_index", "book_last_access_time_index",
				"author_book_index", "bookmark_book_index",
				"genre_group_code_index", "genre_code_index", "book_genre_index",
				"opds_catalog_name_index", "opds_catalog_url_index",
				"opds_catalog_last_usage_index", "search_history_index"
		};
		for (String indexName : requiredIndexes)
			requireIndex(database, indexName);
	}

	private static void apply(
			Backend database,
			int currentVersion,
			int targetVersion,
			String name,
			Operation migration,
			Operation postcondition) {
		if (currentVersion >= targetVersion)
			return;
		database.log(
				"Applying schema migration " + targetVersion + " (" + name + ")");
		migration.run();
		postcondition.run();
	}

	private static void migrateBookFormats(Backend database) {
		Map<Long, Long> updates = findBookFormatUpdates(database);
		if (!updates.isEmpty())
			database.updateBookFormats(updates);
	}

	private static void verifyBookFormats(Backend database) {
		requireColumn(database, "book", "format");
		require(
				findBookFormatUpdates(database).isEmpty(),
				"Invalid document format ordinals remain");
	}

	private static Map<Long, Long> findBookFormatUpdates(Backend database) {
		Map<Long, Long> updates = new HashMap<>();
		for (BookFormatRow row : database.readBookFormats()) {
			if (row.format <= 1 || row.pathname == null)
				continue;
			DocumentFormat newFormat = DocumentFormat.byExtension(row.pathname);
			if (newFormat != null && row.format != newFormat.ordinal())
				updates.put(row.id, (long) newFormat.ordinal());
		}
		return updates;
	}

	private static void verifyGenreMetadataSchema(Backend database) {
		requireTable(database, "metadata");
		requireTable(database, "genre_group");
		requireTable(database, "genre");
		requireTable(database, "book_genre");
		requireColumn(database, "genre", "id");
		requireColumn(database, "genre", "code");
		requireColumn(database, "genre", "parent");
		requireIndex(database, "genre_group_code_index");
		requireIndex(database, "genre_code_index");
		requireIndex(database, "book_genre_index");
	}

	private static void normalizeGenres(Backend database) {
		database.execute(GenreSchema.CREATE_HIERARCHY_TABLE);
		if (database.columnExists("genre", "parent"))
			database.execute(GenreSchema.NORMALIZE_V34);
		database.execute(GenreSchema.CREATE_NORMALIZED_INDEX);
	}

	private static void verifyNormalizedGenreSchema(Backend database) {
		requireTable(database, "genre_hier");
		requireColumn(database, "genre", "id");
		requireColumn(database, "genre", "code");
		requireNoColumn(database, "genre", "parent");
		requireIndex(database, "genre_code_index");
	}

	private static void removeLegacyOpdsCredentialColumns(Backend database) {
		if (!database.columnExists("opds_catalog", "username")
				&& !database.columnExists("opds_catalog", "password")) {
			verifyOpdsCatalogSchema(database);
			return;
		}

		long sourceCount = database.queryLong("SELECT count(*) FROM opds_catalog");
		database.execute(OpdsCatalogSchema.REMOVE_CREDENTIAL_COLUMNS);
		long migratedCount = database.queryLong("SELECT count(*) FROM opds_catalog");
		require(
				migratedCount == sourceCount,
				"OPDS catalog migration lost records");
	}

	private static void verifyOpdsCatalogSchema(Backend database) {
		requireTable(database, "opds_catalog");
		requireColumn(database, "opds_catalog", "id");
		requireColumn(database, "opds_catalog", "name");
		requireColumn(database, "opds_catalog", "url");
		requireColumn(database, "opds_catalog", "last_usage");
		requireNoColumn(database, "opds_catalog", "username");
		requireNoColumn(database, "opds_catalog", "password");
	}

	private static void addColumnIfMissing(
			Backend database,
			String tableName,
			String columnName,
			String statement) {
		requireTable(database, tableName);
		if (!database.columnExists(tableName, columnName))
			database.execute(statement);
		requireColumn(database, tableName, columnName);
	}

	private static void requireTable(Backend database, String tableName) {
		require(
				database.tableExists(tableName),
				"Required table is missing: " + tableName);
	}

	private static void requireColumn(
			Backend database, String tableName, String columnName) {
		require(
				database.columnExists(tableName, columnName),
				"Required column is missing: " + tableName + "." + columnName);
	}

	private static void requireNoColumn(
			Backend database, String tableName, String columnName) {
		require(
				!database.columnExists(tableName, columnName),
				"Forbidden column still exists: " + tableName + "." + columnName);
	}

	private static void requireIndex(Backend database, String indexName) {
		require(
				database.indexExists(indexName),
				"Required index is missing: " + indexName);
	}

	private static void require(boolean condition, String message) {
		if (!condition)
			throw new MigrationException(message);
	}

	private MainDbMigrations() {
	}
}
