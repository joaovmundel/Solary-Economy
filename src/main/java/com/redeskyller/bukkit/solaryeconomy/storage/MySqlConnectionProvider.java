package com.redeskyller.bukkit.solaryeconomy.storage;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Logger;

import com.redeskyller.bukkit.solaryeconomy.config.DatabaseSettings;

/**
 * MySQL through the Connector/J bundled with CraftBukkit/Spigot. Timeouts are set so that a
 * database that stops answering blocks only the storage thread, and only for a bounded time.
 */
public final class MySqlConnectionProvider extends AbstractConnectionProvider {

	private static final int CONNECT_TIMEOUT_MS = 10000;
	private static final int SOCKET_TIMEOUT_MS = 60000;

	private final DatabaseSettings settings;

	public MySqlConnectionProvider(DatabaseSettings settings, Logger logger) {
		super(logger);
		this.settings = settings;
	}

	@Override
	protected Connection open() throws SQLException {
		try {
			Class.forName("com.mysql.jdbc.Driver");
		} catch (ClassNotFoundException exception) {
			throw new SQLException("Driver MySQL (com.mysql.jdbc.Driver) não encontrado. O servidor deveria "
					+ "trazê-lo; use o SQLite ou instale o driver.", exception);
		}

		String url = "jdbc:mysql://" + this.settings.getHostname() + "/" + this.settings.getDatabase()
				+ "?useUnicode=true&characterEncoding=UTF-8&connectTimeout=" + CONNECT_TIMEOUT_MS + "&socketTimeout="
				+ SOCKET_TIMEOUT_MS;
		Connection connection = DriverManager.getConnection(url, this.settings.getUsername(),
				this.settings.getPassword());
		this.logger.info("Conexão com o MySQL estabelecida (" + this.settings.getHostname() + "/"
				+ this.settings.getDatabase() + ").");
		return connection;
	}

	/** The server may have closed an idle connection (wait_timeout), which only a round trip reveals. */
	@Override
	protected boolean isUsable(Connection candidate) {
		try {
			if (candidate.isClosed())
				return false;
			Statement ping = candidate.createStatement();
			try {
				ping.execute("SELECT 1");
			} finally {
				ping.close();
			}
			return true;
		} catch (SQLException exception) {
			return false;
		}
	}
}
