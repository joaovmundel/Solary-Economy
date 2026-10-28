package com.redeskyller.bukkit.solaryeconomy.storage;

import java.sql.Connection;
import java.sql.SQLException;

/** Hands out the connection to the database, reconnecting when it was lost. */
public interface ConnectionProvider {

	/** @return a working connection. Owned by the provider: callers must not close it. */
	Connection get() throws SQLException;

	void close();
}
