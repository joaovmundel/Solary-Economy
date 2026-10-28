package com.redeskyller.bukkit.solaryeconomy.storage;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Logger;

import com.redeskyller.bukkit.solaryeconomy.TestSupport;

/** A throwaway SQLite database using the same driver the server bundles, plus helpers to poke at it. */
final class SqliteFixture {

	final File file;
	final ConnectionProvider connections;
	final TestSupport.RecordingLogger logger = TestSupport.logger();

	SqliteFixture(File directory) {
		this.file = new File(directory, "storage.db");
		this.connections = new SqliteConnectionProvider(this.file, this.logger);
	}

	JdbcAccountRepository repository() {
		return new JdbcAccountRepository(this.connections, new SqliteDialect(), "solaryeconomy_accounts", this.logger);
	}

	/** Creates the table exactly as plugin versions 1.5.x did. */
	void createLegacyTable() throws SQLException {
		execute("CREATE TABLE IF NOT EXISTS solaryeconomy (name varchar(40), valor text, toggle int);");
	}

	void insertLegacy(String name, String valor, int toggle) throws SQLException {
		try (PreparedStatement statement = this.connections.get()
				.prepareStatement("INSERT INTO solaryeconomy VALUES (?, ?, ?)")) {
			statement.setString(1, name);
			statement.setString(2, valor);
			statement.setInt(3, toggle);
			statement.executeUpdate();
		}
	}

	void execute(String sql) throws SQLException {
		Connection connection = this.connections.get();
		try (Statement statement = connection.createStatement()) {
			statement.execute(sql);
		}
	}

	long countRows(String table) throws SQLException {
		try (Statement statement = this.connections.get().createStatement();
				java.sql.ResultSet result = statement.executeQuery("SELECT COUNT(*) FROM " + table)) {
			result.next();
			return result.getLong(1);
		}
	}

	Logger log() {
		return this.logger;
	}

	void close() {
		this.connections.close();
	}
}
