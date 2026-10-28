package com.redeskyller.bukkit.solaryeconomy.config;

public final class MagnateSettings {

	private final boolean tagEnabled;
	private final boolean broadcastEnabled;

	public MagnateSettings(boolean tagEnabled, boolean broadcastEnabled) {
		this.tagEnabled = tagEnabled;
		this.broadcastEnabled = broadcastEnabled;
	}

	public boolean isTagEnabled() {
		return this.tagEnabled;
	}

	public boolean isBroadcastEnabled() {
		return this.broadcastEnabled;
	}
}
