package com.redeskyller.bukkit.solaryeconomy.command;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.redeskyller.bukkit.solaryeconomy.message.Message;
import com.redeskyller.bukkit.solaryeconomy.message.Messages;

/** Shared plumbing of the subcommands: identity, usage message and "no completions" default. */
public abstract class AbstractSubCommand implements SubCommand {

	private final String name;
	private final String usage;
	private final String permission;
	private final Message description;
	private final List<String> aliases;
	protected final Messages messages;

	protected AbstractSubCommand(Messages messages, String name, String usage, String permission,
			Message description, String... aliases) {
		this.messages = messages;
		this.name = name;
		this.usage = usage;
		this.permission = permission;
		this.description = description;
		this.aliases = Collections.unmodifiableList(Arrays.asList(aliases));
	}

	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public List<String> getAliases() {
		return this.aliases;
	}

	@Override
	public String getPermission() {
		return this.permission;
	}

	@Override
	public String getUsage() {
		return this.usage;
	}

	@Override
	public Message getDescription() {
		return this.description;
	}

	@Override
	public List<String> complete(CommandContext context) {
		return Collections.emptyList();
	}

	protected void sendUsage(CommandContext context) {
		context.getSender().sendMessage(this.messages.get(Message.USAGE, "label", context.getLabel(), "uso", this.usage));
	}

	/** Online player names for the first argument, which most subcommands take. */
	protected List<String> completePlayerAtFirstArgument(CommandContext context) {
		return context.argCount() == 1 ? PlayerNames.online(context.lastArg()) : Collections.<String>emptyList();
	}
}
