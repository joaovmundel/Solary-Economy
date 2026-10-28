package com.redeskyller.bukkit.solaryeconomy.config;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Rules every amount must obey. Amounts are never rounded in the player's favor: money entering an
 * account is cut down ({@link #credit}), money leaving it is rounded up ({@link #debit}).
 */
public final class MoneySettings {

	/** The MySQL column is DECIMAL(40,8). */
	public static final int MAX_DECIMAL_PLACES = 8;

	private final int decimalPlaces;
	private final BigDecimal maxBalance;
	private final BigDecimal startBalance;
	private final BigDecimal payMinimum;

	public MoneySettings(int decimalPlaces, BigDecimal maxBalance, BigDecimal startBalance, BigDecimal payMinimum) {
		this.decimalPlaces = decimalPlaces;
		this.maxBalance = maxBalance;
		this.startBalance = credit(startBalance);
		this.payMinimum = credit(payMinimum);
	}

	public int getDecimalPlaces() {
		return this.decimalPlaces;
	}

	public BigDecimal getMaxBalance() {
		return this.maxBalance;
	}

	public BigDecimal getStartBalance() {
		return this.startBalance;
	}

	public BigDecimal getPayMinimum() {
		return this.payMinimum;
	}

	/** Scales an amount that is being added to an account (truncates extra decimals). */
	public BigDecimal credit(BigDecimal amount) {
		return amount == null ? null : amount.setScale(this.decimalPlaces, RoundingMode.DOWN);
	}

	/** Scales an amount that is being taken from an account (rounds extra decimals up). */
	public BigDecimal debit(BigDecimal amount) {
		return amount == null ? null : amount.setScale(this.decimalPlaces, RoundingMode.UP);
	}

	public boolean exceedsLimit(BigDecimal balance) {
		return balance.compareTo(this.maxBalance) > 0;
	}
}
