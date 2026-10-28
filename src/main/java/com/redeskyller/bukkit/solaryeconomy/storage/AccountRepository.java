package com.redeskyller.bukkit.solaryeconomy.storage;

import java.util.Collection;
import java.util.List;

import com.redeskyller.bukkit.solaryeconomy.economy.AccountSnapshot;

/** Blocking persistence of accounts. Call it from the storage thread. */
public interface AccountRepository {

	/** Creates the schema when it does not exist yet. */
	void initialize() throws StorageException;

	long count() throws StorageException;

	List<AccountSnapshot> loadAll() throws StorageException;

	/** Inserts or updates every account in a single transaction: all of them are written or none. */
	void saveAll(Collection<AccountSnapshot> accounts) throws StorageException;

	void delete(String key) throws StorageException;

	void close();
}
