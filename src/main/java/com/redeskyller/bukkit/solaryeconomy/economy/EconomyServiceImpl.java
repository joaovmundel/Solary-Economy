package com.redeskyller.bukkit.solaryeconomy.economy;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.redeskyller.bukkit.solaryeconomy.config.MoneySettings;
import com.redeskyller.bukkit.solaryeconomy.config.SettingsProvider;

/**
 * In-memory economy. All accounts live in a {@link ConcurrentMap}; persistence is handled elsewhere
 * through {@link DirtyAccountSource}. Locking rules: an account is only read-modified-written inside
 * {@code synchronized (account)}, and when two accounts are needed they are locked in key order, so
 * concurrent transfers in opposite directions cannot deadlock.
 */
public final class EconomyServiceImpl implements EconomyService, DirtyAccountSource {

	private final ConcurrentMap<String, Account> accounts = new ConcurrentHashMap<String, Account>();
	private final Set<String> dirtyKeys = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());
	private final List<BalanceChangeListener> listeners = new CopyOnWriteArrayList<BalanceChangeListener>();
	private final SettingsProvider settings;
	private final Logger logger;

	public EconomyServiceImpl(SettingsProvider settings, Logger logger) {
		this.settings = settings;
		this.logger = logger;
	}

	/** Populates the economy from storage at startup. Loaded accounts are clean: nothing is rewritten. */
	public void load(Collection<AccountSnapshot> snapshots) {
		for (AccountSnapshot snapshot : snapshots)
			this.accounts.put(snapshot.getKey(), new Account(snapshot.getKey(), snapshot.getDisplayName(),
					snapshot.getBalance(), snapshot.isReceivingDisabled()));
	}

	@Override
	public boolean hasAccount(String name) {
		return find(name) != null;
	}

	@Override
	public AccountSnapshot getAccount(String name) {
		Account account = find(name);
		return account == null ? null : account.snapshot();
	}

	@Override
	public BigDecimal getBalance(String name) {
		Account account = find(name);
		return account == null ? BigDecimal.ZERO : account.getBalance();
	}

	@Override
	public boolean has(String name, BigDecimal amount) {
		BigDecimal needed = debitAmount(amount);
		Account account = find(name);
		return needed != null && account != null && account.getBalance().compareTo(needed) >= 0;
	}

	@Override
	public boolean isReceivingDisabled(String name) {
		Account account = find(name);
		return account != null && account.isReceivingDisabled();
	}

	@Override
	public List<AccountSnapshot> getAccounts() {
		List<AccountSnapshot> snapshots = new ArrayList<AccountSnapshot>(this.accounts.size());
		for (Account account : this.accounts.values())
			snapshots.add(account.snapshot());
		return snapshots;
	}

	@Override
	public CreateResult createAccount(String name) {
		return createAccount(name, this.settings.current().getMoney().getStartBalance());
	}

	@Override
	public CreateResult createAccount(String name, BigDecimal initialBalance) {
		String key = AccountNames.keyOf(name);
		if (key == null)
			return CreateResult.INVALID_NAME;

		MoneySettings money = this.settings.current().getMoney();
		BigDecimal balance = creditAmount(initialBalance);
		if (balance == null || money.exceedsLimit(balance))
			return CreateResult.INVALID_AMOUNT;

		Account account = new Account(key, AccountNames.displayNameOf(name), balance, false);
		if (this.accounts.putIfAbsent(key, account) != null)
			return CreateResult.ALREADY_EXISTS;

		changed(account, BalanceChange.Type.CREATE, null, balance, BigDecimal.ZERO, balance);
		return CreateResult.CREATED;
	}

	@Override
	public boolean deleteAccount(String name) {
		String key = AccountNames.keyOf(name);
		Account account = key == null ? null : this.accounts.remove(key);
		if (account == null)
			return false;

		this.dirtyKeys.remove(key);
		BigDecimal balance = account.getBalance();
		fire(new BalanceChange(BalanceChange.Type.DELETE, account.getDisplayName(), null, balance, balance,
				BigDecimal.ZERO));
		return true;
	}

	@Override
	public TransactionResult deposit(String name, BigDecimal amount) {
		BigDecimal value = creditAmount(amount);
		if (value == null)
			return TransactionResult.INVALID_AMOUNT;
		Account account = find(name);
		if (account == null)
			return TransactionResult.ACCOUNT_NOT_FOUND;
		if (value.signum() == 0)
			return TransactionResult.SUCCESS;

		MoneySettings money = this.settings.current().getMoney();
		BigDecimal before;
		BigDecimal after;
		synchronized (account) {
			before = account.getBalance();
			after = before.add(value);
			if (money.exceedsLimit(after))
				return TransactionResult.BALANCE_LIMIT_EXCEEDED;
			account.setBalance(after);
		}
		changed(account, BalanceChange.Type.DEPOSIT, null, value, before, after);
		return TransactionResult.SUCCESS;
	}

	@Override
	public TransactionResult withdraw(String name, BigDecimal amount) {
		BigDecimal value = debitAmount(amount);
		if (value == null)
			return TransactionResult.INVALID_AMOUNT;
		Account account = find(name);
		if (account == null)
			return TransactionResult.ACCOUNT_NOT_FOUND;
		if (value.signum() == 0)
			return TransactionResult.SUCCESS;

		BigDecimal before;
		BigDecimal after;
		synchronized (account) {
			before = account.getBalance();
			if (before.compareTo(value) < 0)
				return TransactionResult.INSUFFICIENT_FUNDS;
			after = before.subtract(value);
			account.setBalance(after);
		}
		changed(account, BalanceChange.Type.WITHDRAW, null, value, before, after);
		return TransactionResult.SUCCESS;
	}

	@Override
	public TransactionResult setBalance(String name, BigDecimal amount) {
		BigDecimal value = creditAmount(amount);
		if (value == null)
			return TransactionResult.INVALID_AMOUNT;
		Account account = find(name);
		if (account == null)
			return TransactionResult.ACCOUNT_NOT_FOUND;
		if (this.settings.current().getMoney().exceedsLimit(value))
			return TransactionResult.BALANCE_LIMIT_EXCEEDED;

		BigDecimal before;
		synchronized (account) {
			before = account.getBalance();
			account.setBalance(value);
		}
		changed(account, BalanceChange.Type.SET, null, value, before, value);
		return TransactionResult.SUCCESS;
	}

	@Override
	public TransactionResult transfer(String fromName, String toName, BigDecimal amount) {
		// one scaling for both sides: the payer loses exactly what the payee receives
		BigDecimal value = creditAmount(amount);
		if (value == null || value.signum() == 0)
			return TransactionResult.INVALID_AMOUNT;
		Account from = find(fromName);
		Account to = find(toName);
		if (from == null || to == null)
			return TransactionResult.ACCOUNT_NOT_FOUND;
		if (from == to)
			return TransactionResult.SAME_ACCOUNT;

		MoneySettings money = this.settings.current().getMoney();
		Account first = from.getKey().compareTo(to.getKey()) < 0 ? from : to;
		Account second = first == from ? to : from;

		BigDecimal fromBefore;
		BigDecimal fromAfter;
		BigDecimal toBefore;
		BigDecimal toAfter;
		synchronized (first) {
			synchronized (second) {
				fromBefore = from.getBalance();
				if (fromBefore.compareTo(value) < 0)
					return TransactionResult.INSUFFICIENT_FUNDS;
				if (to.isReceivingDisabled())
					return TransactionResult.RECEIVING_DISABLED;
				toBefore = to.getBalance();
				toAfter = toBefore.add(value);
				if (money.exceedsLimit(toAfter))
					return TransactionResult.BALANCE_LIMIT_EXCEEDED;

				fromAfter = fromBefore.subtract(value);
				from.setBalance(fromAfter);
				to.setBalance(toAfter);
			}
		}
		changed(from, BalanceChange.Type.TRANSFER_OUT, to.getDisplayName(), value, fromBefore, fromAfter);
		changed(to, BalanceChange.Type.TRANSFER_IN, from.getDisplayName(), value, toBefore, toAfter);
		return TransactionResult.SUCCESS;
	}

	@Override
	public TransactionResult setReceivingDisabled(String name, boolean disabled) {
		Account account = find(name);
		if (account == null)
			return TransactionResult.ACCOUNT_NOT_FOUND;

		account.setReceivingDisabled(disabled);
		this.dirtyKeys.add(account.getKey());
		return TransactionResult.SUCCESS;
	}

	@Override
	public void addListener(BalanceChangeListener listener) {
		this.listeners.add(listener);
	}

	@Override
	public void removeListener(BalanceChangeListener listener) {
		this.listeners.remove(listener);
	}

	@Override
	public List<AccountSnapshot> drainDirty() {
		List<AccountSnapshot> batch = new ArrayList<AccountSnapshot>();
		Iterator<String> keys = this.dirtyKeys.iterator();
		while (keys.hasNext()) {
			String key = keys.next();
			// unmark first, snapshot second: a change racing with this loop marks the account again
			keys.remove();
			Account account = this.accounts.get(key);
			if (account != null)
				batch.add(account.snapshot());
		}
		return batch;
	}

	@Override
	public void restoreDirty(Collection<String> keys) {
		this.dirtyKeys.addAll(keys);
	}

	private Account find(String name) {
		String key = AccountNames.keyOf(name);
		return key == null ? null : this.accounts.get(key);
	}

	/** @return the amount scaled for money entering an account, or {@code null} if it is null or negative */
	private BigDecimal creditAmount(BigDecimal amount) {
		if (amount == null || amount.signum() < 0)
			return null;
		return this.settings.current().getMoney().credit(amount);
	}

	/** @return the amount scaled for money leaving an account, or {@code null} if it is null or negative */
	private BigDecimal debitAmount(BigDecimal amount) {
		if (amount == null || amount.signum() < 0)
			return null;
		return this.settings.current().getMoney().debit(amount);
	}

	private void changed(Account account, BalanceChange.Type type, String counterpart, BigDecimal amount,
			BigDecimal before, BigDecimal after) {
		this.dirtyKeys.add(account.getKey());
		fire(new BalanceChange(type, account.getDisplayName(), counterpart, amount, before, after));
	}

	private void fire(BalanceChange change) {
		for (BalanceChangeListener listener : this.listeners) {
			try {
				listener.onBalanceChange(change);
			} catch (RuntimeException exception) {
				this.logger.log(Level.WARNING, "Um observador de saldo falhou ao processar " + change, exception);
			}
		}
	}
}
