package com.redeskyller.bukkit.solaryeconomy.command;

import java.util.List;

import org.bukkit.command.CommandSender;

/** The registered subcommands, for the one that lists them (help). */
public interface CommandCatalog {

	/** Every subcommand, in display order. */
	List<SubCommand> getSubCommands();

	boolean isAllowed(CommandSender sender, SubCommand command);
}
