package com.redeskyller.bukkit.solaryeconomy.command.sub;

import org.bukkit.command.CommandSender;

import com.redeskyller.bukkit.solaryeconomy.command.AbstractSubCommand;
import com.redeskyller.bukkit.solaryeconomy.command.CommandCatalog;
import com.redeskyller.bukkit.solaryeconomy.command.CommandContext;
import com.redeskyller.bukkit.solaryeconomy.command.Permissions;
import com.redeskyller.bukkit.solaryeconomy.command.SubCommand;
import com.redeskyller.bukkit.solaryeconomy.message.Message;
import com.redeskyller.bukkit.solaryeconomy.message.Messages;

/** Lists the commands the sender is allowed to use. */
public final class HelpCommand extends AbstractSubCommand {

	private final CommandCatalog catalog;

	public HelpCommand(Messages messages, CommandCatalog catalog) {
		super(messages, "help", "ajuda", "", Message.HELP_HELP, "ajuda", "?");
		this.catalog = catalog;
	}

	@Override
	public void execute(CommandContext context) {
		CommandSender sender = context.getSender();
		String label = context.getLabel();

		sender.sendMessage(" ");
		line(sender, label, "", Message.HELP_BALANCE);
		if (sender.hasPermission(Permissions.MONEY_OTHER))
			line(sender, label, "[jogador]", Message.HELP_BALANCE_OTHER);
		for (SubCommand command : this.catalog.getSubCommands())
			if (this.catalog.isAllowed(sender, command))
				line(sender, label, command.getUsage(), command.getDescription());
		sender.sendMessage(" ");
	}

	private void line(CommandSender sender, String label, String usage, Message description) {
		String line = this.messages.get(Message.HELP_LINE, "label", label, "uso", usage, "descricao",
				this.messages.get(description));
		// the plain "/money" line has an empty {uso}, which would leave two spaces behind
		sender.sendMessage(line.replace("  ", " "));
	}
}
