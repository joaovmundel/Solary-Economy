package com.redeskyller.bukkit.solaryeconomy.config;

/** Immutable snapshot of config.yml. A reload builds a new one and swaps it in atomically. */
public final class PluginSettings {

	private final GeneralSettings general;
	private final DatabaseSettings database;
	private final MoneySettings money;
	private final CurrencySettings currency;
	private final LeaderboardSettings leaderboard;
	private final MagnateSettings magnate;

	public PluginSettings(GeneralSettings general, DatabaseSettings database, MoneySettings money,
			CurrencySettings currency, LeaderboardSettings leaderboard, MagnateSettings magnate) {
		this.general = general;
		this.database = database;
		this.money = money;
		this.currency = currency;
		this.leaderboard = leaderboard;
		this.magnate = magnate;
	}

	public GeneralSettings getGeneral() {
		return this.general;
	}

	public DatabaseSettings getDatabase() {
		return this.database;
	}

	public MoneySettings getMoney() {
		return this.money;
	}

	public CurrencySettings getCurrency() {
		return this.currency;
	}

	public LeaderboardSettings getLeaderboard() {
		return this.leaderboard;
	}

	public MagnateSettings getMagnate() {
		return this.magnate;
	}
}
