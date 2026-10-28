package com.redeskyller.bukkit.solaryeconomy;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

import org.bukkit.plugin.java.JavaPlugin;

import com.redeskyller.bukkit.solaryeconomy.config.ConfigException;
import com.redeskyller.bukkit.solaryeconomy.economy.EconomyService;
import com.redeskyller.bukkit.solaryeconomy.economy.RankAccount;
import com.redeskyller.bukkit.solaryeconomy.format.CurrencyFormatter;
import com.redeskyller.bukkit.solaryeconomy.storage.StorageException;

/**
 * Plugin entry point. It only starts and stops the {@link EconomyRuntime}; if startup fails the plugin
 * disables itself with the reason in the log instead of running half-alive (no database, no balances).
 * <p>
 * Other plugins should normally go through Vault. The static accessors below are the small public API
 * for those that want more (the richest player, the money top).
 */
public class SolaryEconomy extends JavaPlugin {

	private static volatile SolaryEconomy instance;

	private volatile EconomyRuntime runtime;

	@Override
	public void onEnable() {
		EconomyRuntime starting = new EconomyRuntime(this);
		try {
			starting.start();
		} catch (ConfigException | StorageException exception) {
			abortStartup("Não foi possível iniciar o Solary-Economy: " + exception.getMessage(), exception);
			return;
		} catch (RuntimeException exception) {
			abortStartup("Erro inesperado ao iniciar o Solary-Economy.", exception);
			return;
		}

		this.runtime = starting;
		instance = this;
		getLogger().info("Solary-Economy " + getDescription().getVersion() + " habilitado.");
	}

	@Override
	public void onDisable() {
		EconomyRuntime stopping = this.runtime;
		this.runtime = null;
		instance = null;
		if (stopping != null)
			stopping.stop();
	}

	private void abortStartup(String message, Exception cause) {
		getLogger().log(Level.SEVERE, message + " O plugin será desabilitado.", cause);
		getServer().getPluginManager().disablePlugin(this);
	}

	public static SolaryEconomy getInstance() {
		return instance;
	}

	/** @return the economy, or {@code null} while the plugin is not enabled */
	public static EconomyService getEconomy() {
		SolaryEconomy plugin = instance;
		return plugin == null || plugin.runtime == null ? null : plugin.runtime.getEconomy();
	}

	/** @return the richest account of the last ranking refresh, or {@code null} */
	public static RankAccount getMagnata() {
		SolaryEconomy plugin = instance;
		return plugin == null || plugin.runtime == null ? null : plugin.runtime.getLeaderboard().current().getMagnate();
	}

	/** @return the money top, richest first (empty when the plugin is not enabled) */
	public static List<RankAccount> getMoneyTop() {
		SolaryEconomy plugin = instance;
		if (plugin == null || plugin.runtime == null)
			return Collections.emptyList();
		return plugin.runtime.getLeaderboard().current().getEntries();
	}

	/** @return whether the account has switched off receiving money with {@code /money toggle} */
	public static boolean isToggle(String account) {
		EconomyService economy = getEconomy();
		return economy != null && economy.isReceivingDisabled(account);
	}

	/** @return the amount as the plugin shows it in messages ("1.5k" or "1.500 coins"), or the plain number if disabled */
	public static String format(BigDecimal amount) {
		SolaryEconomy plugin = instance;
		if (plugin == null || plugin.runtime == null)
			return amount.toPlainString();
		CurrencyFormatter formatter = plugin.runtime.getFormatter();
		return formatter.format(amount);
	}
}
