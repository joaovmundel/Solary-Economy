package com.redeskyller.bukkit.solaryeconomy.listener;

import java.util.logging.Logger;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import com.redeskyller.bukkit.solaryeconomy.economy.CreateResult;
import com.redeskyller.bukkit.solaryeconomy.economy.EconomyService;

/**
 * Gives every player an account on first join. This is a map insert, not a database call: the write
 * happens later in the background, so logins never wait on the database.
 */
public final class AccountListener implements Listener {

	private static final int MAX_LOGGED_NAME = 40;

	private final EconomyService economy;
	private final Logger logger;

	public AccountListener(EconomyService economy, Logger logger) {
		this.economy = economy;
		this.logger = logger;
	}

	@EventHandler
	public void onJoin(PlayerJoinEvent event) {
		ensureAccount(event.getPlayer().getName());
	}

	/** Creates the account unless it exists. A name that cannot be an account is reported, not stored. */
	public void ensureAccount(String playerName) {
		if (this.economy.createAccount(playerName) == CreateResult.INVALID_NAME)
			this.logger.warning("Nenhuma conta criada para o nome inválido '" + printable(playerName)
					+ "'. Em servidores piratas isto costuma ser uma tentativa de ataque; considere usar AuthMe.");
	}

	/** Names come straight from the client in offline mode, so they are made safe before being logged. */
	private static String printable(String name) {
		String text = name == null ? "null" : name;
		if (text.length() > MAX_LOGGED_NAME)
			text = text.substring(0, MAX_LOGGED_NAME) + "...";
		return text.replaceAll("[\\p{Cntrl}§]", "?");
	}
}
