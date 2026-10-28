package com.redeskyller.bukkit.solaryeconomy.format;

import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.redeskyller.bukkit.solaryeconomy.config.Abbreviation;
import com.redeskyller.bukkit.solaryeconomy.config.AbbreviationSettings;
import com.redeskyller.bukkit.solaryeconomy.config.PluginSettings;
import com.redeskyller.bukkit.solaryeconomy.config.SettingsProvider;

/**
 * Parses what players type after a command: {@code 1500}, {@code 1,5} or {@code 32k}.
 * <p>
 * The format is strict on purpose. Scientific notation is not accepted ({@code 1e999999999} would
 * make a {@link BigDecimal} string of a billion digits), a trailing word must be an abbreviation from
 * the dictionary (an unknown suffix is an error, not "times one") and the digit counts are bounded.
 * Both {@code .} and {@code ,} are decimal separators; thousands separators are not supported.
 */
public final class AmountParser {

	private static final Pattern AMOUNT = Pattern.compile("(\\d{1,40}(?:[.,]\\d{1,20})?)([A-Za-z]*)");

	private final SettingsProvider settings;

	public AmountParser(SettingsProvider settings) {
		this.settings = settings;
	}

	/**
	 * @return the amount scaled to the configured decimal places (extra decimals are dropped)
	 * @throws InvalidAmountException when {@code input} is not a plain non-negative amount
	 */
	public BigDecimal parse(String input) throws InvalidAmountException {
		if (input == null)
			throw new InvalidAmountException("valor ausente");

		Matcher matcher = AMOUNT.matcher(input.trim());
		if (!matcher.matches())
			throw new InvalidAmountException("formato inválido: '" + input + "'");

		PluginSettings current = this.settings.current();
		BigDecimal number = new BigDecimal(matcher.group(1).replace(',', '.'));

		String suffix = matcher.group(2);
		if (!suffix.isEmpty()) {
			AbbreviationSettings abbreviations = current.getCurrency().getAbbreviations();
			if (!abbreviations.isEnabledForCommands())
				throw new InvalidAmountException("abreviações desativadas nos comandos: '" + input + "'");
			Abbreviation abbreviation = abbreviations.findByKey(suffix);
			if (abbreviation == null)
				throw new InvalidAmountException("abreviação desconhecida '" + suffix + "'");
			number = number.multiply(abbreviation.getDivider());
		}
		return current.getMoney().credit(number);
	}
}
