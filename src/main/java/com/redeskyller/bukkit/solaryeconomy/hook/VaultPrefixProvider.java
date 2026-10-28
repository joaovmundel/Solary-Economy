package com.redeskyller.bukkit.solaryeconomy.hook;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

import com.redeskyller.bukkit.solaryeconomy.config.SettingsProvider;

import net.milkbowl.vault.chat.Chat;

/**
 * Prefixes from whichever chat plugin Vault is linked to. An offline player is looked up in the
 * configured main world (the previous implementation passed the wrong key to the config and always
 * ended up with a {@code null} world).
 */
public final class VaultPrefixProvider implements PrefixProvider {

	private final SettingsProvider settings;
	private final Logger logger;
	private final AtomicBoolean failureReported = new AtomicBoolean();

	public VaultPrefixProvider(SettingsProvider settings, Logger logger) {
		this.settings = settings;
		this.logger = logger;
	}

	@Override
	public String getPrefix(String accountName) {
		RegisteredServiceProvider<Chat> registration = Bukkit.getServicesManager().getRegistration(Chat.class);
		if (registration == null || registration.getProvider() == null)
			return "";

		try {
			Player online = Bukkit.getPlayerExact(accountName);
			String world = online != null ? online.getWorld().getName() : this.settings.current().getGeneral().getWorld();
			String prefix = registration.getProvider().getPlayerPrefix(world, accountName);
			return prefix == null ? "" : ChatColor.translateAlternateColorCodes('&', prefix);
		} catch (RuntimeException | LinkageError exception) {
			// a misbehaving chat plugin must not break /money top; say so once, not on every line
			if (this.failureReported.compareAndSet(false, true))
				this.logger.log(Level.WARNING, "O Vault não conseguiu fornecer o prefixo de '" + accountName
						+ "'; os prefixos serão omitidos enquanto isso falhar.", exception);
			return "";
		}
	}
}
