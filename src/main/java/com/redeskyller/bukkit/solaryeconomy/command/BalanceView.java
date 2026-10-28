package com.redeskyller.bukkit.solaryeconomy.command;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.redeskyller.bukkit.solaryeconomy.economy.AccountSnapshot;
import com.redeskyller.bukkit.solaryeconomy.economy.EconomyService;
import com.redeskyller.bukkit.solaryeconomy.format.CurrencyFormatter;
import com.redeskyller.bukkit.solaryeconomy.message.Message;
import com.redeskyller.bukkit.solaryeconomy.message.Messages;

/** What {@code /money} and {@code /money <player>} print: a balance, not a subcommand. */
final class BalanceView {

	private final EconomyService economy;
	private final CurrencyFormatter formatter;
	private final Messages messages;

	BalanceView(EconomyService economy, CurrencyFormatter formatter, Messages messages) {
		this.economy = economy;
		this.formatter = formatter;
		this.messages = messages;
	}

	void showOwn(CommandSender sender, String label) {
		if (!(sender instanceof Player)) {
			sender.sendMessage(this.messages.get(Message.CONSOLE_HINT, "label", label));
			return;
		}
		sender.sendMessage(this.messages.get(Message.MONEY, "valor",
				this.formatter.format(this.economy.getBalance(sender.getName()))));
	}

	void showOther(CommandSender sender, String label, String accountName) {
		if (sender.getName().equalsIgnoreCase(accountName)) {
			showOwn(sender, label);
			return;
		}
		if (!sender.hasPermission(Permissions.MONEY_OTHER)) {
			sender.sendMessage(this.messages.get(Message.NO_PERMISSION));
			return;
		}

		AccountSnapshot account = this.economy.getAccount(accountName);
		if (account == null)
			sender.sendMessage(this.messages.get(Message.PLAYER_NOTFOUND, "player", accountName));
		else
			sender.sendMessage(this.messages.get(Message.MONEY_OTHER, "player", account.getDisplayName(), "valor",
					this.formatter.format(account.getBalance())));
	}
}
