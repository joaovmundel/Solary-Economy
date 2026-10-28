package com.redeskyller.bukkit.solaryeconomy.hook;

import java.util.Locale;

import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.Plugin;

import com.redeskyller.bukkit.solaryeconomy.economy.EconomyService;
import com.redeskyller.bukkit.solaryeconomy.format.CurrencyFormatter;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;

/**
 * {@code %solaryeconomy_balance%} and {@code %solaryeconomy_balance_formatted%}. PlaceholderAPI does not
 * exist for 1.5.2; this only loads on servers where it is installed.
 */
public final class PlaceholdersHook extends PlaceholderExpansion implements Hook {

	private final Plugin owner;
	private final EconomyService economy;
	private final CurrencyFormatter formatter;

	private PlaceholdersHook(Plugin owner, EconomyService economy, CurrencyFormatter formatter) {
		this.owner = owner;
		this.economy = economy;
		this.formatter = formatter;
	}

	public static PlaceholdersHook install(Plugin owner, EconomyService economy, CurrencyFormatter formatter) {
		PlaceholdersHook hook = new PlaceholdersHook(owner, economy, formatter);
		hook.register();
		return hook;
	}

	@Override
	public void uninstall() {
		// PlaceholderAPI 2.9 has no unregister; the expansion simply stops answering once the plugin is disabled
	}

	@Override
	public String onRequest(OfflinePlayer player, String placeholder) {
		if (player == null || player.getName() == null)
			return "";

		String balanceKey = placeholder.toLowerCase(Locale.ENGLISH);
		if (balanceKey.equals("balance"))
			return this.formatter.format(this.economy.getBalance(player.getName()));
		if (balanceKey.equals("balance_formatted"))
			return this.formatter.formatCompact(this.economy.getBalance(player.getName()));
		return null;
	}

	@Override
	public String getAuthor() {
		return "Sr_Edition";
	}

	@Override
	public String getIdentifier() {
		return "solaryeconomy";
	}

	@Override
	public String getVersion() {
		return this.owner.getDescription().getVersion();
	}
}
