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

final class GenreSchema {
	static final String[] CREATE_V33 = {
			"CREATE TABLE IF NOT EXISTS metadata (" +
					"param VARCHAR NOT NULL PRIMARY KEY, " +
					"value VARCHAR NOT NULL)",
			"CREATE TABLE IF NOT EXISTS genre_group (" +
					"id INTEGER NOT NULL PRIMARY KEY, " +
					"code VARCHAR NOT NULL)",
			"CREATE TABLE IF NOT EXISTS genre (" +
					"id INTEGER NOT NULL, " +
					"parent INTEGER NOT NULL REFERENCES genre_group(id), " +
					"code VARCHAR NOT NULL, " +
					"PRIMARY KEY (id, parent))",
			"CREATE TABLE IF NOT EXISTS book_genre (" +
					"book_fk INTEGER NOT NULL REFERENCES book(id), " +
					"genre_fk INTEGER NOT NULL REFERENCES genre(id), " +
					"UNIQUE (book_fk, genre_fk))",
			"CREATE INDEX IF NOT EXISTS " +
					"genre_group_code_index ON genre_group (code)",
			"CREATE INDEX IF NOT EXISTS genre_code_index ON genre (code)",
			"CREATE UNIQUE INDEX IF NOT EXISTS " +
					"book_genre_index ON book_genre (book_fk, genre_fk)"
	};

	static final String CREATE_HIERARCHY_TABLE =
			"CREATE TABLE IF NOT EXISTS genre_hier (" +
					"group_fk INTEGER NOT NULL REFERENCES genre_group(id), " +
					"genre_fk INTEGER NOT NULL REFERENCES genre(id))";

	static final String[] NORMALIZE_V34 = {
			"INSERT INTO genre_hier (group_fk, genre_fk) " +
					"SELECT parent, id FROM genre ORDER BY parent, id",
			"DROP TABLE IF EXISTS genre_new",
			"CREATE TABLE genre_new (" +
					"id INTEGER NOT NULL PRIMARY KEY, " +
					"code VARCHAR NOT NULL UNIQUE)",
			"INSERT INTO genre_new (id, code) " +
					"SELECT id, code FROM genre GROUP BY id",
			"DROP TABLE genre",
			"ALTER TABLE genre_new RENAME TO genre"
	};

	static final String CREATE_NORMALIZED_INDEX =
			"CREATE INDEX IF NOT EXISTS genre_code_index ON genre (code)";

	private GenreSchema() {
	}
}
