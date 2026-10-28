package com.redeskyller.bukkit.solaryeconomy.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import com.redeskyller.bukkit.solaryeconomy.economy.EconomyService;
import com.redeskyller.bukkit.solaryeconomy.format.CurrencyFormatter;
import com.redeskyller.bukkit.solaryeconomy.message.Message;
import com.redeskyller.bukkit.solaryeconomy.message.Messages;

/**
 * Entry point of {@code /money}: finds the subcommand, checks its permission and runs it. Anything
 * that is not a subcommand is a player name whose balance is shown. A failure inside a command is
 * logged with the full command line and answered with a friendly message, instead of leaving the
 * player with Bukkit's generic "internal error" and the admin with a bare stack trace.
 */
public final class MoneyCommand implements CommandExecutor, TabCompleter, CommandCatalog {

	private final List<SubCommand> subCommands = new ArrayList<SubCommand>();
	private final EconomyService economy;
	private final Messages messages;
	private final BalanceView balanceView;
	private final Logger logger;

	public MoneyCommand(EconomyService economy, CurrencyFormatter formatter, Messages messages, Logger logger) {
		this.economy = economy;
		this.messages = messages;
		this.logger = logger;
		this.balanceView = new BalanceView(economy, formatter, messages);
	}

	/** Registers a subcommand; they are listed by {@code help} in registration order. */
	public MoneyCommand register(SubCommand command) {
		this.subCommands.add(command);
		return this;
	}

	@Override
	public List<SubCommand> getSubCommands() {
		return Collections.unmodifiableList(this.subCommands);
	}

	@Override
	public boolean isAllowed(CommandSender sender, SubCommand command) {
		return command.getPermission().isEmpty() || sender.hasPermission(command.getPermission());
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		try {
			dispatch(sender, label, args);
		} catch (RuntimeException exception) {
			this.logger.log(Level.SEVERE, "Erro ao executar '/" + label + " " + join(args) + "' por "
					+ sender.getName(), exception);
			sender.sendMessage(this.messages.get(Message.INTERNAL_ERROR));
		}
		return true;
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		if (args.length == 0)
			return Collections.emptyList();

		if (args.length == 1) {
			String typed = args[0].toLowerCase(Locale.ENGLISH);
			List<String> matches = new ArrayList<String>();
			for (SubCommand sub : this.subCommands)
				if (isAllowed(sender, sub) && sub.getName().startsWith(typed))
					matches.add(sub.getName());
			return matches;
		}

		SubCommand sub = find(args[0]);
		if (sub == null || !isAllowed(sender, sub))
			return Collections.emptyList();
		return sub.complete(new CommandContext(sender, alias, tail(args)));
	}

	private void dispatch(CommandSender sender, String label, String[] args) {
		// accounts are normally created on join; this covers a player who was online before the plugin started
		if (sender instanceof Player && !this.economy.hasAccount(sender.getName()))
			this.economy.createAccount(sender.getName());

		if (args.length == 0) {
			this.balanceView.showOwn(sender, label);
			return;
		}

		SubCommand sub = find(args[0]);
		if (sub == null) {
			this.balanceView.showOther(sender, label, args[0]);
			return;
		}
		if (!isAllowed(sender, sub)) {
			sender.sendMessage(this.messages.get(Message.NO_PERMISSION));
			return;
		}
		sub.execute(new CommandContext(sender, label, tail(args)));
	}

	private SubCommand find(String word) {
		String wanted = word.toLowerCase(Locale.ENGLISH);
		for (SubCommand sub : this.subCommands)
			if (sub.getName().equals(wanted) || sub.getAliases().contains(wanted))
				return sub;
		return null;
	}

	private static String[] tail(String[] args) {
		String[] rest = new String[args.length - 1];
		System.arraycopy(args, 1, rest, 0, rest.length);
		return rest;
	}

	private static String join(String[] args) {
		StringBuilder text = new StringBuilder();
		for (String arg : args)
			text.append(text.length() == 0 ? "" : " ").append(arg);
		return text.toString();
	}
}
