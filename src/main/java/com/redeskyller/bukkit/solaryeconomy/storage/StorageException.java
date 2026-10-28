package com.redeskyller.bukkit.solaryeconomy.storage;

/** The database could not be reached or answered with an error. */
public class StorageException extends Exception {

	private static final long serialVersionUID = 1L;

	public StorageException(String message) {
		super(message);
	}

	public StorageException(String message, Throwable cause) {
		super(message, cause);
	}
}
