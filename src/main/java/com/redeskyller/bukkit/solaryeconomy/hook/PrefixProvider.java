package com.redeskyller.bukkit.solaryeconomy.hook;

/** Source of chat prefixes shown before names in the money top ("[VIP] Steve"). */
public interface PrefixProvider {

	/** @return the prefix with Minecraft color codes already applied, or an empty string; never {@code null} */
	String getPrefix(String accountName);
}
