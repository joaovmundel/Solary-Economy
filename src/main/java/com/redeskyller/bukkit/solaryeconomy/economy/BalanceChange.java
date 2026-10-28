package com.redeskyller.bukkit.solaryeconomy.economy;

import java.math.BigDecimal;

/** Immutable description of a successful change, delivered to {@link BalanceChangeListener}s. */
public final class BalanceChange {

	public enum Type {
		CREATE, DELETE, DEPOSIT, WITHDRAW, SET, TRANSFER_IN, TRANSFER_OUT
	}

	private final Type type;
	private final String account;
	private final String counterpart;
	private final BigDecimal amount;
	private final BigDecimal balanceBefore;
	private final BigDecimal balanceAfter;

	public BalanceChange(Type type, String account, String counterpart, BigDecimal amount, BigDecimal balanceBefore,
			BigDecimal balanceAfter) {
		this.type = type;
		this.account = account;
		this.counterpart = counterpart;
		this.amount = amount;
		this.balanceBefore = balanceBefore;
		this.balanceAfter = balanceAfter;
	}

	public Type getType() {
		return this.type;
	}

	/** Display name of the account that changed. */
	public String getAccount() {
		return this.account;
	}

	/** The other side of a transfer, {@code null} for every other type. */
	public String getCounterpart() {
		return this.counterpart;
	}

	public BigDecimal getAmount() {
		return this.amount;
	}

	public BigDecimal getBalanceBefore() {
		return this.balanceBefore;
	}

	public BigDecimal getBalanceAfter() {
		return this.balanceAfter;
	}

	@Override
	public String toString() {
		StringBuilder line = new StringBuilder();
		line.append(this.type).append(" | ").append(this.account);
		if (this.counterpart != null)
			line.append(" | ").append(this.counterpart);
		line.append(" | amount=").append(this.amount.toPlainString());
		line.append(" | ").append(this.balanceBefore.toPlainString()).append(" -> ")
				.append(this.balanceAfter.toPlainString());
		return line.toString();
	}
}
