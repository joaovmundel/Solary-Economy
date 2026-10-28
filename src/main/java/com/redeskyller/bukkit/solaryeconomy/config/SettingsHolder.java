package com.redeskyller.bukkit.solaryeconomy.config;

import java.util.concurrent.atomic.AtomicReference;

public final class SettingsHolder implements SettingsProvider {

	private final AtomicReference<PluginSettings> settings;

	public SettingsHolder(PluginSettings initial) {
		this.settings = new AtomicReference<PluginSettings>(initial);
	}

	@Override
	public PluginSettings current() {
		return this.settings.get();
	}

	public void replace(PluginSettings newSettings) {
		this.settings.set(newSettings);
	}
}
