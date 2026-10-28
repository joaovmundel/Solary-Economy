package com.redeskyller.bukkit.solaryeconomy.economy;

import java.math.BigDecimal;
import java.util.List;

/**
 * The economy: accounts and the operations on their balances. Thread-safe; every operation is
 * atomic, so it can be called from the main thread and from other plugins' async tasks alike.
 * Account names are case-insensitive (see {@link AccountNames}).
 */
public interface EconomyService {

	boolean hasAccount(String name);

	/** @return the current state of the account, or {@code null} when it does not exist */
	AccountSnapshot getAccount(String name);

	/** @return the balance, or zero when the account does not exist */
	BigDecimal getBalance(String name);

	/** @return whether the account exists and holds at least {@code amount} */
	boolean has(String name, BigDecimal amount);

	boolean isReceivingDisabled(String name);

	/** @return a snapshot of every account, in no particular order */
	List<AccountSnapshot> getAccounts();

	/** Creates an account with the configured start balance. */
	CreateResult createAccount(String name);

	CreateResult createAccount(String name, BigDecimal initialBalance);

	/** @return whether an account was removed */
	boolean deleteAccount(String name);

	TransactionResult deposit(String name, BigDecimal amount);

	/** Never takes an account below zero. */
	TransactionResult withdraw(String name, BigDecimal amount);

	TransactionResult setBalance(String name, BigDecimal amount);

	/** Moves money atomically: either both balances change or neither does. */
	TransactionResult transfer(String from, String to, BigDecimal amount);

	TransactionResult setReceivingDisabled(String name, boolean disabled);

	void addListener(BalanceChangeListener listener);

	void removeListener(BalanceChangeListener listener);
}
