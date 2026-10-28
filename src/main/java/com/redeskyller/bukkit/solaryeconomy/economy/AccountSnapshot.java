package com.redeskyller.bukkit.solaryeconomy.economy;

import java.math.BigDecimal;

/** Immutable copy of an account's state, safe to hand to other threads (persistence, ranking). */
public final class AccountSnapshot {

	private final String key;
	private final String displayName;
	private final BigDecimal balance;
	private final boolean receivingDisabled;

	public AccountSnapshot(String key, String displayName, BigDecimal balance, boolean receivingDisabled) {
		if (key == null || displayName == null || balance == null)
			throw new IllegalArgumentException("key, displayName and balance are required");
		this.key = key;
		this.displayName = displayName;
		this.balance = balance;
		this.receivingDisabled = receivingDisabled;
	}

	/** Lower-case lookup key, see {@link AccountNames#keyOf(String)}. */
	public String getKey() {
		return this.key;
	}

	public String getDisplayName() {
		return this.displayName;
	}

	public BigDecimal getBalance() {
		return this.balance;
	}

	public boolean isReceivingDisabled() {
		return this.receivingDisabled;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other)
			return true;
		if (!(other instanceof AccountSnapshot))
			return false;
		AccountSnapshot that = (AccountSnapshot) other;
		return this.key.equals(that.key) && this.displayName.equals(that.displayName)
				&& this.balance.compareTo(that.balance) == 0 && this.receivingDisabled == that.receivingDisabled;
	}

	@Override
	public int hashCode() {
		return 31 * this.key.hashCode() + this.displayName.hashCode();
	}

	@Override
	public String toString() {
		return "AccountSnapshot[" + this.displayName + ", balance=" + this.balance.toPlainString()
				+ ", receivingDisabled=" + this.receivingDisabled + "]";
	}
}
