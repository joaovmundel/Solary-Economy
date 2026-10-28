package com.redeskyller.bukkit.solaryeconomy.storage;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import com.redeskyller.bukkit.solaryeconomy.economy.AccountNames;
import com.redeskyller.bukkit.solaryeconomy.economy.AccountSnapshot;

/**
 * JDBC implementation of {@link AccountRepository}. Every value reaches the database as a bound
 * parameter; the only text spliced into SQL is the table name, which is validated up front.
 */
public final class JdbcAccountRepository implements AccountRepository {

	private static final Pattern TABLE_NAME = Pattern.compile("[A-Za-z0-9_]{1,64}");

	private final ConnectionProvider connections;
	private final SqlDialect dialect;
	private final String table;
	private final Logger logger;

	public JdbcAccountRepository(ConnectionProvider connections, SqlDialect dialect, String table, Logger logger) {
		if (!TABLE_NAME.matcher(table).matches())
			throw new IllegalArgumentException("Nome de tabela inválido: " + table);
		this.connections = connections;
		this.dialect = dialect;
		this.table = table;
		this.logger = logger;
	}

	@Override
	public void initialize() throws StorageException {
		try (Statement statement = this.connections.get().createStatement()) {
			statement.execute(this.dialect.createTable(this.table));
		} catch (SQLException exception) {
			throw failure("preparar a tabela '" + this.table + "'", exception);
		}
	}

	@Override
	public long count() throws StorageException {
		try (Statement statement = this.connections.get().createStatement();
				ResultSet result = statement.executeQuery("SELECT COUNT(*) FROM " + this.table)) {
			return result.next() ? result.getLong(1) : 0L;
		} catch (SQLException exception) {
			throw failure("contar as contas", exception);
		}
	}

	@Override
	public List<AccountSnapshot> loadAll() throws StorageException {
		String sql = "SELECT account_key, display_name, balance, receiving_disabled FROM " + this.table;
		List<AccountSnapshot> accounts = new ArrayList<AccountSnapshot>();
		try (Statement statement = this.connections.get().createStatement();
				ResultSet result = statement.executeQuery(sql)) {
			while (result.next()) {
				AccountSnapshot account = readRow(result);
				if (account != null)
					accounts.add(account);
			}
		} catch (SQLException exception) {
			throw failure("carregar as contas", exception);
		}
		return accounts;
	}

	@Override
	public void saveAll(Collection<AccountSnapshot> accounts) throws StorageException {
		if (accounts.isEmpty())
			return;

		long now = System.currentTimeMillis();
		try {
			Connection connection = this.connections.get();
			boolean autoCommit = connection.getAutoCommit();
			connection.setAutoCommit(false);
			try {
				try (PreparedStatement statement = connection.prepareStatement(this.dialect.upsert(this.table))) {
					for (AccountSnapshot account : accounts) {
						statement.setString(1, account.getKey());
						statement.setString(2, account.getDisplayName());
						statement.setString(3, account.getBalance().toPlainString());
						statement.setInt(4, account.isReceivingDisabled() ? 1 : 0);
						statement.setLong(5, now);
						statement.addBatch();
					}
					statement.executeBatch();
				}
				connection.commit();
			} catch (SQLException exception) {
				rollbackQuietly(connection);
				throw exception;
			} finally {
				restoreAutoCommit(connection, autoCommit);
			}
		} catch (SQLException exception) {
			throw failure("gravar " + accounts.size() + " conta(s)", exception);
		}
	}

	@Override
	public void delete(String key) throws StorageException {
		try (PreparedStatement statement = this.connections.get()
				.prepareStatement("DELETE FROM " + this.table + " WHERE account_key = ?")) {
			statement.setString(1, key);
			statement.executeUpdate();
		} catch (SQLException exception) {
			throw failure("remover a conta '" + key + "'", exception);
		}
	}

	@Override
	public void close() {
		this.connections.close();
	}

	/** @return the account, or {@code null} when the row is damaged (it is reported and skipped) */
	private AccountSnapshot readRow(ResultSet result) throws SQLException {
		String key = result.getString(1);
		String displayName = result.getString(2);
		String rawBalance = result.getString(3);
		boolean receivingDisabled = result.getInt(4) != 0;

		if (key == null || !key.equals(AccountNames.keyOf(key))) {
			this.logger.warning("Conta ignorada: chave inválida '" + key + "' na tabela " + this.table + ".");
			return null;
		}
		try {
			BigDecimal balance = new BigDecimal(rawBalance.trim());
			boolean displayMatchesKey = displayName != null && key.equals(AccountNames.keyOf(displayName));
			return new AccountSnapshot(key, displayMatchesKey ? displayName.trim() : key, balance, receivingDisabled);
		} catch (RuntimeException exception) {
			this.logger.warning("Conta '" + key + "' ignorada: saldo ilegível '" + rawBalance + "'.");
			return null;
		}
	}

	private StorageException failure(String action, SQLException cause) {
		return new StorageException("Não foi possível " + action + ": " + cause.getMessage(), cause);
	}

	private void rollbackQuietly(Connection connection) {
		try {
			connection.rollback();
		} catch (SQLException exception) {
			this.logger.log(Level.WARNING, "Falha ao desfazer a transação", exception);
		}
	}

	private void restoreAutoCommit(Connection connection, boolean autoCommit) {
		try {
			connection.setAutoCommit(autoCommit);
		} catch (SQLException exception) {
			this.logger.log(Level.WARNING, "Falha ao restaurar o auto-commit", exception);
		}
	}
}
