package com.redeskyller.bukkit.solaryeconomy.economy;

/** Outcome of a balance operation. Business failures are results, not exceptions. */
public enum TransactionResult {

	SUCCESS,
	ACCOUNT_NOT_FOUND,
	INVALID_AMOUNT,
	INSUFFICIENT_FUNDS,
	RECEIVING_DISABLED,
	BALANCE_LIMIT_EXCEEDED,
	SAME_ACCOUNT;

	public boolean isSuccess() {
		return this == SUCCESS;
	}
}
