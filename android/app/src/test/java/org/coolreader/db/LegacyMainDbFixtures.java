package org.coolreader.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

final class LegacyMainDbFixtures {
	static Connection openEmpty() throws SQLException {
		return DriverManager.getConnection("jdbc:sqlite::memory:");
	}

	static Connection open(int version) throws SQLException {
		return open(version, false);
	}

	static Connection openDamagedV34() throws SQLException {
		return open(34, true);
	}

	private static Connection open(int version, boolean damagedV34)
			throws SQLException {
		Connection connection =
				DriverManager.getConnection("jdbc:sqlite::memory:");
		try {
			createV1Schema(connection, damagedV34);
			applyHistoricalSchema(connection, version, damagedV34);
			insertFixtureData(connection, version);
			setVersion(connection, version);
			return connection;
		} catch (SQLException e) {
			connection.close();
			throw e;
		}
	}

	private static void createV1Schema(
			Connection connection, boolean omitRepairColumns) throws SQLException {
		execute(connection,
				"CREATE TABLE author (" +
						"id INTEGER PRIMARY KEY AUTOINCREMENT," +
						"name VARCHAR NOT NULL COLLATE NOCASE)",
				"CREATE INDEX author_name_index ON author (name)",
				"CREATE TABLE series (" +
						"id INTEGER PRIMARY KEY AUTOINCREMENT," +
						"name VARCHAR NOT NULL COLLATE NOCASE)",
				"CREATE INDEX series_name_index ON series (name)",
				"CREATE TABLE folder (" +
						"id INTEGER PRIMARY KEY AUTOINCREMENT," +
						"name VARCHAR NOT NULL)",
				"CREATE INDEX folder_name_index ON folder (name)",
				"CREATE TABLE book (" +
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
						(omitRepairColumns ? "" : "arcsize INTEGER,") +
						"create_time INTEGER," +
						"last_access_time INTEGER)",
				"CREATE INDEX book_folder_index ON book (folder_fk)",
				"CREATE INDEX book_filename_index ON book (filename)",
				"CREATE UNIQUE INDEX book_pathname_index ON book (pathname)",
				"CREATE INDEX book_title_index ON book (title)",
				"CREATE INDEX " +
						"book_last_access_time_index ON book (last_access_time)",
				"CREATE TABLE book_author (" +
						"book_fk INTEGER NOT NULL REFERENCES book (id)," +
						"author_fk INTEGER NOT NULL REFERENCES author (id)," +
						"PRIMARY KEY (book_fk, author_fk))",
				"CREATE UNIQUE INDEX " +
						"author_book_index ON book_author (author_fk, book_fk)",
				"CREATE TABLE bookmark (" +
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
						"comment_text VARCHAR)",
				"CREATE INDEX bookmark_book_index ON bookmark (book_fk)");
	}

	private static void applyHistoricalSchema(
			Connection connection, int version, boolean damagedV34)
			throws SQLException {
		if (version >= 4)
			execute(connection,
					"ALTER TABLE book ADD COLUMN flags INTEGER DEFAULT 0");
		if (version >= 6)
			createLegacyOpdsSchema(connection);
		if (version >= 13)
			execute(connection,
					"ALTER TABLE book ADD COLUMN language VARCHAR DEFAULT NULL");
		if (version >= 15)
			execute(connection,
					"ALTER TABLE opds_catalog ADD COLUMN " +
							"last_usage INTEGER DEFAULT 0",
					"CREATE INDEX opds_catalog_last_usage_index " +
							"ON opds_catalog (last_usage)");
		if (version >= 16 && !damagedV34)
			execute(connection,
					"ALTER TABLE bookmark ADD COLUMN " +
							"time_elapsed INTEGER DEFAULT 0");
		if (version >= 21)
			execute(connection,
					"CREATE TABLE favorite_folders (" +
							"id INTEGER PRIMARY KEY AUTOINCREMENT," +
							"path VARCHAR NOT NULL," +
							"position INTEGER NOT NULL DEFAULT 0)");
		if (version >= 26)
			execute(connection,
					"CREATE TABLE search_history (" +
							"id INTEGER PRIMARY KEY AUTOINCREMENT," +
							"book_fk INTEGER NOT NULL REFERENCES book (id)," +
							"search_text VARCHAR)",
					"CREATE INDEX search_history_index " +
							"ON search_history (book_fk)");
		if (version >= 28)
			execute(connection,
					"ALTER TABLE book ADD COLUMN crc32 INTEGER DEFAULT NULL",
					"ALTER TABLE book ADD COLUMN domVersion INTEGER DEFAULT 0",
					"ALTER TABLE book ADD COLUMN rendFlags INTEGER DEFAULT 0");
		if (version >= 31)
			execute(connection,
					"ALTER TABLE book ADD COLUMN description TEXT DEFAULT NULL");
		if (version >= 33)
			createV33GenreSchema(connection);
		if (version >= 34)
			createV34GenreSchema(connection);
	}

	private static void createLegacyOpdsSchema(Connection connection)
			throws SQLException {
		execute(connection,
				"CREATE TABLE opds_catalog (" +
						"id INTEGER PRIMARY KEY AUTOINCREMENT," +
						"name VARCHAR NOT NULL COLLATE NOCASE," +
						"url VARCHAR NOT NULL COLLATE NOCASE," +
						"username VARCHAR DEFAULT NULL," +
						"password VARCHAR DEFAULT NULL)",
				"CREATE INDEX opds_catalog_name_index ON opds_catalog (name)",
				"CREATE INDEX opds_catalog_url_index ON opds_catalog (url)");
	}

	private static void createV33GenreSchema(Connection connection)
			throws SQLException {
		execute(connection,
				"CREATE TABLE metadata (" +
						"param VARCHAR NOT NULL PRIMARY KEY," +
						"value VARCHAR NOT NULL)",
				"CREATE TABLE genre_group (" +
						"id INTEGER NOT NULL PRIMARY KEY," +
						"code VARCHAR NOT NULL)",
				"CREATE TABLE genre (" +
						"id INTEGER NOT NULL," +
						"parent INTEGER NOT NULL REFERENCES genre_group(id)," +
						"code VARCHAR NOT NULL," +
						"PRIMARY KEY (id, parent))",
				"CREATE TABLE book_genre (" +
						"book_fk INTEGER NOT NULL REFERENCES book(id)," +
						"genre_fk INTEGER NOT NULL REFERENCES genre(id)," +
						"UNIQUE (book_fk, genre_fk))",
				"CREATE INDEX genre_group_code_index ON genre_group (code)",
				"CREATE INDEX genre_code_index ON genre (code)",
				"CREATE UNIQUE INDEX " +
						"book_genre_index ON book_genre (book_fk, genre_fk)");
	}

	private static void createV34GenreSchema(Connection connection)
			throws SQLException {
		execute(connection,
				"DROP TABLE book_genre",
				"DROP INDEX genre_code_index",
				"DROP TABLE genre",
				"CREATE TABLE genre (" +
						"id INTEGER NOT NULL PRIMARY KEY," +
						"code VARCHAR NOT NULL UNIQUE)",
				"CREATE TABLE genre_hier (" +
						"group_fk INTEGER NOT NULL REFERENCES genre_group(id)," +
						"genre_fk INTEGER NOT NULL REFERENCES genre(id))",
				"CREATE TABLE book_genre (" +
						"book_fk INTEGER NOT NULL REFERENCES book(id)," +
						"genre_fk INTEGER NOT NULL REFERENCES genre(id)," +
						"UNIQUE (book_fk, genre_fk))",
				"CREATE INDEX genre_code_index ON genre (code)",
				"CREATE UNIQUE INDEX " +
						"book_genre_index ON book_genre (book_fk, genre_fk)");
	}

	private static void insertFixtureData(Connection connection, int version)
			throws SQLException {
		execute(connection,
				"INSERT INTO author (id, name) VALUES (1, 'Fixture Author')",
				"INSERT INTO book (id, pathname, filename, format" +
						(version >= 28 ? ", domVersion" : "") +
						") VALUES (1, '/fixture/book.epub', 'book.epub', " +
						(version >= 29 ? "5" : "3") +
						(version >= 28
								? ", " + (version >= 30 ? "20200824" : "20200223")
								: "") +
						")",
				"INSERT INTO bookmark (id, book_fk, start_pos) " +
						"VALUES (1, 1, 'fixture-position')");
		if (version >= 6) {
			execute(connection,
					"INSERT INTO opds_catalog " +
							"(id, name, url, username, password" +
							(version >= 15 ? ", last_usage" : "") +
							") VALUES (7, 'Fixture catalog', " +
							"'https://catalog.example/opds', " +
							(version >= 35 ? "NULL, NULL" : "'alice', 'secret'") +
							(version >= 15 ? ", 9" : "") +
							")");
		}
		if (version >= 33) {
			execute(connection,
					"INSERT INTO genre_group (id, code) VALUES " +
							"(1, 'main'), (2, 'secondary')",
					"INSERT INTO genre (id, " +
							(version >= 34 ? "" : "parent, ") +
							"code) VALUES " +
							(version >= 34
									? "(10, 'fiction'), (20, 'history')"
									: "(10, 1, 'fiction'), (10, 2, 'fiction'), " +
											"(20, 2, 'history')"),
					version >= 34
							? "INSERT INTO genre_hier (group_fk, genre_fk) VALUES " +
									"(1, 10), (2, 10), (2, 20)"
							: "INSERT INTO metadata (param, value) " +
									"VALUES ('fixture', 'v33')");
		}
	}

	private static void setVersion(Connection connection, int version)
			throws SQLException {
		execute(connection, "PRAGMA user_version=" + version);
	}

	private static void execute(Connection connection, String... statements)
			throws SQLException {
		try (Statement statement = connection.createStatement()) {
			for (String sql : statements)
				statement.execute(sql);
		}
	}

	private LegacyMainDbFixtures() {
	}
}
