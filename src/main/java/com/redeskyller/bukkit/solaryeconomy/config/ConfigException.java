package com.redeskyller.bukkit.solaryeconomy.config;

/** A configuration file is missing, unreadable or not valid YAML. */
public class ConfigException extends Exception {

	private static final long serialVersionUID = 1L;

	public ConfigException(String message) {
		super(message);
	}

	public ConfigException(String message, Throwable cause) {
		super(message, cause);
	}
}
