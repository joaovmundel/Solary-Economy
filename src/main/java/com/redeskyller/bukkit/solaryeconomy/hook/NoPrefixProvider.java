package com.redeskyller.bukkit.solaryeconomy.hook;

/** Used when there is no chat plugin behind Vault. */
public final class NoPrefixProvider implements PrefixProvider {

	public static final NoPrefixProvider INSTANCE = new NoPrefixProvider();

	private NoPrefixProvider() {
	}

	@Override
	public String getPrefix(String accountName) {
		return "";
	}
}
