package com.redeskyller.bukkit.solaryeconomy.storage;

import java.util.List;

/** Read-only access to the data of plugin versions before 1.6. */
public interface LegacyAccountSource {

	boolean exists() throws StorageException;

	List<LegacyAccount> readAll() throws StorageException;
}
