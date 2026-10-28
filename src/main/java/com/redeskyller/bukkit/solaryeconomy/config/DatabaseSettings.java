package com.redeskyller.bukkit.solaryeconomy.config;

/**
 * Where accounts are stored. Only read at startup, so a change needs a server restart
 * ({@link #equals} is used by {@code /money reload} to tell the admin so).
 */
public final class DatabaseSettings {

	public enum Type {
		SQLITE, MYSQL
	}

	/** Table used by versions 1.5.x, the source of the one-time import. */
	public static final String DEFAULT_TABLE = "solaryeconomy";

	private final Type type;
	private final String hostname;
	private final String database;
	private final String username;
	private final String password;
	private final String table;

	public DatabaseSettings(Type type, String hostname, String database, String username, String password,
			String table) {
		this.type = type;
		this.hostname = hostname;
		this.database = database;
		this.username = username;
		this.password = password;
		this.table = table;
	}

	public Type getType() {
		return this.type;
	}

	/** Host with an optional port, e.g. {@code localhost:3306}. */
	public String getHostname() {
		return this.hostname;
	}

	public String getDatabase() {
		return this.database;
	}

	public String getUsername() {
		return this.username;
	}

	public String getPassword() {
		return this.password;
	}

	/** Table of the legacy schema (name, valor, toggle). */
	public String getLegacyTable() {
		return this.table;
	}

	/** Table of the current schema. */
	public String getAccountsTable() {
		return this.table + "_accounts";
	}

	@Override
	public boolean equals(Object other) {
		if (this == other)
			return true;
		if (!(other instanceof DatabaseSettings))
			return false;
		DatabaseSettings that = (DatabaseSettings) other;
		return this.type == that.type && eq(this.hostname, that.hostname) && eq(this.database, that.database)
				&& eq(this.username, that.username) && eq(this.password, that.password) && eq(this.table, that.table);
	}

	@Override
	public int hashCode() {
		return 31 * this.type.hashCode() + (this.table == null ? 0 : this.table.hashCode());
	}

	/** Never includes the password. */
	@Override
	public String toString() {
		if (this.type == Type.SQLITE)
			return "SQLite(" + this.table + ")";
		return "MySQL(" + this.username + "@" + this.hostname + "/" + this.database + ", " + this.table + ")";
	}

	private static boolean eq(String a, String b) {
		return a == null ? b == null : a.equals(b);
	}
}
