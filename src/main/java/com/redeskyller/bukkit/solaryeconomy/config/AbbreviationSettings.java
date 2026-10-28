package com.redeskyller.bukkit.solaryeconomy.config;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Value abbreviation ("1.5k") configuration. */
public final class AbbreviationSettings {

	private final boolean enabledForMessages;
	private final boolean enabledForCommands;
	private final int decimalPlaces;
	private final List<Abbreviation> entries;

	public AbbreviationSettings(boolean enabledForMessages, boolean enabledForCommands, int decimalPlaces,
			List<Abbreviation> entries) {
		this.enabledForMessages = enabledForMessages;
		this.enabledForCommands = enabledForCommands;
		this.decimalPlaces = decimalPlaces;

		List<Abbreviation> sorted = new ArrayList<Abbreviation>(entries);
		Collections.sort(sorted, new Comparator<Abbreviation>() {
			@Override
			public int compare(Abbreviation a, Abbreviation b) {
				return a.getDivider().compareTo(b.getDivider());
			}
		});
		this.entries = Collections.unmodifiableList(sorted);
	}

	public boolean isEnabledForMessages() {
		return this.enabledForMessages && !this.entries.isEmpty();
	}

	public boolean isEnabledForCommands() {
		return this.enabledForCommands && !this.entries.isEmpty();
	}

	public int getDecimalPlaces() {
		return this.decimalPlaces;
	}

	public List<Abbreviation> getEntries() {
		return this.entries;
	}

	/** @return the entry with the largest divider that is not greater than {@code value}, or {@code null} */
	public Abbreviation floor(BigDecimal value) {
		Abbreviation found = null;
		for (Abbreviation entry : this.entries) {
			if (entry.getDivider().compareTo(value) > 0)
				break;
			found = entry;
		}
		return found;
	}

	/** @return the entry typed as {@code token} in a command (case-insensitive), or {@code null} */
	public Abbreviation findByKey(String token) {
		String key = token.toLowerCase(Locale.ENGLISH);
		for (Abbreviation entry : this.entries)
			if (entry.getKey().equals(key))
				return entry;
		return null;
	}
}
