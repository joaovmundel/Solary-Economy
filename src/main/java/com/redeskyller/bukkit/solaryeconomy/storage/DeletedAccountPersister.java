package com.redeskyller.bukkit.solaryeconomy.storage;

import com.redeskyller.bukkit.solaryeconomy.economy.AccountNames;
import com.redeskyller.bukkit.solaryeconomy.economy.BalanceChange;
import com.redeskyller.bukkit.solaryeconomy.economy.BalanceChangeListener;

/**
 * Removes deleted accounts from the database. Writes are driven by the dirty set, but a deleted
 * account has no state left to flush, so deletions are queued on the storage thread explicitly
 * (where they stay ordered after any write requested before them).
 */
public final class DeletedAccountPersister implements BalanceChangeListener {

	private final AccountRepository repository;
	private final StorageExecutor executor;

	public DeletedAccountPersister(AccountRepository repository, StorageExecutor executor) {
		this.repository = repository;
		this.executor = executor;
	}

	@Override
	public void onBalanceChange(BalanceChange change) {
		if (change.getType() != BalanceChange.Type.DELETE)
			return;

		final String key = AccountNames.keyOf(change.getAccount());
		this.executor.execute("remoção da conta '" + change.getAccount() + "'", new StorageTask() {
			@Override
			public void run() throws StorageException {
				DeletedAccountPersister.this.repository.delete(key);
			}
		});
	}
}
