package com.redeskyller.bukkit.solaryeconomy.config;

public final class LeaderboardSettings {

	private final int size;
	private final int refreshSeconds;
	private final boolean showPrefix;
	private final int maxNameLength;

	public LeaderboardSettings(int size, int refreshSeconds, boolean showPrefix, int maxNameLength) {
		this.size = size;
		this.refreshSeconds = refreshSeconds;
		this.showPrefix = showPrefix;
		this.maxNameLength = maxNameLength;
	}

	public int getSize() {
		return this.size;
	}

	public int getRefreshSeconds() {
		return this.refreshSeconds;
	}

	public boolean isShowPrefix() {
		return this.showPrefix;
	}

	/** Accounts with longer names (factions, shops, ...) are left out of the ranking. */
	public int getMaxNameLength() {
		return this.maxNameLength;
	}
}
