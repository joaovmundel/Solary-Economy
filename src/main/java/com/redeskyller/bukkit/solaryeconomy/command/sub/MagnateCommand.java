package com.redeskyller.bukkit.solaryeconomy.command.sub;

import org.bukkit.command.CommandSender;

import com.redeskyller.bukkit.solaryeconomy.command.AbstractSubCommand;
import com.redeskyller.bukkit.solaryeconomy.command.CommandContext;
import com.redeskyller.bukkit.solaryeconomy.config.SettingsProvider;
import com.redeskyller.bukkit.solaryeconomy.economy.LeaderboardView;
import com.redeskyller.bukkit.solaryeconomy.economy.RankAccount;
import com.redeskyller.bukkit.solaryeconomy.format.CurrencyFormatter;
import com.redeskyller.bukkit.solaryeconomy.hook.PrefixProvider;
import com.redeskyller.bukkit.solaryeconomy.message.Message;
import com.redeskyller.bukkit.solaryeconomy.message.Messages;

public final class MagnateCommand extends AbstractSubCommand {

	private final LeaderboardView leaderboard;
	private final CurrencyFormatter formatter;
	private final SettingsProvider settings;
	private final PrefixProvider prefixes;

	public MagnateCommand(Messages messages, LeaderboardView leaderboard, CurrencyFormatter formatter,
			SettingsProvider settings, PrefixProvider prefixes) {
		super(messages, "magnata", "magnata", "solaryeconomy.commands.magnata", Message.HELP_MAGNATA);
		this.leaderboard = leaderboard;
		this.formatter = formatter;
		this.settings = settings;
		this.prefixes = prefixes;
	}

	@Override
	public void execute(CommandContext context) {
		CommandSender sender = context.getSender();
		RankAccount magnate = this.leaderboard.current().getMagnate();

		sender.sendMessage(" ");
		if (magnate == null) {
			sender.sendMessage(this.messages.get(Message.MAGNATA_NOT_FOUND));
		} else {
			String name = this.settings.current().getLeaderboard().isShowPrefix()
					? this.prefixes.getPrefix(magnate.getName()) + magnate.getName()
					: magnate.getName();
			sender.sendMessage(this.messages.get(Message.MAGNATA_VIEW, "player", name, "valor",
					this.formatter.format(magnate.getBalance())));
		}
		sender.sendMessage(" ");
	}
}
