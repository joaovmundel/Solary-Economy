package com.redeskyller.bukkit.solaryeconomy.storage;

/** A raw row of the pre-1.6 table (name, valor, toggle), before any cleaning. */
public final class LegacyAccount {

	private final String name;
	private final String rawBalance;
	private final boolean receivingDisabled;

	public LegacyAccount(String name, String rawBalance, boolean receivingDisabled) {
		this.name = name;
		this.rawBalance = rawBalance;
		this.receivingDisabled = receivingDisabled;
	}

	public String getName() {
		return this.name;
	}

	/** The {@code valor} column: a decimal number stored as text. */
	public String getRawBalance() {
		return this.rawBalance;
	}

	public boolean isReceivingDisabled() {
		return this.receivingDisabled;
	}
}
