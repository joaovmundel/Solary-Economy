package com.redeskyller.bukkit.solaryeconomy.config;

/**
 * Read access to the live settings. Components ask for {@link #current()} on every use instead of
 * caching values, so {@code /money reload} reaches all of them without any re-wiring.
 */
public interface SettingsProvider {

	PluginSettings current();
}
