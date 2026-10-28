package com.redeskyller.bukkit.solaryeconomy.storage;

import java.util.List;
import java.util.logging.Logger;

import com.redeskyller.bukkit.solaryeconomy.config.MoneySettings;
import com.redeskyller.bukkit.solaryeconomy.economy.AccountSnapshot;

/**
 * Startup sequence of the database: create the schema, import the pre-1.6 table when this is the
 * first run after an upgrade, then load every account. Runs on the storage thread.
 */
public final class StorageInitializer {

	private final AccountRepository repository;
	private final LegacyAccountSource legacy;
	private final MoneySettings money;
	private final Logger logger;

	public StorageInitializer(AccountRepository repository, LegacyAccountSource legacy, MoneySettings money,
			Logger logger) {
		this.repository = repository;
		this.legacy = legacy;
		this.money = money;
		this.logger = logger;
	}

	public List<AccountSnapshot> initializeAndLoad() throws StorageException {
		this.repository.initialize();
		if (this.repository.count() == 0)
			importLegacyData();

		List<AccountSnapshot> accounts = this.repository.loadAll();
		this.logger.info(accounts.size() + " conta(s) carregada(s).");
		return accounts;
	}

	/**
	 * The old table is copied, never modified or dropped, so downgrading is still possible. The import
	 * is one transaction; if it fails the new table stays empty and the next start tries again.
	 */
	private void importLegacyData() throws StorageException {
		if (!this.legacy.exists())
			return;

		List<LegacyAccount> rows = this.legacy.readAll();
		if (rows.isEmpty())
			return;

		this.logger.info("Tabela de versões anteriores encontrada com " + rows.size() + " linha(s); importando...");
		List<AccountSnapshot> converted = LegacyMigration.convert(rows, this.money, this.logger);
		this.repository.saveAll(converted);
		this.logger.info("Importação concluída: " + converted.size() + " conta(s). A tabela antiga foi mantida intacta.");
	}
}
