package com.redeskyller.bukkit.solaryeconomy.storage;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Logger;

/** SQLite through the driver bundled with CraftBukkit/Spigot (sqlite-jdbc 3.7.2 on 1.5.2). */
public final class SqliteConnectionProvider extends AbstractConnectionProvider {

	private final File file;

	public SqliteConnectionProvider(File file, Logger logger) {
		super(logger);
		this.file = file;
	}

	@Override
	protected Connection open() throws SQLException {
		try {
			Class.forName("org.sqlite.JDBC");
		} catch (ClassNotFoundException exception) {
			throw new SQLException("Driver SQLite (org.sqlite.JDBC) não encontrado. O servidor deveria trazê-lo; "
					+ "use o MySQL ou instale o driver.", exception);
		}

		File parent = this.file.getAbsoluteFile().getParentFile();
		if (parent != null && !parent.exists() && !parent.mkdirs())
			throw new SQLException("Não foi possível criar a pasta " + parent.getPath());

		String path = this.file.getAbsolutePath().replace('\\', '/');
		Connection connection = DriverManager.getConnection("jdbc:sqlite:" + path);
		this.logger.info("Banco de dados SQLite aberto: " + this.file.getName());
		return connection;
	}

	/** A local file cannot drop the connection; only an explicit close makes it unusable. */
	@Override
	protected boolean isUsable(Connection candidate) {
		try {
			return !candidate.isClosed();
		} catch (SQLException exception) {
			return false;
		}
	}
}
