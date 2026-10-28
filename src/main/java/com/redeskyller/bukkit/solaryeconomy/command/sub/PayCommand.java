package com.redeskyller.bukkit.solaryeconomy.command.sub;

import java.math.BigDecimal;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.redeskyller.bukkit.solaryeconomy.command.AbstractSubCommand;
import com.redeskyller.bukkit.solaryeconomy.command.CommandContext;
import com.redeskyller.bukkit.solaryeconomy.config.MoneySettings;
import com.redeskyller.bukkit.solaryeconomy.config.SettingsProvider;
import com.redeskyller.bukkit.solaryeconomy.economy.AccountSnapshot;
import com.redeskyller.bukkit.solaryeconomy.economy.EconomyService;
import com.redeskyller.bukkit.solaryeconomy.economy.TransactionResult;
import com.redeskyller.bukkit.solaryeconomy.format.AmountParser;
import com.redeskyller.bukkit.solaryeconomy.format.CurrencyFormatter;
import com.redeskyller.bukkit.solaryeconomy.format.InvalidAmountException;
import com.redeskyller.bukkit.solaryeconomy.message.Message;
import com.redeskyller.bukkit.solaryeconomy.message.Messages;

public final class PayCommand extends AbstractSubCommand {

	private final EconomyService economy;
	private final AmountParser parser;
	private final CurrencyFormatter formatter;
	private final SettingsProvider settings;

	public PayCommand(Messages messages, EconomyService economy, AmountParser parser, CurrencyFormatter formatter,
			SettingsProvider settings) {
		super(messages, "pay", "pay [jogador] [valor]", "solaryeconomy.commands.pay", Message.HELP_PAY, "pagar",
				"enviar");
		this.economy = economy;
		this.parser = parser;
		this.formatter = formatter;
		this.settings = settings;
	}

	@Override
	public void execute(CommandContext context) {
		CommandSender sender = context.getSender();
		if (!(sender instanceof Player)) {
			sender.sendMessage(this.messages.get(Message.PLAYER_ONLY));
			return;
		}
		if (context.argCount() < 2) {
			sendUsage(context);
			return;
		}

		String targetName = context.arg(0);
		BigDecimal amount;
		try {
			amount = this.parser.parse(context.arg(1));
		} catch (InvalidAmountException exception) {
			sender.sendMessage(this.messages.get(Message.NUMBER_NULL));
			return;
		}

		MoneySettings money = this.settings.current().getMoney();
		if (amount.signum() <= 0 || amount.compareTo(money.getPayMinimum()) < 0) {
			sender.sendMessage(this.messages.get(Message.NUMBER_NULL));
			return;
		}

		TransactionResult result = this.economy.transfer(sender.getName(), targetName, amount);
		if (result.isSuccess())
			notifyBoth((Player) sender, targetName, amount);
		else
			sender.sendMessage(failureMessage(result));
	}

	@Override
	public List<String> complete(CommandContext context) {
		return completePlayerAtFirstArgument(context);
	}

	private void notifyBoth(Player sender, String targetName, BigDecimal amount) {
		String value = this.formatter.format(amount);
		AccountSnapshot target = this.economy.getAccount(targetName);
		String shownTarget = target == null ? targetName : target.getDisplayName();
		sender.sendMessage(this.messages.get(Message.MONEY_PAY_SENDER, "player", shownTarget, "valor", value));

		Player online = Bukkit.getPlayerExact(targetName);
		if (online != null && online != sender)
			online.sendMessage(this.messages.get(Message.MONEY_PAY_RECEIVER, "player", sender.getName(), "valor", value));
	}

	private String failureMessage(TransactionResult result) {
		switch (result) {
		case SAME_ACCOUNT:
			return this.messages.get(Message.MONEY_PAY_ERRO);
		case ACCOUNT_NOT_FOUND:
			return this.messages.get(Message.PLAYER_NOTFOUND);
		case INSUFFICIENT_FUNDS:
			return this.messages.get(Message.NO_MONEY);
		case RECEIVING_DISABLED:
			return this.messages.get(Message.MONEY_TOGGLED);
		case BALANCE_LIMIT_EXCEEDED:
			return this.messages.get(Message.BALANCE_LIMIT, "valor",
					this.formatter.format(this.settings.current().getMoney().getMaxBalance()));
		default:
			return this.messages.get(Message.NUMBER_NULL);
		}
	}
}
