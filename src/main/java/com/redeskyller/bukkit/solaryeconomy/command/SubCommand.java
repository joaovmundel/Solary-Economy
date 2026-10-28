package com.redeskyller.bukkit.solaryeconomy.command;

import java.util.List;

import com.redeskyller.bukkit.solaryeconomy.message.Message;

/** One word after {@code /money}: {@code pay}, {@code top}, ... */
public interface SubCommand {

	String getName();

	/** Other words that run the same command, lower case. */
	List<String> getAliases();

	/** Permission node, or an empty string when everybody may use it. */
	String getPermission();

	/** Arguments as shown to the player, e.g. {@code pay [jogador] [valor]}. */
	String getUsage();

	/** Help text of this command. */
	Message getDescription();

	void execute(CommandContext context);

	/** Tab completions for the last argument in {@code context}; empty when there is nothing to suggest. */
	List<String> complete(CommandContext context);
}
