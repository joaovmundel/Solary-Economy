package com.redeskyller.bukkit.solaryeconomy.config;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;

import com.redeskyller.bukkit.solaryeconomy.economy.AccountNames;

/**
 * Turns a loaded config.yml into {@link PluginSettings}. A value that is missing, has the wrong type
 * or is out of range is reported once and replaced by the default shipped inside the jar; the
 * plugin keeps working with a clear explanation instead of failing on a typo.
 */
public final class SettingsLoader {

	/** Free-form sections that must not be reported as "missing keys". */
	public static final String DICTIONARY_PATH = "abbreviations.dictionary";

	private static final Pattern TABLE_NAME = Pattern.compile("[A-Za-z0-9_]{1,48}");

	/** The MySQL column is DECIMAL(40,8): 32 digits before the decimal point. */
	private static final BigDecimal MAX_SUPPORTED_BALANCE = BigDecimal.TEN.pow(32).subtract(BigDecimal.ONE);

	private final Logger logger;

	public SettingsLoader(Logger logger) {
		this.logger = logger;
	}

	public PluginSettings load(Configuration config) {
		Source source = new Source(config);

		int decimalPlaces = source.integer("decimal_places", 0, MoneySettings.MAX_DECIMAL_PLACES);
		MoneySettings money = new MoneySettings(decimalPlaces,
				source.decimal("max_balance", BigDecimal.ONE, MAX_SUPPORTED_BALANCE),
				source.decimal("start_value", BigDecimal.ZERO, MAX_SUPPORTED_BALANCE),
				source.decimal("pay_minimum", BigDecimal.ZERO, MAX_SUPPORTED_BALANCE));

		GeneralSettings general = new GeneralSettings(source.text("world"), source.bool("use_vault"),
				source.integer("save_interval", 1, 3600), source.bool("audit_log"), source.bool("check_updates"),
				source.bool("debug"));

		LeaderboardSettings leaderboard = new LeaderboardSettings(source.integer("economy_top.size", 1, 100),
				source.integer("economy_top.refresh_time", 5, 86400), source.bool("economy_top.prefix"),
				source.integer("economy_top.name_size", 1, AccountNames.MAX_LENGTH));

		MagnateSettings magnate = new MagnateSettings(source.bool("magnata_tag"), source.bool("magnata_broadcast"));

		CurrencySettings currency = new CurrencySettings(source.text("currency_name.singular"),
				source.text("currency_name.plural"), loadAbbreviations(source));

		return new PluginSettings(general, loadDatabase(source), money, currency, leaderboard, magnate);
	}

	private DatabaseSettings loadDatabase(Source source) {
		if (!source.bool("mysql.enable"))
			return new DatabaseSettings(DatabaseSettings.Type.SQLITE, "", "", "", "", DatabaseSettings.DEFAULT_TABLE);

		String table = source.text("mysql.table");
		if (!TABLE_NAME.matcher(table).matches()) {
			this.logger.warning("config.yml: 'mysql.table' = '" + table
					+ "' é inválido (use apenas letras, números e _, até 48 caracteres); usando '"
					+ DatabaseSettings.DEFAULT_TABLE + "'.");
			table = DatabaseSettings.DEFAULT_TABLE;
		}
		return new DatabaseSettings(DatabaseSettings.Type.MYSQL, source.text("mysql.hostname"),
				source.text("mysql.database"), source.text("mysql.username"), source.rawText("mysql.password"), table);
	}

	private AbbreviationSettings loadAbbreviations(Source source) {
		int decimalPlaces = source.integer("abbreviations.decimal_places", 0, MoneySettings.MAX_DECIMAL_PLACES);
		ConfigurationSection dictionary = source.section(DICTIONARY_PATH);

		List<Abbreviation> entries = new ArrayList<Abbreviation>();
		if (dictionary != null) {
			for (String key : dictionary.getKeys(false)) {
				Abbreviation entry = parseAbbreviation(key, dictionary.getConfigurationSection(key));
				if (entry != null)
					entries.add(entry);
			}
		}
		return new AbbreviationSettings(source.bool("abbreviations.enable.messages"),
				source.bool("abbreviations.enable.commands"), decimalPlaces, entries);
	}

	private Abbreviation parseAbbreviation(String key, ConfigurationSection section) {
		String where = "config.yml: '" + DICTIONARY_PATH + "." + key + "'";
		if (section == null) {
			this.logger.warning(where + " deveria ter 'display' e 'divider'; ignorado.");
			return null;
		}

		BigDecimal divider = parseDecimal(section.get("divider"));
		if (divider == null || divider.signum() <= 0) {
			this.logger.warning(where + ": 'divider' precisa ser um número maior que zero; ignorado.");
			return null;
		}

		String display = section.getString("display");
		if (display == null || display.trim().isEmpty())
			display = key;
		return new Abbreviation(key.toLowerCase(Locale.ENGLISH), display, divider);
	}

	private static BigDecimal parseDecimal(Object raw) {
		if (raw == null)
			return null;
		try {
			return new BigDecimal(raw.toString().trim());
		} catch (NumberFormatException exception) {
			return null;
		}
	}

	private static Integer parseInteger(Object raw) {
		if (raw instanceof Number) {
			Number number = (Number) raw;
			return number.doubleValue() == number.intValue() ? Integer.valueOf(number.intValue()) : null;
		}
		if (raw instanceof String) {
			try {
				return Integer.valueOf(((String) raw).trim());
			} catch (NumberFormatException exception) {
				return null;
			}
		}
		return null;
	}

	private static Boolean parseBoolean(Object raw) {
		if (raw instanceof Boolean)
			return (Boolean) raw;
		if (raw instanceof String) {
			String text = ((String) raw).trim().toLowerCase(Locale.ENGLISH);
			if (text.equals("true"))
				return Boolean.TRUE;
			if (text.equals("false"))
				return Boolean.FALSE;
		}
		return null;
	}

	/** Reads values, falling back to the bundled default (and saying so) when the admin's value is unusable. */
	private final class Source {

		private final Configuration config;
		private final Configuration defaults;

		Source(Configuration config) {
			this.config = config;
			this.defaults = config.getDefaults() != null ? config.getDefaults() : config;
		}

		int integer(String path, int min, int max) {
			Object raw = this.config.get(path);
			Integer value = parseInteger(raw);
			if (value == null)
				return invalid(path, raw, "precisa ser um número inteiro", this.defaults.getInt(path));
			if (value < min || value > max)
				return invalid(path, raw, "precisa estar entre " + min + " e " + max, this.defaults.getInt(path));
			return value;
		}

		boolean bool(String path) {
			Object raw = this.config.get(path);
			Boolean value = parseBoolean(raw);
			if (value == null)
				return invalid(path, raw, "precisa ser true ou false", this.defaults.getBoolean(path));
			return value;
		}

		BigDecimal decimal(String path, BigDecimal min, BigDecimal max) {
			Object raw = this.config.get(path);
			BigDecimal value = parseDecimal(raw);
			if (value == null)
				return invalid(path, raw, "precisa ser um número", parseDecimal(this.defaults.get(path)));
			if (value.compareTo(min) < 0 || value.compareTo(max) > 0)
				return invalid(path, raw, "precisa estar entre " + min.toPlainString() + " e " + max.toPlainString(),
						parseDecimal(this.defaults.get(path)));
			return value;
		}

		/** A non-blank string. */
		String text(String path) {
			String value = this.config.getString(path);
			if (value == null || value.trim().isEmpty())
				return invalid(path, value, "não pode ficar vazio", this.defaults.getString(path));
			return value.trim();
		}

		/** A string where empty is fine (a database password). */
		String rawText(String path) {
			String value = this.config.getString(path);
			return value == null ? "" : value;
		}

		ConfigurationSection section(String path) {
			ConfigurationSection section = this.config.getConfigurationSection(path);
			if (section != null && !section.getKeys(false).isEmpty())
				return section;
			return this.defaults.getConfigurationSection(path);
		}

		private <T> T invalid(String path, Object raw, String reason, T fallback) {
			SettingsLoader.this.logger.warning("config.yml: '" + path + "' = '" + raw + "' é inválido (" + reason
					+ "); usando o padrão: " + fallback);
			return fallback;
		}
	}
}
