package com.redeskyller.bukkit.solaryeconomy.log;

import java.util.logging.Logger;

import com.redeskyller.bukkit.solaryeconomy.config.SettingsProvider;

/**
 * Diagnostic messages switched by {@code debug} in config.yml. They go out at INFO level with a
 * prefix because Bukkit's console handler hides FINE and below, so a plain {@code logger.fine}
 * would never be visible to the admin who turned debugging on.
 */
public final class DebugLog {

	private final Logger logger;
	private final SettingsProvider settings;

	public DebugLog(Logger logger, SettingsProvider settings) {
		this.logger = logger;
		this.settings = settings;
	}

	public void debug(String message) {
		if (this.settings.current().getGeneral().isDebug())
			this.logger.info("[debug] " + message);
	}
}
