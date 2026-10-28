package com.redeskyller.bukkit.solaryeconomy.command.sub;

import java.math.BigDecimal;

import com.redeskyller.bukkit.solaryeconomy.config.SettingsProvider;
import com.redeskyller.bukkit.solaryeconomy.economy.EconomyService;
import com.redeskyller.bukkit.solaryeconomy.economy.TransactionResult;
import com.redeskyller.bukkit.solaryeconomy.format.AmountParser;
import com.redeskyller.bukkit.solaryeconomy.format.CurrencyFormatter;
import com.redeskyller.bukkit.solaryeconomy.message.Message;
import com.redeskyller.bukkit.solaryeconomy.message.Messages;

public final class SetCommand extends AccountAmountCommand {

	public SetCommand(Messages messages, EconomyService economy, AmountParser parser, CurrencyFormatter formatter,
			SettingsProvider settings) {
		super(messages, economy, parser, formatter, settings, "set", "solaryeconomy.commands.set",
				Message.HELP_SET, Message.MONEY_SET, true, "definir", "setar");
	}

	@Override
	protected TransactionResult apply(String account, BigDecimal amount) {
		return this.economy.setBalance(account, amount);
	}
}
