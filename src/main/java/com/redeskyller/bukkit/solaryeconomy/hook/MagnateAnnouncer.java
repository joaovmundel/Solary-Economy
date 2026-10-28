package com.redeskyller.bukkit.solaryeconomy.hook;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import com.redeskyller.bukkit.solaryeconomy.config.SettingsProvider;
import com.redeskyller.bukkit.solaryeconomy.economy.MagnateChangeListener;
import com.redeskyller.bukkit.solaryeconomy.economy.RankAccount;
import com.redeskyller.bukkit.solaryeconomy.format.CurrencyFormatter;
import com.redeskyller.bukkit.solaryeconomy.message.Message;
import com.redeskyller.bukkit.solaryeconomy.message.Messages;

/**
 * Tells everyone online about a new magnate. The leaderboard is refreshed on an async thread, but the
 * Bukkit API (broadcasting, prefix lookups) is only safe on the main thread, so the announcement is
 * handed over to the scheduler.
 */
public final class MagnateAnnouncer implements MagnateChangeListener {

	private final Plugin plugin;
	private final Messages messages;
	private final CurrencyFormatter formatter;
	private final SettingsProvider settings;
	private final PrefixProvider prefixes;

	public MagnateAnnouncer(Plugin plugin, Messages messages, CurrencyFormatter formatter, SettingsProvider settings,
			PrefixProvider prefixes) {
		this.plugin = plugin;
		this.messages = messages;
		this.formatter = formatter;
		this.settings = settings;
		this.prefixes = prefixes;
	}

	@Override
	public void onMagnateChanged(RankAccount previous, final RankAccount current) {
		if (!this.settings.current().getMagnate().isBroadcastEnabled() || !this.plugin.isEnabled())
			return;

		Bukkit.getScheduler().runTask(this.plugin, new Runnable() {
			@Override
			public void run() {
				announce(current);
			}
		});
	}

	private void announce(RankAccount magnate) {
		String name = this.settings.current().getLeaderboard().isShowPrefix()
				? this.prefixes.getPrefix(magnate.getName()) + magnate.getName()
				: magnate.getName();

		Bukkit.broadcastMessage(" ");
		Bukkit.broadcastMessage(this.messages.get(Message.MAGNATA_NEW, "player", name, "valor",
				this.formatter.format(magnate.getBalance())));
		Bukkit.broadcastMessage(" ");
	}
}
