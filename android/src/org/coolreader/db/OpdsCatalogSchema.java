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

final class OpdsCatalogSchema {
	static final String CREATE_CURRENT_TABLE =
			"CREATE TABLE IF NOT EXISTS opds_catalog (" +
					"id INTEGER PRIMARY KEY AUTOINCREMENT, " +
					"name VARCHAR NOT NULL COLLATE NOCASE, " +
					"url VARCHAR NOT NULL COLLATE NOCASE, " +
					"last_usage INTEGER DEFAULT 0" +
					")";

	static final String[] CREATE_INDEXES = {
			"CREATE INDEX IF NOT EXISTS opds_catalog_name_index ON opds_catalog (name)",
			"CREATE INDEX IF NOT EXISTS opds_catalog_url_index ON opds_catalog (url)",
			"CREATE INDEX IF NOT EXISTS opds_catalog_last_usage_index ON opds_catalog (last_usage)"
	};

	static final String[] REMOVE_CREDENTIAL_COLUMNS = {
			"DROP TABLE IF EXISTS opds_catalog_v36",
			"CREATE TABLE opds_catalog_v36 (" +
					"id INTEGER PRIMARY KEY AUTOINCREMENT, " +
					"name VARCHAR NOT NULL COLLATE NOCASE, " +
					"url VARCHAR NOT NULL COLLATE NOCASE, " +
					"last_usage INTEGER DEFAULT 0" +
					")",
			"INSERT INTO opds_catalog_v36 (id, name, url, last_usage) " +
					"SELECT id, name, url, last_usage FROM opds_catalog",
			"DROP TABLE opds_catalog",
			"ALTER TABLE opds_catalog_v36 RENAME TO opds_catalog",
			"CREATE INDEX opds_catalog_name_index ON opds_catalog (name)",
			"CREATE INDEX opds_catalog_url_index ON opds_catalog (url)",
			"CREATE INDEX opds_catalog_last_usage_index ON opds_catalog (last_usage)"
	};

	private OpdsCatalogSchema() {
	}
}
