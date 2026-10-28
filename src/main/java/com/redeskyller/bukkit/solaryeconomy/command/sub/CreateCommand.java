package com.redeskyller.bukkit.solaryeconomy.command.sub;

import java.math.BigDecimal;

import org.bukkit.command.CommandSender;

import com.redeskyller.bukkit.solaryeconomy.command.AbstractSubCommand;
import com.redeskyller.bukkit.solaryeconomy.command.CommandContext;
import com.redeskyller.bukkit.solaryeconomy.economy.CreateResult;
import com.redeskyller.bukkit.solaryeconomy.economy.EconomyService;
import com.redeskyller.bukkit.solaryeconomy.format.AmountParser;
import com.redeskyller.bukkit.solaryeconomy.format.InvalidAmountException;
import com.redeskyller.bukkit.solaryeconomy.message.Message;
import com.redeskyller.bukkit.solaryeconomy.message.Messages;

public final class CreateCommand extends AbstractSubCommand {

	private final EconomyService economy;
	private final AmountParser parser;

	public CreateCommand(Messages messages, EconomyService economy, AmountParser parser) {
		super(messages, "criar", "criar [nome] [valor]", "solaryeconomy.commands.criar", Message.HELP_CREATE,
				"create", "new");
		this.economy = economy;
		this.parser = parser;
	}

	@Override
	public void execute(CommandContext context) {
		CommandSender sender = context.getSender();
		if (context.argCount() < 2) {
			sendUsage(context);
			return;
		}

		String name = context.arg(0);
		BigDecimal amount;
		try {
			amount = this.parser.parse(context.arg(1));
		} catch (InvalidAmountException exception) {
			sender.sendMessage(this.messages.get(Message.NUMBER_NULL));
			return;
		}

		CreateResult result = this.economy.createAccount(name, amount);
		switch (result) {
		case CREATED:
			sender.sendMessage(this.messages.get(Message.ACCOUNT_CREATE, "nome", name));
			break;
		case ALREADY_EXISTS:
			sender.sendMessage(this.messages.get(Message.ACCOUNT_EXISTS, "nome", name));
			break;
		case INVALID_NAME:
			sender.sendMessage(this.messages.get(Message.INVALID_ACCOUNT_NAME));
			break;
		default:
			sender.sendMessage(this.messages.get(Message.NUMBER_NULL));
			break;
		}
	}
}
