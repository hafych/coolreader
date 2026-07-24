package org.coolreader.db;

import org.coolreader.crengine.DocumentFormat;
import org.junit.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class MainDbMigrationsTest {
	@Test
	public void emptyVersionZeroDatabaseCreatesCurrentSchema() throws Exception {
		try (Connection connection = LegacyMainDbFixtures.openEmpty()) {
			JdbcMigrationBackend backend = new JdbcMigrationBackend(connection);
			connection.setAutoCommit(false);
			MainDbMigrations.upgrade(backend, 0);
			setVersion(connection, MainDbMigrations.CURRENT_VERSION);
			connection.commit();

			MainDbMigrations.verifyCurrentSchema(backend);
			assertEquals(MainDbMigrations.CURRENT_VERSION, readVersion(connection));
			assertEquals(0, queryLong(connection, "SELECT count(*) FROM book"));
			assertEquals(0, queryLong(connection, "SELECT count(*) FROM opds_catalog"));
		}
	}

	@Test
	public void everySupportedLegacyFixtureUpgradesAndReruns() throws Exception {
		for (int version : MainDbMigrations.SUPPORTED_LEGACY_VERSIONS) {
			try (Connection connection = LegacyMainDbFixtures.open(version)) {
				JdbcMigrationBackend backend = new JdbcMigrationBackend(connection);
				connection.setAutoCommit(false);
				MainDbMigrations.upgrade(backend, version);
				setVersion(connection, MainDbMigrations.CURRENT_VERSION);
				connection.commit();

				assertEquals(
						"wrong upgraded version for fixture v" + version,
						MainDbMigrations.CURRENT_VERSION,
						readVersion(connection));
				MainDbMigrations.verifyCurrentSchema(backend);
				assertFixtureData(connection, version);

				String fingerprint = fingerprint(connection);
				MainDbMigrations.upgrade(
						backend, MainDbMigrations.CURRENT_VERSION);
				assertEquals(
						"current migration is not idempotent for fixture v" + version,
						fingerprint,
						fingerprint(connection));
			}
		}
	}

	@Test
	public void everySupportedLegacyFixtureRollsBackAtomically() throws Exception {
		for (int version : MainDbMigrations.SUPPORTED_LEGACY_VERSIONS) {
			try (Connection connection = LegacyMainDbFixtures.open(version)) {
				String before = fingerprint(connection);
				JdbcMigrationBackend backend = new JdbcMigrationBackend(connection);
				connection.setAutoCommit(false);
				boolean upgradeCompleted = false;
				try {
					MainDbMigrations.upgrade(backend, version);
					setVersion(connection, MainDbMigrations.CURRENT_VERSION);
					upgradeCompleted = true;
					backend.execute("INSERT INTO missing_table VALUES (1)");
					fail("The deliberate failure must abort fixture v" + version);
				} catch (MainDbMigrations.MigrationException expected) {
					assertTrue(
							"fixture v" + version +
									" failed before the deliberate rollback point",
							upgradeCompleted);
					connection.rollback();
				}

				assertEquals(version, readVersion(connection));
				assertEquals(
						"rollback changed fixture v" + version,
						before,
						fingerprint(connection));
			}
		}
	}

	@Test
	public void earlyDownstreamV34FixtureRepairsMissingLegacyColumns()
			throws Exception {
		try (Connection connection = LegacyMainDbFixtures.openDamagedV34()) {
			JdbcMigrationBackend backend = new JdbcMigrationBackend(connection);
			assertFalse(backend.columnExists("book", "arcsize"));
			assertFalse(backend.columnExists("bookmark", "time_elapsed"));

			connection.setAutoCommit(false);
			MainDbMigrations.upgrade(backend, 34);
			setVersion(connection, MainDbMigrations.CURRENT_VERSION);
			connection.commit();

			assertTrue(backend.columnExists("book", "arcsize"));
			assertTrue(backend.columnExists("bookmark", "time_elapsed"));
			MainDbMigrations.verifyCurrentSchema(backend);
		}
	}

	@Test
	public void stableIdentityMigrationClassifiesUriAndArchiveRows()
			throws Exception {
		try (Connection connection = LegacyMainDbFixtures.open(36)) {
			try (Statement statement = connection.createStatement()) {
				statement.execute(
						"INSERT INTO book (id, pathname, filename, format) VALUES " +
								"(2, 'content://provider/document/42', " +
								"'uri.epub', 5)");
				statement.execute(
						"INSERT INTO book " +
								"(id, pathname, filename, arcname, format) VALUES " +
								"(3, 'inside/book.fb2', 'book.fb2', " +
								"'/library/books.zip', 1)");
			}
			JdbcMigrationBackend backend = new JdbcMigrationBackend(connection);
			connection.setAutoCommit(false);
			MainDbMigrations.upgrade(backend, 36);
			connection.commit();

			assertEquals(
					"CONTENT_URI|content://provider/document/42|null|legacy:2",
					queryText(
							connection,
							"SELECT source_type || '|' || source_locator || '|' || " +
									"coalesce(archive_entry, 'null') || '|' || book_key " +
									"FROM book WHERE id=2"));
			assertEquals(
					"ARCHIVE_ENTRY|/library/books.zip|inside/book.fb2|legacy:3",
					queryText(
							connection,
							"SELECT source_type || '|' || source_locator || '|' || " +
									"archive_entry || '|' || book_key " +
									"FROM book WHERE id=3"));
			try (Statement statement = connection.createStatement()) {
				statement.execute(
						"INSERT INTO book " +
								"(pathname, filename, book_key, source_type, " +
								"source_locator) VALUES " +
								"('/duplicate.epub', 'duplicate.epub', " +
								"'legacy:1', 'FILE', '/duplicate.epub')");
				fail("book_key_index must reject duplicate stable identities");
			} catch (SQLException expected) {
				assertTrue(expected.getMessage().contains("UNIQUE"));
			}
		}
	}

	@Test
	public void sourceFingerprintMigrationAddsBookAndDirectoryStorage()
			throws Exception {
		try (Connection connection = LegacyMainDbFixtures.open(37)) {
			JdbcMigrationBackend backend =
					new JdbcMigrationBackend(connection);
			assertFalse(backend.columnExists(
					"book", "scan_fingerprint"));
			assertFalse(backend.tableExists("library_directory"));

			connection.setAutoCommit(false);
			MainDbMigrations.upgrade(backend, 37);
			connection.commit();

			assertTrue(backend.columnExists(
					"book", "scan_fingerprint"));
			assertTrue(backend.tableExists("library_directory"));
			MainDbMigrations.verifyCurrentSchema(backend);
		}
	}

	@Test
	public void futureSchemaIsRejectedWithoutChanges() throws Exception {
		try (Connection connection = LegacyMainDbFixtures.open(35)) {
			int futureVersion = MainDbMigrations.CURRENT_VERSION + 1;
			setVersion(connection, futureVersion);
			String before = fingerprint(connection);
			try {
				MainDbMigrations.upgrade(
						new JdbcMigrationBackend(connection), futureVersion);
				fail("A future schema must be rejected");
			} catch (MainDbMigrations.FutureSchemaException expected) {
				assertTrue(expected.getMessage().contains(
						Integer.toString(futureVersion)));
			}
			assertEquals(futureVersion, readVersion(connection));
			assertEquals(before, fingerprint(connection));
		}
	}

	@Test
	public void failedPostconditionLeavesLegacyFixtureUntouched()
			throws Exception {
		try (Connection connection = LegacyMainDbFixtures.open(35)) {
			try (Statement statement = connection.createStatement()) {
				statement.execute("DROP INDEX search_history_index");
			}
			String before = fingerprint(connection);
			connection.setAutoCommit(false);
			try {
				MainDbMigrations.upgrade(
						new JdbcMigrationBackend(connection), 35);
				setVersion(connection, MainDbMigrations.CURRENT_VERSION);
				fail("The missing required index must fail the postcondition");
			} catch (MainDbMigrations.MigrationException expected) {
				connection.rollback();
				assertTrue(expected.getMessage().contains(
						"search_history_index"));
			}
			assertEquals(35, readVersion(connection));
			assertEquals(before, fingerprint(connection));
		}
	}

	private static void assertFixtureData(Connection connection, int sourceVersion)
			throws SQLException {
		assertEquals(1, queryLong(connection, "SELECT count(*) FROM author"));
		assertEquals(1, queryLong(connection, "SELECT count(*) FROM book"));
		assertEquals(1, queryLong(connection, "SELECT count(*) FROM bookmark"));
		assertEquals(
				DocumentFormat.EPUB.ordinal(),
				queryLong(connection, "SELECT format FROM book WHERE id=1"));
		assertEquals(
				0,
				queryLong(connection,
						"SELECT count(*) FROM book WHERE domVersion=20200223"));
		assertEquals(
				1,
				queryLong(connection,
						"SELECT count(*) FROM book WHERE id=1 " +
								"AND book_key='legacy:1' " +
								"AND source_type='FILE' " +
								"AND source_locator='/fixture/book.epub'"));
		assertEquals(
				1,
				queryLong(connection,
						"SELECT count(*) FROM bookmark " +
								"WHERE id=1 AND book_fk=1"));
		if (sourceVersion >= 6) {
			assertEquals(
					1,
					queryLong(connection, "SELECT count(*) FROM opds_catalog"));
			assertEquals(
					7,
					queryLong(connection, "SELECT id FROM opds_catalog"));
		}
		if (sourceVersion >= 33) {
			assertEquals(2, queryLong(connection, "SELECT count(*) FROM genre"));
			assertEquals(
					3,
					queryLong(connection, "SELECT count(*) FROM genre_hier"));
		}
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

	private static String queryText(Connection connection, String query)
			throws SQLException {
		try (Statement statement = connection.createStatement();
			 ResultSet rows = statement.executeQuery(query)) {
			assertTrue(rows.next());
			return rows.getString(1);
		}
	}

	private static String fingerprint(Connection connection) throws SQLException {
		StringBuilder result = new StringBuilder();
		appendQuery(
				connection,
				result,
				"SELECT type, name, tbl_name, coalesce(sql, '') " +
						"FROM sqlite_master WHERE name NOT LIKE 'sqlite_%' " +
						"ORDER BY type, name",
				4);
		appendQuery(
				connection,
				result,
				"SELECT id, name FROM author ORDER BY id",
				2);
		appendQuery(
				connection,
				result,
				"SELECT id, pathname, filename, format FROM book ORDER BY id",
				4);
		if (columnExists(connection, "book", "book_key")) {
			appendQuery(
					connection,
					result,
					"SELECT id, book_key, source_type, source_locator, " +
							"archive_entry, content_hash FROM book ORDER BY id",
					6);
		}
		appendQuery(
				connection,
				result,
				"SELECT id, book_fk, start_pos FROM bookmark ORDER BY id",
				3);
		if (tableExists(connection, "opds_catalog")) {
			appendQuery(
					connection,
					result,
					"SELECT id, name, url FROM opds_catalog ORDER BY id",
					3);
		}
		if (tableExists(connection, "genre")) {
			appendQuery(
					connection,
					result,
					"SELECT id, code FROM genre ORDER BY id, code",
					2);
		}
		result.append("version=").append(readVersion(connection));
		return result.toString();
	}

	private static void appendQuery(
			Connection connection,
			StringBuilder output,
			String query,
			int columnCount) throws SQLException {
		try (Statement statement = connection.createStatement();
			 ResultSet rows = statement.executeQuery(query)) {
			while (rows.next()) {
				for (int column = 1; column <= columnCount; column++) {
					output.append(rows.getString(column)).append('\u001f');
				}
				output.append('\n');
			}
		}
	}

	private static boolean tableExists(Connection connection, String name)
			throws SQLException {
		try (Statement statement = connection.createStatement();
			 ResultSet rows = statement.executeQuery(
					 "SELECT 1 FROM sqlite_master WHERE type='table' " +
							 "AND name='" + name + "'")) {
			return rows.next();
		}
	}

	private static boolean columnExists(
			Connection connection, String tableName, String columnName)
			throws SQLException {
		try (Statement statement = connection.createStatement();
			 ResultSet rows = statement.executeQuery(
					 "PRAGMA table_info('" + tableName + "')")) {
			while (rows.next()) {
				if (columnName.equals(rows.getString("name")))
					return true;
			}
			return false;
		}
	}
}
