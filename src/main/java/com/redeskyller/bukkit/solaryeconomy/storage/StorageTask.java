package com.redeskyller.bukkit.solaryeconomy.storage;

/** A unit of database work, always run on the storage thread. */
public interface StorageTask {

	void run() throws Exception;
}
