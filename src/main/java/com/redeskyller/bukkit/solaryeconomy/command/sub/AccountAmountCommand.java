package com.redeskyller.bukkit.solaryeconomy.command.sub;

import java.math.BigDecimal;
import java.util.List;

import org.bukkit.command.CommandSender;

import com.redeskyller.bukkit.solaryeconomy.command.AbstractSubCommand;
import com.redeskyller.bukkit.solaryeconomy.command.CommandContext;
import com.redeskyller.bukkit.solaryeconomy.config.SettingsProvider;
import com.redeskyller.bukkit.solaryeconomy.economy.AccountSnapshot;
import com.redeskyller.bukkit.solaryeconomy.economy.EconomyService;
import com.redeskyller.bukkit.solaryeconomy.economy.TransactionResult;
import com.redeskyller.bukkit.solaryeconomy.format.AmountParser;
import com.redeskyller.bukkit.solaryeconomy.format.CurrencyFormatter;
import com.redeskyller.bukkit.solaryeconomy.format.InvalidAmountException;
import com.redeskyller.bukkit.solaryeconomy.message.Message;
import com.redeskyller.bukkit.solaryeconomy.message.Messages;

/**
 * Template for the admin commands shaped {@code <command> [account] [amount]} (add, remove, set):
 * argument parsing and error reporting live here, subclasses only say what operation to run.
 */
abstract class AccountAmountCommand extends AbstractSubCommand {

	protected final EconomyService economy;
	private final AmountParser parser;
	private final CurrencyFormatter formatter;
	private final SettingsProvider settings;
	private final Message successMessage;
	private final boolean zeroAllowed;

	protected AccountAmountCommand(Messages messages, EconomyService economy, AmountParser parser,
			CurrencyFormatter formatter, SettingsProvider settings, String name, String permission,
			Message description, Message successMessage, boolean zeroAllowed, String... aliases) {
		super(messages, name, name + " [jogador] [valor]", permission, description, aliases);
		this.economy = economy;
		this.parser = parser;
		this.formatter = formatter;
		this.settings = settings;
		this.successMessage = successMessage;
		this.zeroAllowed = zeroAllowed;
	}

	/** Runs the operation this command stands for. */
	protected abstract TransactionResult apply(String account, BigDecimal amount);

	@Override
	public final void execute(CommandContext context) {
		CommandSender sender = context.getSender();
		if (context.argCount() < 2) {
			sendUsage(context);
			return;
		}

		String account = context.arg(0);
		BigDecimal amount;
		try {
			amount = this.parser.parse(context.arg(1));
		} catch (InvalidAmountException exception) {
			sender.sendMessage(this.messages.get(Message.NUMBER_NULL));
			return;
		}
		if (amount.signum() == 0 && !this.zeroAllowed) {
			sender.sendMessage(this.messages.get(Message.NUMBER_NULL));
			return;
		}

		TransactionResult result = apply(account, amount);
		if (result.isSuccess())
			sender.sendMessage(this.messages.get(this.successMessage, "player", displayName(account), "valor",
					this.formatter.format(amount)));
		else
			sender.sendMessage(failureMessage(result));
	}

	@Override
	public List<String> complete(CommandContext context) {
		return completePlayerAtFirstArgument(context);
	}

	private String failureMessage(TransactionResult result) {
		switch (result) {
		case ACCOUNT_NOT_FOUND:
			return this.messages.get(Message.PLAYER_NOTFOUND);
		case INSUFFICIENT_FUNDS:
			return this.messages.get(Message.TARGET_NO_MONEY);
		case BALANCE_LIMIT_EXCEEDED:
			return this.messages.get(Message.BALANCE_LIMIT, "valor",
					this.formatter.format(this.settings.current().getMoney().getMaxBalance()));
		default:
			return this.messages.get(Message.NUMBER_NULL);
		}
	}

	private String displayName(String account) {
		AccountSnapshot snapshot = this.economy.getAccount(account);
		return snapshot == null ? account : snapshot.getDisplayName();
	}
}
