package org.coolreader.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

final class JdbcMigrationBackend implements MainDbMigrations.Backend {
	private final Connection connection;

	JdbcMigrationBackend(Connection connection) {
		this.connection = connection;
	}

	@Override
	public void execute(String... statements) {
		try (Statement statement = connection.createStatement()) {
			for (String sql : statements)
				statement.execute(sql);
		} catch (SQLException e) {
			throw failure("SQL migration statement failed", e);
		}
	}

	@Override
	public boolean tableExists(String tableName) {
		return schemaObjectExists("table", tableName);
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
			throw failure("Cannot inspect table " + tableName, e);
		}
	}

	@Override
	public boolean indexExists(String indexName) {
		return schemaObjectExists("index", indexName);
	}

	@Override
	public long queryLong(String query) {
		try (Statement statement = connection.createStatement();
			 ResultSet rows = statement.executeQuery(query)) {
			if (!rows.next())
				throw new MainDbMigrations.MigrationException(
						"Query returned no rows: " + query);
			return rows.getLong(1);
		} catch (SQLException e) {
			throw failure("Migration query failed", e);
		}
	}

	@Override
	public List<MainDbMigrations.BookFormatRow> readBookFormats() {
		ArrayList<MainDbMigrations.BookFormatRow> rows = new ArrayList<>();
		try (Statement statement = connection.createStatement();
			 ResultSet result =
					 statement.executeQuery("SELECT id, pathname, format FROM book")) {
			while (result.next()) {
				rows.add(new MainDbMigrations.BookFormatRow(
						result.getLong("id"),
						result.getString("pathname"),
						result.getLong("format")));
			}
			return rows;
		} catch (SQLException e) {
			throw failure("Cannot read document formats", e);
		}
	}

	@Override
	public void updateBookFormats(Map<Long, Long> updates) {
		try (PreparedStatement statement =
					 connection.prepareStatement(
							 "UPDATE book SET format = ? WHERE id = ?")) {
			for (Map.Entry<Long, Long> update : updates.entrySet()) {
				statement.setLong(1, update.getValue());
				statement.setLong(2, update.getKey());
				statement.addBatch();
			}
			statement.executeBatch();
		} catch (SQLException e) {
			throw failure("Cannot update document formats", e);
		}
	}

	@Override
	public void log(String message) {
		// Tests validate state rather than log output.
	}

	private boolean schemaObjectExists(String type, String name) {
		try (PreparedStatement statement = connection.prepareStatement(
				"SELECT 1 FROM sqlite_master WHERE type=? AND name=?")) {
			statement.setString(1, type);
			statement.setString(2, name);
			try (ResultSet rows = statement.executeQuery()) {
				return rows.next();
			}
		} catch (SQLException e) {
			throw failure("Cannot inspect schema object " + name, e);
		}
	}

	private static MainDbMigrations.MigrationException failure(
			String message, SQLException cause) {
		return new MainDbMigrations.MigrationException(message, cause);
	}
}
