package com.redeskyller.bukkit.solaryeconomy.config;

import java.math.BigDecimal;

/** One entry of the abbreviation dictionary: "k" displayed as "k" and worth 1000. */
public final class Abbreviation {

	private final String key;
	private final String display;
	private final BigDecimal divider;

	public Abbreviation(String key, String display, BigDecimal divider) {
		this.key = key;
		this.display = display;
		this.divider = divider;
	}

	/** Lower-case token players type in commands (the "k" in "32k"). */
	public String getKey() {
		return this.key;
	}

	/** Text shown in messages. */
	public String getDisplay() {
		return this.display;
	}

	public BigDecimal getDivider() {
		return this.divider;
	}
}
