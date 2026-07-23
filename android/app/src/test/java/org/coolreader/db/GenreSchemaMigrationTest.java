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

public class GenreSchemaMigrationTest {
	@Test
	public void migrationPreservesGenresAndNormalizesHierarchy() throws Exception {
		try (Connection connection = openV33Database()) {
			connection.setAutoCommit(false);
			executeMigration(connection);
			connection.commit();

			assertEquals(
					new HashSet<>(Arrays.asList("id", "code")),
					readNames(connection, "PRAGMA table_info(genre)", "name"));
			assertTrue(
					readNames(connection, "PRAGMA index_list(genre)", "name")
							.contains("genre_code_index"));

			try (Statement statement = connection.createStatement();
				 ResultSet rows = statement.executeQuery(
						 "SELECT id, code FROM genre ORDER BY id")) {
				assertTrue(rows.next());
				assertEquals(10, rows.getLong("id"));
				assertEquals("fiction", rows.getString("code"));
				assertTrue(rows.next());
				assertEquals(20, rows.getLong("id"));
				assertEquals("history", rows.getString("code"));
				assertFalse(rows.next());
			}

			try (Statement statement = connection.createStatement();
				 ResultSet rows = statement.executeQuery(
						 "SELECT group_fk, genre_fk FROM genre_hier " +
								 "ORDER BY group_fk, genre_fk")) {
				assertTrue(rows.next());
				assertEquals(1, rows.getLong("group_fk"));
				assertEquals(10, rows.getLong("genre_fk"));
				assertTrue(rows.next());
				assertEquals(2, rows.getLong("group_fk"));
				assertEquals(10, rows.getLong("genre_fk"));
				assertTrue(rows.next());
				assertEquals(2, rows.getLong("group_fk"));
				assertEquals(20, rows.getLong("genre_fk"));
				assertFalse(rows.next());
			}
			assertEquals(1, countRows(connection, "book_genre"));
		}
	}

	@Test
	public void failedMigrationRollsBackToV33Schema() throws Exception {
		try (Connection connection = openV33Database()) {
			connection.setAutoCommit(false);
			try {
				executeMigration(connection);
				try (Statement statement = connection.createStatement()) {
					statement.execute("INSERT INTO missing_table VALUES (1)");
				}
				fail("The deliberate failure must abort the migration");
			} catch (SQLException expected) {
				connection.rollback();
			}

			Set<String> columns =
					readNames(connection, "PRAGMA table_info(genre)", "name");
			assertTrue(columns.contains("parent"));
			assertEquals(3, countRows(connection, "genre"));
			assertFalse(tableExists(connection, "genre_hier"));
		}
	}

	@Test
	public void normalizedSchemaStepCanRunAgain() throws Exception {
		try (Connection connection = openV33Database()) {
			connection.setAutoCommit(false);
			executeMigration(connection);
			connection.commit();

			executeCurrentSchemaStep(connection);
			executeCurrentSchemaStep(connection);

			assertEquals(2, countRows(connection, "genre"));
			assertEquals(3, countRows(connection, "genre_hier"));
			assertFalse(
					readNames(connection, "PRAGMA table_info(genre)", "name")
							.contains("parent"));
		}
	}

	private static Connection openV33Database() throws SQLException {
		Connection connection = DriverManager.getConnection("jdbc:sqlite::memory:");
		try (Statement statement = connection.createStatement()) {
			statement.execute("CREATE TABLE book (id INTEGER NOT NULL PRIMARY KEY)");
			statement.execute("INSERT INTO book (id) VALUES (1)");
		}
		executeAll(connection, GenreSchema.CREATE_V33);
		try (Statement statement = connection.createStatement()) {
			statement.execute(
					"INSERT INTO genre_group (id, code) VALUES " +
							"(1, 'main'), (2, 'secondary')");
			statement.execute(
					"INSERT INTO genre (id, parent, code) VALUES " +
							"(10, 1, 'fiction'), " +
							"(10, 2, 'fiction'), " +
							"(20, 2, 'history')");
			statement.execute(
					"INSERT INTO book_genre (book_fk, genre_fk) VALUES (1, 10)");
		}
		return connection;
	}

	private static void executeMigration(Connection connection) throws SQLException {
		try (Statement statement = connection.createStatement()) {
			statement.execute(GenreSchema.CREATE_HIERARCHY_TABLE);
		}
		executeAll(connection, GenreSchema.NORMALIZE_V34);
		try (Statement statement = connection.createStatement()) {
			statement.execute(GenreSchema.CREATE_NORMALIZED_INDEX);
		}
	}

	private static void executeCurrentSchemaStep(Connection connection)
			throws SQLException {
		try (Statement statement = connection.createStatement()) {
			statement.execute(GenreSchema.CREATE_HIERARCHY_TABLE);
			statement.execute(GenreSchema.CREATE_NORMALIZED_INDEX);
		}
	}

	private static void executeAll(Connection connection, String[] statements)
			throws SQLException {
		try (Statement statement = connection.createStatement()) {
			for (String sql : statements)
				statement.execute(sql);
		}
	}

	private static int countRows(Connection connection, String table)
			throws SQLException {
		try (Statement statement = connection.createStatement();
			 ResultSet rows = statement.executeQuery("SELECT count(*) FROM " + table)) {
			assertTrue(rows.next());
			return rows.getInt(1);
		}
	}

	private static boolean tableExists(Connection connection, String table)
			throws SQLException {
		try (Statement statement = connection.createStatement();
			 ResultSet rows = statement.executeQuery(
					 "SELECT 1 FROM sqlite_master WHERE type='table' " +
							 "AND name='" + table + "'")) {
			return rows.next();
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
