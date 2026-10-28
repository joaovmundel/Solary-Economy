package com.redeskyller.bukkit.solaryeconomy.economy;

/** Outcome of an account creation. */
public enum CreateResult {

	CREATED,
	ALREADY_EXISTS,
	INVALID_NAME,
	INVALID_AMOUNT;

	public boolean isCreated() {
		return this == CREATED;
	}
}
