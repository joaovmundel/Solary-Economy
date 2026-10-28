package com.redeskyller.bukkit.solaryeconomy.format;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

import com.redeskyller.bukkit.solaryeconomy.config.Abbreviation;
import com.redeskyller.bukkit.solaryeconomy.config.AbbreviationSettings;
import com.redeskyller.bukkit.solaryeconomy.config.CurrencySettings;
import com.redeskyller.bukkit.solaryeconomy.config.PluginSettings;
import com.redeskyller.bukkit.solaryeconomy.config.SettingsProvider;

/**
 * Turns amounts into text. Stateless and thread-safe: it reads the live settings on each call, and
 * builds a fresh {@link DecimalFormat} every time because that class is not thread-safe. Display never
 * rounds up, so a balance is never shown as more than the player has.
 */
public final class CurrencyFormatter {

	private static final String[] COMPACT_SUFFIXES = { "", "K", "M", "B", "T", "Q", "QQ", "S", "SS", "OC", "N", "D",
			"UN", "DD", "TR", "QT", "QN" };

	private static final BigDecimal THOUSAND = BigDecimal.valueOf(1000);

	private final SettingsProvider settings;

	public CurrencyFormatter(SettingsProvider settings) {
		this.settings = settings;
	}

	/** For messages: abbreviated ("1.5m") when enabled, otherwise "1.500.000 coins". */
	public String format(BigDecimal amount) {
		AbbreviationSettings abbreviations = this.settings.current().getCurrency().getAbbreviations();
		return abbreviations.isEnabledForMessages() ? abbreviate(amount, abbreviations) : formatFull(amount);
	}

	/** Never abbreviated: "1.500.000 coins" (singular for -1..1). */
	public String formatFull(BigDecimal amount) {
		PluginSettings current = this.settings.current();
		CurrencySettings currency = current.getCurrency();
		int decimalPlaces = current.getMoney().getDecimalPlaces();

		// singular or plural follows the number the player actually sees, not the hidden decimals
		BigDecimal shown = amount.setScale(decimalPlaces, RoundingMode.DOWN);
		String name = shown.abs().compareTo(BigDecimal.ONE) <= 0 ? currency.getSingularName()
				: currency.getPluralName();
		return grouped(shown, decimalPlaces) + " " + name;
	}

	/** Short form for scoreboards and placeholders: "1.5m" with abbreviations, otherwise "1.5M". */
	public String formatCompact(BigDecimal amount) {
		AbbreviationSettings abbreviations = this.settings.current().getCurrency().getAbbreviations();
		return abbreviations.isEnabledForMessages() ? abbreviate(amount, abbreviations) : compact(amount);
	}

	/** Plain number without currency name, with the configured decimals ("1500.5"). */
	public String formatPlain(BigDecimal amount) {
		return amount.setScale(this.settings.current().getMoney().getDecimalPlaces(), RoundingMode.DOWN)
				.toPlainString();
	}

	private static String abbreviate(BigDecimal value, AbbreviationSettings abbreviations) {
		Abbreviation abbreviation = value.compareTo(BigDecimal.ONE) < 0 ? null : abbreviations.floor(value);
		if (abbreviation == null)
			return trimmed(value);

		BigDecimal scaled = value.divide(abbreviation.getDivider(), abbreviations.getDecimalPlaces(),
				RoundingMode.DOWN);
		return trimmed(scaled) + abbreviation.getDisplay();
	}

	private static String trimmed(BigDecimal value) {
		// stripTrailingZeros() mishandles zero before Java 8 (0.00 stays 0.00)
		return value.signum() == 0 ? "0" : value.stripTrailingZeros().toPlainString();
	}

	private static String compact(BigDecimal amount) {
		BigDecimal value = amount;
		int index = 0;
		while (value.abs().compareTo(THOUSAND) >= 0 && index < COMPACT_SUFFIXES.length - 1) {
			value = value.divide(THOUSAND);
			index++;
		}
		DecimalFormat format = new DecimalFormat("#.#", new DecimalFormatSymbols(Locale.ENGLISH));
		format.setRoundingMode(RoundingMode.DOWN);
		return format.format(value) + COMPACT_SUFFIXES[index];
	}

	/** Brazilian style: "1.234.567,89". */
	private static String grouped(BigDecimal amount, int decimalPlaces) {
		StringBuilder pattern = new StringBuilder("#,##0");
		if (decimalPlaces > 0) {
			pattern.append('.');
			for (int i = 0; i < decimalPlaces; i++)
				pattern.append('#');
		}

		DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.ENGLISH);
		symbols.setGroupingSeparator('.');
		symbols.setDecimalSeparator(',');
		DecimalFormat format = new DecimalFormat(pattern.toString(), symbols);
		format.setRoundingMode(RoundingMode.DOWN);
		return format.format(amount);
	}
}
