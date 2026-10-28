package com.redeskyller.bukkit.solaryeconomy.format;

/** The text typed by a player is not an amount. The message says why, for logs and debugging. */
public class InvalidAmountException extends Exception {

	private static final long serialVersionUID = 1L;

	public InvalidAmountException(String message) {
		super(message);
	}
}
