package com.redeskyller.bukkit.solaryeconomy.economy;

import java.util.Locale;

/**
 * Account identity rules.
 * <p>
 * Minecraft 1.5.2 has no player UUID, and servers in offline mode (cracked) never have a
 * trustworthy one, so accounts are identified by name. Names are compared case-insensitively:
 * "Steve", "steve" and "STEVE" are the same account, which is how the server itself treats a login.
 */
public final class AccountNames {

	/** The legacy table used varchar(40), so longer names could never have been stored. */
	public static final int MAX_LENGTH = 40;

	private static final char SECTION_SIGN = '§';

	private AccountNames() {
	}

	/**
	 * @return the canonical lookup key for the given account name, or {@code null} when the name
	 *         cannot be an account name (blank, too long, control characters or color codes)
	 */
	public static String keyOf(String name) {
		String trimmed = clean(name);
		return trimmed == null ? null : trimmed.toLowerCase(Locale.ENGLISH);
	}

	/**
	 * @return the name as it should be displayed (trimmed, original casing), or {@code null} when invalid
	 */
	public static String displayNameOf(String name) {
		return clean(name);
	}

	public static boolean isValid(String name) {
		return clean(name) != null;
	}

	private static String clean(String name) {
		if (name == null)
			return null;

		String trimmed = name.trim();
		if (trimmed.isEmpty() || trimmed.length() > MAX_LENGTH)
			return null;

		for (int i = 0; i < trimmed.length(); i++) {
			char c = trimmed.charAt(i);
			if (Character.isISOControl(c) || c == SECTION_SIGN)
				return null;
		}
		return trimmed;
	}
}
