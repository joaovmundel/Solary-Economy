package com.redeskyller.bukkit.solaryeconomy.command.sub;

import java.util.List;

import com.redeskyller.bukkit.solaryeconomy.command.AbstractSubCommand;
import com.redeskyller.bukkit.solaryeconomy.command.CommandContext;
import com.redeskyller.bukkit.solaryeconomy.economy.EconomyService;
import com.redeskyller.bukkit.solaryeconomy.message.Message;
import com.redeskyller.bukkit.solaryeconomy.message.Messages;

public final class DeleteCommand extends AbstractSubCommand {

	private final EconomyService economy;

	public DeleteCommand(Messages messages, EconomyService economy) {
		super(messages, "deletar", "deletar [nome]", "solaryeconomy.commands.deletar", Message.HELP_DELETE, "delete",
				"del");
		this.economy = economy;
	}

	@Override
	public void execute(CommandContext context) {
		if (context.argCount() < 1) {
			sendUsage(context);
			return;
		}

		String name = context.arg(0);
		Message result = this.economy.deleteAccount(name) ? Message.ACCOUNT_DELETE : Message.ACCOUNT_NOFOUND;
		context.getSender().sendMessage(this.messages.get(result, "nome", name));
	}

	@Override
	public List<String> complete(CommandContext context) {
		return completePlayerAtFirstArgument(context);
	}
}
