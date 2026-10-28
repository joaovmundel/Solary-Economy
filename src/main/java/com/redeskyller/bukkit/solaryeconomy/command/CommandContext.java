package com.redeskyller.bukkit.solaryeconomy.command;

import org.bukkit.command.CommandSender;

/** Who ran which command with which arguments. The arguments exclude the subcommand word itself. */
public final class CommandContext {

	private final CommandSender sender;
	private final String label;
	private final String[] args;

	public CommandContext(CommandSender sender, String label, String[] args) {
		this.sender = sender;
		this.label = label;
		this.args = args;
	}

	public CommandSender getSender() {
		return this.sender;
	}

	/** The command word the sender typed (money, coins, ...). */
	public String getLabel() {
		return this.label;
	}

	public int argCount() {
		return this.args.length;
	}

	public String arg(int index) {
		return this.args[index];
	}

	/** The argument being typed when tab-completing, or an empty string. */
	public String lastArg() {
		return this.args.length == 0 ? "" : this.args[this.args.length - 1];
	}
}
