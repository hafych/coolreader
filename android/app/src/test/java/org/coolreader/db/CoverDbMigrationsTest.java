package org.coolreader.db;

import org.junit.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class CoverDbMigrationsTest {
	@Test
	public void emptyVersionZeroDatabaseCreatesCurrentSchema() throws Exception {
		try (Connection connection =
					 DriverManager.getConnection("jdbc:sqlite::memory:")) {
			JdbcCoverBackend backend = new JdbcCoverBackend(connection);
			connection.setAutoCommit(false);
			CoverDbMigrations.upgrade(backend, 0);
			setVersion(connection, CoverDbMigrations.CURRENT_VERSION);
			connection.commit();

			CoverDbMigrations.verifyCurrentSchema(backend);
			assertEquals(CoverDbMigrations.CURRENT_VERSION, readVersion(connection));
			assertEquals(
					0,
					queryLong(connection, "SELECT count(*) FROM coverpages"));
		}
	}

	@Test
	public void everyLegacyVersionUpgradesAndReruns() throws Exception {
		for (int version = 1; version < CoverDbMigrations.CURRENT_VERSION; version++) {
			try (Connection connection = openFixture(version)) {
				JdbcCoverBackend backend = new JdbcCoverBackend(connection);
				connection.setAutoCommit(false);
				CoverDbMigrations.upgrade(backend, version);
				setVersion(connection, CoverDbMigrations.CURRENT_VERSION);
				connection.commit();

				CoverDbMigrations.verifyCurrentSchema(backend);
				assertEquals(
						CoverDbMigrations.CURRENT_VERSION,
						readVersion(connection));
				assertEquals(
						1,
						queryLong(connection, "SELECT count(*) FROM coverpages"));
				assertFalse(backend.tableExists("coverpage"));

				String fingerprint = fingerprint(connection);
				CoverDbMigrations.upgrade(
						backend, CoverDbMigrations.CURRENT_VERSION);
				assertEquals(fingerprint, fingerprint(connection));
			}
		}
	}

	@Test
	public void everyLegacyVersionRollsBackAtomically() throws Exception {
		for (int version = 1; version < CoverDbMigrations.CURRENT_VERSION; version++) {
			try (Connection connection = openFixture(version)) {
				String before = fingerprint(connection);
				JdbcCoverBackend backend = new JdbcCoverBackend(connection);
				connection.setAutoCommit(false);
				boolean upgradeCompleted = false;
				try {
					CoverDbMigrations.upgrade(backend, version);
					setVersion(connection, CoverDbMigrations.CURRENT_VERSION);
					upgradeCompleted = true;
					backend.execute("INSERT INTO missing_table VALUES (1)");
					fail("The deliberate failure must abort cover fixture v" + version);
				} catch (CoverDbMigrations.MigrationException expected) {
					assertTrue(upgradeCompleted);
					connection.rollback();
				}

				assertEquals(version, readVersion(connection));
				assertEquals(before, fingerprint(connection));
			}
		}
	}

	@Test
	public void futureVersionIsRejectedWithoutChanges() throws Exception {
		try (Connection connection = openFixture(8)) {
			int futureVersion = CoverDbMigrations.CURRENT_VERSION + 1;
			setVersion(connection, futureVersion);
			String before = fingerprint(connection);
			try {
				CoverDbMigrations.upgrade(
						new JdbcCoverBackend(connection), futureVersion);
				fail("A future cover schema must be rejected");
			} catch (CoverDbMigrations.FutureSchemaException expected) {
				assertTrue(expected.getMessage().contains(
						Integer.toString(futureVersion)));
			}
			assertEquals(futureVersion, readVersion(connection));
			assertEquals(before, fingerprint(connection));
		}
	}

	private static Connection openFixture(int version) throws SQLException {
		Connection connection =
				DriverManager.getConnection("jdbc:sqlite::memory:");
		try (Statement statement = connection.createStatement()) {
			statement.execute(
					"CREATE TABLE coverpages (" +
							"book_path VARCHAR NOT NULL PRIMARY KEY," +
							"imagedata BLOB NULL)");
			statement.execute(
					"INSERT INTO coverpages (book_path, imagedata) " +
							"VALUES ('retained-book', x'0102')");
			statement.execute(
					"CREATE TABLE coverpage (" +
							"book_path VARCHAR NOT NULL PRIMARY KEY," +
							"imagedata BLOB NULL)");
			statement.execute(
					"INSERT INTO coverpage (book_path, imagedata) " +
							"VALUES ('obsolete-book', x'03')");
			statement.execute("PRAGMA user_version=" + version);
		}
		return connection;
	}

	private static int readVersion(Connection connection) throws SQLException {
		return (int) queryLong(connection, "PRAGMA user_version");
	}

	private static void setVersion(Connection connection, int version)
			throws SQLException {
		try (Statement statement = connection.createStatement()) {
			statement.execute("PRAGMA user_version=" + version);
		}
	}

	private static long queryLong(Connection connection, String query)
			throws SQLException {
		try (Statement statement = connection.createStatement();
			 ResultSet rows = statement.executeQuery(query)) {
			assertTrue(rows.next());
			return rows.getLong(1);
		}
	}

	private static String fingerprint(Connection connection) throws SQLException {
		StringBuilder result = new StringBuilder();
		try (Statement statement = connection.createStatement();
			 ResultSet rows = statement.executeQuery(
					 "SELECT type, name, tbl_name, coalesce(sql, '') " +
							 "FROM sqlite_master WHERE name NOT LIKE 'sqlite_%' " +
							 "ORDER BY type, name")) {
			while (rows.next()) {
				for (int column = 1; column <= 4; column++)
					result.append(rows.getString(column)).append('\u001f');
				result.append('\n');
			}
		}
		if (new JdbcCoverBackend(connection).tableExists("coverpages")) {
			try (Statement statement = connection.createStatement();
				 ResultSet rows = statement.executeQuery(
						 "SELECT book_path, hex(imagedata) " +
								 "FROM coverpages ORDER BY book_path")) {
				while (rows.next()) {
					result.append(rows.getString(1))
							.append('\u001f')
							.append(rows.getString(2))
							.append('\n');
				}
			}
		}
		result.append("version=").append(readVersion(connection));
		return result.toString();
	}

	private static final class JdbcCoverBackend
			implements CoverDbMigrations.Backend {
		private final Connection connection;

		JdbcCoverBackend(Connection connection) {
			this.connection = connection;
		}

		@Override
		public void execute(String... statements) {
			try (Statement statement = connection.createStatement()) {
				for (String sql : statements)
					statement.execute(sql);
			} catch (SQLException e) {
				throw new CoverDbMigrations.MigrationException(
						"Cover migration statement failed", e);
			}
		}

		@Override
		public boolean tableExists(String tableName) {
			try (PreparedStatement statement = connection.prepareStatement(
					"SELECT 1 FROM sqlite_master WHERE type='table' AND name=?")) {
				statement.setString(1, tableName);
				try (ResultSet rows = statement.executeQuery()) {
					return rows.next();
				}
			} catch (SQLException e) {
				throw new CoverDbMigrations.MigrationException(
						"Cannot inspect cover table " + tableName, e);
			}
		}

		@Override
		public boolean columnExists(String tableName, String columnName) {
			try (Statement statement = connection.createStatement();
				 ResultSet rows =
						 statement.executeQuery("PRAGMA table_info(" + tableName + ")")) {
				while (rows.next()) {
					if (columnName.equalsIgnoreCase(rows.getString("name")))
						return true;
				}
				return false;
			} catch (SQLException e) {
				throw new CoverDbMigrations.MigrationException(
						"Cannot inspect cover table " + tableName, e);
			}
		}

		@Override
		public void log(String message) {
			// Tests validate state rather than log output.
		}
	}
}
