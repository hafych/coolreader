package org.coolreader.db;

import org.junit.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class OpdsCatalogSchemaTest {
	@Test
	public void migrationPreservesCatalogsAndRemovesCredentialColumns() throws Exception {
		try (Connection connection = openLegacyDatabase()) {
			connection.setAutoCommit(false);
			executeAll(connection, OpdsCatalogSchema.REMOVE_CREDENTIAL_COLUMNS);
			connection.commit();

			assertEquals(
					new HashSet<>(Arrays.asList("id", "name", "url", "last_usage")),
					readNames(connection, "PRAGMA table_info(opds_catalog)", "name"));
			assertEquals(
					new HashSet<>(Arrays.asList(
							"opds_catalog_name_index",
							"opds_catalog_url_index",
							"opds_catalog_last_usage_index")),
					readNames(connection, "PRAGMA index_list(opds_catalog)", "name"));

			try (Statement statement = connection.createStatement();
				 ResultSet rows = statement.executeQuery(
						 "SELECT id, name, url, last_usage FROM opds_catalog ORDER BY id")) {
				assertTrue(rows.next());
				assertEquals(7, rows.getLong("id"));
				assertEquals("Private catalog", rows.getString("name"));
				assertEquals("https://catalog.example/private", rows.getString("url"));
				assertEquals(3, rows.getLong("last_usage"));
				assertTrue(rows.next());
				assertEquals(11, rows.getLong("id"));
				assertEquals("Public catalog", rows.getString("name"));
				assertEquals("https://catalog.example/public", rows.getString("url"));
				assertEquals(9, rows.getLong("last_usage"));
				assertFalse(rows.next());
			}

			try (Statement statement = connection.createStatement()) {
				statement.executeQuery("SELECT username, password FROM opds_catalog");
				fail("Legacy credential columns must not be queryable");
			} catch (SQLException expected) {
				assertTrue(expected.getMessage().contains("no such column"));
			}
		}
	}

	@Test
	public void failedMigrationRollsBackWithoutDestroyingLegacyTable() throws Exception {
		try (Connection connection = openLegacyDatabase()) {
			connection.setAutoCommit(false);
			try {
				executeAll(connection, OpdsCatalogSchema.REMOVE_CREDENTIAL_COLUMNS);
				try (Statement statement = connection.createStatement()) {
					statement.execute("INSERT INTO missing_table VALUES (1)");
				}
				fail("The deliberate post-migration failure must abort the transaction");
			} catch (SQLException expected) {
				connection.rollback();
			}

			Set<String> columns =
					readNames(connection, "PRAGMA table_info(opds_catalog)", "name");
			assertTrue(columns.contains("username"));
			assertTrue(columns.contains("password"));
			try (Statement statement = connection.createStatement();
				 ResultSet rows = statement.executeQuery(
						 "SELECT username, password FROM opds_catalog WHERE id=7")) {
				assertTrue(rows.next());
				assertEquals("alice", rows.getString("username"));
				assertEquals("plaintext-secret", rows.getString("password"));
			}
		}
	}

	@Test
	public void currentSchemaNeverCreatesCredentialColumns() throws Exception {
		try (Connection connection = DriverManager.getConnection("jdbc:sqlite::memory:")) {
			try (Statement statement = connection.createStatement()) {
				statement.execute(OpdsCatalogSchema.CREATE_CURRENT_TABLE);
			}
			executeAll(connection, OpdsCatalogSchema.CREATE_INDEXES);

			Set<String> columns =
					readNames(connection, "PRAGMA table_info(opds_catalog)", "name");
			assertFalse(columns.contains("username"));
			assertFalse(columns.contains("password"));
		}
	}

	private static Connection openLegacyDatabase() throws SQLException {
		Connection connection = DriverManager.getConnection("jdbc:sqlite::memory:");
		try (Statement statement = connection.createStatement()) {
			statement.execute(
					"CREATE TABLE opds_catalog (" +
							"id INTEGER PRIMARY KEY AUTOINCREMENT, " +
							"name VARCHAR NOT NULL COLLATE NOCASE, " +
							"url VARCHAR NOT NULL COLLATE NOCASE, " +
							"username VARCHAR DEFAULT NULL, " +
							"password VARCHAR DEFAULT NULL, " +
							"last_usage INTEGER DEFAULT 0)");
			statement.execute(
					"INSERT INTO opds_catalog " +
							"(id, name, url, username, password, last_usage) VALUES " +
							"(7, 'Private catalog', 'https://catalog.example/private', " +
							"'alice', 'plaintext-secret', 3), " +
							"(11, 'Public catalog', 'https://catalog.example/public', " +
							"NULL, NULL, 9)");
		}
		return connection;
	}

	private static void executeAll(Connection connection, String[] statements)
			throws SQLException {
		try (Statement statement = connection.createStatement()) {
			for (String sql : statements)
				statement.execute(sql);
		}
	}

	private static Set<String> readNames(
			Connection connection, String query, String column) throws SQLException {
		Set<String> values = new HashSet<>();
		try (Statement statement = connection.createStatement();
			 ResultSet rows = statement.executeQuery(query)) {
			while (rows.next())
				values.add(rows.getString(column));
		}
		return values;
	}
}
