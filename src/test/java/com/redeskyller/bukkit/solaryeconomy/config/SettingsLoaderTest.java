package com.redeskyller.bukkit.solaryeconomy.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.Test;

import com.redeskyller.bukkit.solaryeconomy.TestSupport;

public class SettingsLoaderTest {

	private static PluginSettings load(Map<String, Object> overrides, TestSupport.RecordingLogger logger) {
		return new SettingsLoader(logger).load(TestSupport.config(overrides));
	}

	private static Map<String, Object> with(String path, Object value) {
		Map<String, Object> overrides = new HashMap<String, Object>();
		overrides.put(path, value);
		return overrides;
	}

	@Test
	public void theShippedConfigLoadsWithoutAnyWarning() {
		TestSupport.RecordingLogger logger = TestSupport.logger();

		PluginSettings settings = load(new HashMap<String, Object>(), logger);

		assertEquals(0, logger.count(Level.WARNING));
		assertEquals(DatabaseSettings.Type.SQLITE, settings.getDatabase().getType());
		assertEquals("solaryeconomy_accounts", settings.getDatabase().getAccountsTable());
		assertEquals(2, settings.getMoney().getDecimalPlaces());
		assertEquals(0, new BigDecimal("1000000000000000000").compareTo(settings.getMoney().getMaxBalance()));
		assertEquals(30, settings.getGeneral().getSaveIntervalSeconds());
		assertEquals(10, settings.getLeaderboard().getSize());
		assertEquals(300, settings.getLeaderboard().getRefreshSeconds());
		assertEquals("coin", settings.getCurrency().getSingularName());
		assertEquals(4, settings.getCurrency().getAbbreviations().getEntries().size());
		assertEquals("k", settings.getCurrency().getAbbreviations().getEntries().get(0).getKey());
	}

	@Test
	public void anOutOfRangeNumberFallsBackToTheDefaultWithAWarningNamingTheKey() {
		TestSupport.RecordingLogger logger = TestSupport.logger();

		PluginSettings settings = load(with("decimal_places", 99), logger);

		assertEquals(2, settings.getMoney().getDecimalPlaces());
		assertTrue(logger.contains(Level.WARNING, "decimal_places"));
	}

	@Test
	public void aWrongTypeFallsBackToTheDefault() {
		TestSupport.RecordingLogger logger = TestSupport.logger();
		Map<String, Object> overrides = with("economy_top.size", "many");
		overrides.put("use_vault", "maybe");
		overrides.put("max_balance", "lots");

		PluginSettings settings = load(overrides, logger);

		assertEquals(10, settings.getLeaderboard().getSize());
		assertTrue(settings.getGeneral().isUseVault());
		assertEquals(0, new BigDecimal("1000000000000000000").compareTo(settings.getMoney().getMaxBalance()));
		assertEquals(3, logger.count(Level.WARNING));
	}

	@Test
	public void numbersWrittenAsStringsAreAccepted() {
		PluginSettings settings = load(with("economy_top.size", "25"), TestSupport.logger());

		assertEquals(25, settings.getLeaderboard().getSize());
	}

	@Test
	public void aNegativeStartValueIsRefused() {
		TestSupport.RecordingLogger logger = TestSupport.logger();

		PluginSettings settings = load(with("start_value", -100), logger);

		assertEquals(0, BigDecimal.ZERO.compareTo(settings.getMoney().getStartBalance()));
		assertTrue(logger.contains(Level.WARNING, "start_value"));
	}

	@Test
	public void aMaximumBalanceBeyondTheDatabaseColumnIsRefused() {
		TestSupport.RecordingLogger logger = TestSupport.logger();

		PluginSettings settings = load(with("max_balance", "100000000000000000000000000000000"), logger);

		assertEquals(0, new BigDecimal("1000000000000000000").compareTo(settings.getMoney().getMaxBalance()));
		assertTrue(logger.contains(Level.WARNING, "max_balance"));
	}

	@Test
	public void aTableNameThatCouldInjectSqlIsReplacedByTheDefault() {
		TestSupport.RecordingLogger logger = TestSupport.logger();
		Map<String, Object> overrides = with("mysql.enable", true);
		overrides.put("mysql.table", "x; DROP TABLE y;--");

		PluginSettings settings = load(overrides, logger);

		assertEquals(DatabaseSettings.Type.MYSQL, settings.getDatabase().getType());
		assertEquals("solaryeconomy", settings.getDatabase().getLegacyTable());
		assertTrue(logger.contains(Level.WARNING, "mysql.table"));
	}

	@Test
	public void sqliteIgnoresTheMysqlTableName() {
		PluginSettings settings = load(with("mysql.table", "other"), TestSupport.logger());

		assertEquals("solaryeconomy", settings.getDatabase().getLegacyTable());
	}

	@Test
	public void theDatabasePasswordIsNeverPrintedAndMayBeEmpty() {
		Map<String, Object> overrides = with("mysql.enable", true);
		overrides.put("mysql.password", "s3cret!");

		PluginSettings settings = load(overrides, TestSupport.logger());

		assertFalse(settings.getDatabase().toString().contains("s3cret"));
		assertEquals("s3cret!", settings.getDatabase().getPassword());
		assertEquals("", load(with("mysql.enable", true), TestSupport.logger()).getDatabase().getPassword());
	}

	@Test
	public void databaseSettingsCompareByValueSoReloadCanDetectAChange() {
		Map<String, Object> changed = with("mysql.enable", true);
		changed.put("mysql.hostname", "db.example");

		assertEquals(load(new HashMap<String, Object>(), TestSupport.logger()).getDatabase(),
				load(new HashMap<String, Object>(), TestSupport.logger()).getDatabase());
		assertFalse(load(new HashMap<String, Object>(), TestSupport.logger()).getDatabase()
				.equals(load(changed, TestSupport.logger()).getDatabase()));
	}

	@Test
	public void aBrokenDictionaryEntryIsSkippedWithAWarning() {
		TestSupport.RecordingLogger logger = TestSupport.logger();
		Map<String, Object> overrides = with("abbreviations.dictionary.x.divider", "zero");
		overrides.put("abbreviations.dictionary.y.divider", 0);

		PluginSettings settings = load(overrides, logger);

		assertEquals(4, settings.getCurrency().getAbbreviations().getEntries().size());
		assertEquals(2, logger.count(Level.WARNING));
	}

	@Test
	public void anEmptyDictionaryFallsBackToTheShippedOne() {
		YamlConfiguration config = TestSupport.config(new HashMap<String, Object>());
		config.set("abbreviations.dictionary", null);
		config.createSection("abbreviations.dictionary");

		PluginSettings settings = new SettingsLoader(TestSupport.logger()).load(config);

		assertEquals(4, settings.getCurrency().getAbbreviations().getEntries().size());
	}

	@Test
	public void entriesAreSortedByDividerAndLookedUpByLowerCaseKey() {
		AbbreviationSettings abbreviations = load(new HashMap<String, Object>(), TestSupport.logger()).getCurrency()
				.getAbbreviations();

		assertEquals("k", abbreviations.floor(new BigDecimal("1500")).getKey());
		assertEquals("m", abbreviations.floor(new BigDecimal("2000000")).getKey());
		assertEquals(null, abbreviations.floor(new BigDecimal("999")));
		assertNotNull(abbreviations.findByKey("M"));
		assertEquals(null, abbreviations.findByKey("q"));
	}
}
