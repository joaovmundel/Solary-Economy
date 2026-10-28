package com.redeskyller.bukkit.solaryeconomy.command;

import com.redeskyller.bukkit.solaryeconomy.config.ConfigException;

/** Something that can re-read the configuration files without a restart. */
public interface Reloadable {

	/**
	 * @throws ConfigException when a file cannot be read; in that case nothing is changed
	 */
	void reload() throws ConfigException;
}
