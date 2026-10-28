package com.redeskyller.bukkit.solaryeconomy.economy;

import java.math.BigDecimal;

/** One line of the money top. Immutable. */
public final class RankAccount {

	private final String key;
	private final String name;
	private final BigDecimal balance;

	public RankAccount(String key, String name, BigDecimal balance) {
		this.key = key;
		this.name = name;
		this.balance = balance;
	}

	/** Case-insensitive identity, see {@link AccountNames#keyOf(String)}. */
	public String getKey() {
		return this.key;
	}

	public String getName() {
		return this.name;
	}

	public BigDecimal getBalance() {
		return this.balance;
	}
}
