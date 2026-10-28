package com.redeskyller.bukkit.solaryeconomy.economy;

/**
 * Observer of successful balance changes (audit log, future Bukkit events, ...).
 * Called synchronously on the thread that performed the change, outside any account lock.
 * Implementations must be fast and thread-safe; exceptions are logged and never abort the operation.
 */
public interface BalanceChangeListener {

	void onBalanceChange(BalanceChange change);
}
