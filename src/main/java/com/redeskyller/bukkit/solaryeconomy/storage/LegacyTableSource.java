package com.redeskyller.bukkit.solaryeconomy.storage;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/** Reads the table created by plugin versions before 1.6. Never writes to it. */
public final class LegacyTableSource implements LegacyAccountSource {

	private static final Pattern TABLE_NAME = Pattern.compile("[A-Za-z0-9_]{1,64}");

	private final ConnectionProvider connections;
	private final String table;

	public LegacyTableSource(ConnectionProvider connections, String table) {
		if (!TABLE_NAME.matcher(table).matches())
			throw new IllegalArgumentException("Nome de tabela inválido: " + table);
		this.connections = connections;
		this.table = table;
	}

	@Override
	public boolean exists() throws StorageException {
		Connection connection = connect();
		// a failing probe is the portable way to ask "does this table exist?"
		try (Statement statement = connection.createStatement()) {
			statement.executeQuery("SELECT name, valor, toggle FROM " + this.table + " WHERE 1 = 0").close();
			return true;
		} catch (SQLException exception) {
			return false;
		}
	}

	@Override
	public List<LegacyAccount> readAll() throws StorageException {
		Connection connection = connect();
		List<LegacyAccount> rows = new ArrayList<LegacyAccount>();
		try (Statement statement = connection.createStatement();
				ResultSet result = statement.executeQuery("SELECT name, valor, toggle FROM " + this.table)) {
			while (result.next())
				rows.add(new LegacyAccount(result.getString(1), result.getString(2), result.getInt(3) == 1));
		} catch (SQLException exception) {
			throw new StorageException(
					"Não foi possível ler a tabela antiga '" + this.table + "': " + exception.getMessage(), exception);
		}
		return rows;
	}

	private Connection connect() throws StorageException {
		try {
			return this.connections.get();
		} catch (SQLException exception) {
			throw new StorageException("Não foi possível conectar ao banco de dados: " + exception.getMessage(),
					exception);
		}
	}
}
