package com.redeskyller.bukkit.solaryeconomy.storage;

/**
 * The few statements that differ between databases. The upsert deliberately avoids
 * {@code ON CONFLICT ... DO UPDATE}: the SQLite bundled with 1.5.2 is 3.7.2, which predates it.
 * The upsert takes (key, display_name, balance, receiving_disabled, updated_at) in this order.
 */
public interface SqlDialect {

	String createTable(String table);

	String upsert(String table);
}
