package com.redeskyller.bukkit.solaryeconomy.hook;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.ServicePriority;

import net.milkbowl.vault.economy.Economy;

/**
 * Registers the economy with Vault. Kept apart from the rest so the Vault classes are only loaded
 * when Vault is installed. Priority is {@code Highest} because this plugin is meant to be the
 * server's economy; other economy plugins (Essentials' own) stay registered but are not used.
 */
public final class VaultHook implements Hook {

	private final VaultEconomyProvider provider;

	private VaultHook(VaultEconomyProvider provider) {
		this.provider = provider;
	}

	public static VaultHook install(Plugin owner, VaultEconomyProvider provider) {
		Bukkit.getServicesManager().register(Economy.class, provider, owner, ServicePriority.Highest);
		return new VaultHook(provider);
	}

	@Override
	public void uninstall() {
		Bukkit.getServicesManager().unregister(Economy.class, this.provider);
	}
}
