package com.redeskyller.bukkit.solaryeconomy.storage;

public final class SqliteDialect implements SqlDialect {

	@Override
	public String createTable(String table) {
		return "CREATE TABLE IF NOT EXISTS " + table + " ("
				+ "account_key VARCHAR(40) NOT NULL PRIMARY KEY, "
				+ "display_name VARCHAR(40) NOT NULL, "
				+ "balance TEXT NOT NULL, "
				+ "receiving_disabled INTEGER NOT NULL DEFAULT 0, "
				+ "updated_at INTEGER NOT NULL)";
	}

	@Override
	public String upsert(String table) {
		return "INSERT OR REPLACE INTO " + table
				+ " (account_key, display_name, balance, receiving_disabled, updated_at) VALUES (?, ?, ?, ?, ?)";
	}
}
