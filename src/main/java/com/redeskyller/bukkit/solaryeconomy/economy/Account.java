package com.redeskyller.bukkit.solaryeconomy.economy;

import java.math.BigDecimal;

/**
 * Mutable account state. Instances never leave this package: every read-modify-write must happen
 * inside {@code synchronized (account)}, which is how {@link EconomyServiceImpl} keeps balances
 * consistent when Vault consumers call in from other threads.
 */
final class Account {

	private final String key;
	private final String displayName;
	private BigDecimal balance;
	private boolean receivingDisabled;

	Account(String key, String displayName, BigDecimal balance, boolean receivingDisabled) {
		this.key = key;
		this.displayName = displayName;
		this.balance = balance;
		this.receivingDisabled = receivingDisabled;
	}

	String getKey() {
		return this.key;
	}

	String getDisplayName() {
		return this.displayName;
	}

	synchronized BigDecimal getBalance() {
		return this.balance;
	}

	synchronized void setBalance(BigDecimal balance) {
		this.balance = balance;
	}

	synchronized boolean isReceivingDisabled() {
		return this.receivingDisabled;
	}

	synchronized void setReceivingDisabled(boolean receivingDisabled) {
		this.receivingDisabled = receivingDisabled;
	}

	synchronized AccountSnapshot snapshot() {
		return new AccountSnapshot(this.key, this.displayName, this.balance, this.receivingDisabled);
	}
}
