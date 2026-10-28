package com.redeskyller.bukkit.solaryeconomy.command.sub;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.redeskyller.bukkit.solaryeconomy.command.AbstractSubCommand;
import com.redeskyller.bukkit.solaryeconomy.command.CommandContext;
import com.redeskyller.bukkit.solaryeconomy.economy.EconomyService;
import com.redeskyller.bukkit.solaryeconomy.message.Message;
import com.redeskyller.bukkit.solaryeconomy.message.Messages;

public final class ToggleCommand extends AbstractSubCommand {

	private final EconomyService economy;

	public ToggleCommand(Messages messages, EconomyService economy) {
		super(messages, "toggle", "toggle", "solaryeconomy.commands.toggle", Message.HELP_TOGGLE);
		this.economy = economy;
	}

	@Override
	public void execute(CommandContext context) {
		CommandSender sender = context.getSender();
		if (!(sender instanceof Player)) {
			sender.sendMessage(this.messages.get(Message.PLAYER_ONLY));
			return;
		}

		boolean disable = !this.economy.isReceivingDisabled(sender.getName());
		if (!this.economy.setReceivingDisabled(sender.getName(), disable).isSuccess()) {
			sender.sendMessage(this.messages.get(Message.PLAYER_NOTFOUND));
			return;
		}
		sender.sendMessage(this.messages.get(Message.MONEY_TOGGLE, "toggle", disable ? "OFF" : "ON"));
	}
}
