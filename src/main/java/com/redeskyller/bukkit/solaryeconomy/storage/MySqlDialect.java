package com.redeskyller.bukkit.solaryeconomy.storage;

public final class MySqlDialect implements SqlDialect {

	/**
	 * The key column is utf8_bin on purpose: with the default case/accent-insensitive collation
	 * "jose" and "josé" would be the same primary key and overwrite each other.
	 */
	@Override
	public String createTable(String table) {
		return "CREATE TABLE IF NOT EXISTS " + table + " ("
				+ "account_key VARCHAR(40) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL, "
				+ "display_name VARCHAR(40) CHARACTER SET utf8 NOT NULL, "
				+ "balance DECIMAL(40,8) NOT NULL, "
				+ "receiving_disabled TINYINT(1) NOT NULL DEFAULT 0, "
				+ "updated_at BIGINT NOT NULL, "
				+ "PRIMARY KEY (account_key)"
				+ ") ENGINE=InnoDB DEFAULT CHARSET=utf8";
	}

	@Override
	public String upsert(String table) {
		return "INSERT INTO " + table + " (account_key, display_name, balance, receiving_disabled, updated_at) "
				+ "VALUES (?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE display_name = VALUES(display_name), "
				+ "balance = VALUES(balance), receiving_disabled = VALUES(receiving_disabled), "
				+ "updated_at = VALUES(updated_at)";
	}
}
