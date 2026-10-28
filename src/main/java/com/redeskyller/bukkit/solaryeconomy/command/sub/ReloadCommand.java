package com.redeskyller.bukkit.solaryeconomy.command.sub;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.command.CommandSender;

import com.redeskyller.bukkit.solaryeconomy.command.AbstractSubCommand;
import com.redeskyller.bukkit.solaryeconomy.command.CommandContext;
import com.redeskyller.bukkit.solaryeconomy.command.Reloadable;
import com.redeskyller.bukkit.solaryeconomy.config.ConfigException;
import com.redeskyller.bukkit.solaryeconomy.message.Message;
import com.redeskyller.bukkit.solaryeconomy.message.Messages;

public final class ReloadCommand extends AbstractSubCommand {

	private final Reloadable target;
	private final Logger logger;

	public ReloadCommand(Messages messages, Reloadable target, Logger logger) {
		super(messages, "reload", "reload", "solaryeconomy.commands.reload", Message.HELP_RELOAD, "recarregar");
		this.target = target;
		this.logger = logger;
	}

	@Override
	public void execute(CommandContext context) {
		CommandSender sender = context.getSender();
		try {
			this.target.reload();
			sender.sendMessage(this.messages.get(Message.RELOAD_SUCCESS));
		} catch (ConfigException exception) {
			this.logger.log(Level.WARNING, "/money reload falhou, as configurações anteriores continuam ativas.",
					exception);
			sender.sendMessage(this.messages.get(Message.RELOAD_FAILED, "erro", exception.getMessage()));
		}
	}
}
