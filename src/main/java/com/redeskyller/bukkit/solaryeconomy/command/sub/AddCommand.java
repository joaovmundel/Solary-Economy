package com.redeskyller.bukkit.solaryeconomy.command.sub;

import java.math.BigDecimal;

import com.redeskyller.bukkit.solaryeconomy.config.SettingsProvider;
import com.redeskyller.bukkit.solaryeconomy.economy.EconomyService;
import com.redeskyller.bukkit.solaryeconomy.economy.TransactionResult;
import com.redeskyller.bukkit.solaryeconomy.format.AmountParser;
import com.redeskyller.bukkit.solaryeconomy.format.CurrencyFormatter;
import com.redeskyller.bukkit.solaryeconomy.message.Message;
import com.redeskyller.bukkit.solaryeconomy.message.Messages;

public final class AddCommand extends AccountAmountCommand {

	public AddCommand(Messages messages, EconomyService economy, AmountParser parser, CurrencyFormatter formatter,
			SettingsProvider settings) {
		super(messages, economy, parser, formatter, settings, "add", "solaryeconomy.commands.add",
				Message.HELP_ADD, Message.MONEY_ADD, false, "give");
	}

	@Override
	protected TransactionResult apply(String account, BigDecimal amount) {
		return this.economy.deposit(account, amount);
	}
}
