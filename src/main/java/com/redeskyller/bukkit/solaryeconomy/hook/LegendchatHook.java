package com.redeskyller.bukkit.solaryeconomy.hook;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

import com.redeskyller.bukkit.solaryeconomy.config.SettingsProvider;
import com.redeskyller.bukkit.solaryeconomy.economy.AccountNames;
import com.redeskyller.bukkit.solaryeconomy.economy.LeaderboardView;
import com.redeskyller.bukkit.solaryeconomy.economy.RankAccount;
import com.redeskyller.bukkit.solaryeconomy.message.Message;
import com.redeskyller.bukkit.solaryeconomy.message.Messages;

import br.com.devpaulo.legendchat.api.events.ChatMessageEvent;

/**
 * Fills the {@code {solary_economy_magnata}} tag of Legendchat for the richest player. Independent of
 * Vault (the old code only enabled it when {@code use_vault} was on).
 */
public final class LegendchatHook implements Hook, Listener {

	private static final String TAG = "solary_economy_magnata";

	private final LeaderboardView leaderboard;
	private final Messages messages;
	private final SettingsProvider settings;

	private LegendchatHook(LeaderboardView leaderboard, Messages messages, SettingsProvider settings) {
		this.leaderboard = leaderboard;
		this.messages = messages;
		this.settings = settings;
	}

	public static LegendchatHook install(Plugin owner, LeaderboardView leaderboard, Messages messages,
			SettingsProvider settings) {
		LegendchatHook hook = new LegendchatHook(leaderboard, messages, settings);
		owner.getServer().getPluginManager().registerEvents(hook, owner);
		return hook;
	}

	@Override
	public void uninstall() {
		HandlerList.unregisterAll(this);
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onChat(ChatMessageEvent event) {
		if (event.isCancelled() || !this.settings.current().getMagnate().isTagEnabled())
			return;

		Player sender = event.getSender();
		RankAccount magnate = this.leaderboard.current().getMagnate();
		if (sender != null && magnate != null && magnate.getKey().equals(AccountNames.keyOf(sender.getName())))
			event.setTagValue(TAG, this.messages.get(Message.MAGNATA_TAG));
	}
}
