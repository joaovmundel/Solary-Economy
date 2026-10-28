package com.redeskyller.bukkit.solaryeconomy.storage;

import java.io.File;
import java.util.logging.Logger;

import com.redeskyller.bukkit.solaryeconomy.config.DatabaseSettings;

/** The database collaborators chosen by the configuration. */
public final class Storage {

	private final AccountRepository repository;
	private final LegacyAccountSource legacySource;

	private Storage(AccountRepository repository, LegacyAccountSource legacySource) {
		this.repository = repository;
		this.legacySource = legacySource;
	}

	public static Storage create(DatabaseSettings settings, File dataFolder, Logger logger) {
		ConnectionProvider connections;
		SqlDialect dialect;
		if (settings.getType() == DatabaseSettings.Type.MYSQL) {
			connections = new MySqlConnectionProvider(settings, logger);
			dialect = new MySqlDialect();
		} else {
			connections = new SqliteConnectionProvider(new File(dataFolder, "storage.db"), logger);
			dialect = new SqliteDialect();
		}

		return new Storage(new JdbcAccountRepository(connections, dialect, settings.getAccountsTable(), logger),
				new LegacyTableSource(connections, settings.getLegacyTable()));
	}

	public AccountRepository getRepository() {
		return this.repository;
	}

	public LegacyAccountSource getLegacySource() {
		return this.legacySource;
	}
}
