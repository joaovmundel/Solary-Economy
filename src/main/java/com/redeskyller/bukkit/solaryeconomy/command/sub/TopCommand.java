package com.redeskyller.bukkit.solaryeconomy.command.sub;

import java.util.List;

import org.bukkit.command.CommandSender;

import com.redeskyller.bukkit.solaryeconomy.command.AbstractSubCommand;
import com.redeskyller.bukkit.solaryeconomy.command.CommandContext;
import com.redeskyller.bukkit.solaryeconomy.config.PluginSettings;
import com.redeskyller.bukkit.solaryeconomy.config.SettingsProvider;
import com.redeskyller.bukkit.solaryeconomy.economy.LeaderboardView;
import com.redeskyller.bukkit.solaryeconomy.economy.RankAccount;
import com.redeskyller.bukkit.solaryeconomy.format.CurrencyFormatter;
import com.redeskyller.bukkit.solaryeconomy.hook.PrefixProvider;
import com.redeskyller.bukkit.solaryeconomy.message.Message;
import com.redeskyller.bukkit.solaryeconomy.message.Messages;

public final class TopCommand extends AbstractSubCommand {

	private final LeaderboardView leaderboard;
	private final CurrencyFormatter formatter;
	private final SettingsProvider settings;
	private final PrefixProvider prefixes;

	public TopCommand(Messages messages, LeaderboardView leaderboard, CurrencyFormatter formatter,
			SettingsProvider settings, PrefixProvider prefixes) {
		super(messages, "top", "top", "solaryeconomy.commands.top", Message.HELP_TOP, "rank");
		this.leaderboard = leaderboard;
		this.formatter = formatter;
		this.settings = settings;
		this.prefixes = prefixes;
	}

	@Override
	public void execute(CommandContext context) {
		CommandSender sender = context.getSender();
		PluginSettings current = this.settings.current();
		List<RankAccount> entries = this.leaderboard.current().getEntries();

		sender.sendMessage(this.messages.get(Message.MONEY_TOP_TITLE));
		sender.sendMessage(" ");
		if (entries.isEmpty()) {
			sender.sendMessage(this.messages.get(Message.MONEY_TOP_NULL));
		} else {
			int position = 1;
			for (RankAccount account : entries) {
				String name = current.getLeaderboard().isShowPrefix()
						? this.prefixes.getPrefix(account.getName()) + account.getName()
						: account.getName();
				if (position == 1 && current.getMagnate().isTagEnabled())
					name = this.messages.get(Message.MAGNATA_TAG) + name;

				sender.sendMessage(this.messages.get(Message.MONEY_TOP_FORMAT, "i", String.valueOf(position),
						"player", name, "valor", this.formatter.format(account.getBalance())));
				position++;
			}
		}
		sender.sendMessage(" ");
	}
}
