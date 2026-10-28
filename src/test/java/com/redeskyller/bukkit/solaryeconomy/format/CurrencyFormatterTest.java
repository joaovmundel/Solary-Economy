package com.redeskyller.bukkit.solaryeconomy.format;

import static org.junit.Assert.assertEquals;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.redeskyller.bukkit.solaryeconomy.TestSupport;

public class CurrencyFormatterTest {

	private static CurrencyFormatter withAbbreviations(boolean enabled) {
		Map<String, Object> overrides = new HashMap<String, Object>();
		overrides.put("abbreviations.enable.messages", enabled);
		return new CurrencyFormatter(TestSupport.holder(overrides));
	}

	private static BigDecimal n(String value) {
		return new BigDecimal(value);
	}

	@Test
	public void abbreviatesWithTheConfiguredDecimalPlaces() {
		CurrencyFormatter formatter = withAbbreviations(true);

		assertEquals("4k", formatter.format(n("4000.00")));
		assertEquals("1.2k", formatter.format(n("1234.5"))); // abbreviations.decimal_places is 1; it was hard-coded to 2
		assertEquals("1.5m", formatter.format(n("1500000")));
		assertEquals("2.5b", formatter.format(n("2500000000")));
		assertEquals("1000000t", formatter.format(n("1000000000000000000")));
	}

	@Test
	public void abbreviationNeverRoundsUp() {
		assertEquals("999.9k", withAbbreviations(true).format(n("999999")));
	}

	@Test
	public void smallAndZeroValuesStayPlain() {
		CurrencyFormatter formatter = withAbbreviations(true);

		assertEquals("999", formatter.format(n("999")));
		assertEquals("0.5", formatter.format(n("0.50")));
		assertEquals("0", formatter.format(n("0.00")));
		assertEquals("-50", formatter.format(n("-50.00")));
	}

	@Test
	public void fullFormatUsesBrazilianGroupingAndSingularOrPluralName() {
		CurrencyFormatter formatter = withAbbreviations(false);

		assertEquals("1.234.567,89 coins", formatter.format(n("1234567.89")));
		assertEquals("1 coin", formatter.format(n("1")));
		assertEquals("0 coin", formatter.format(n("0")));
		assertEquals("0,5 coin", formatter.format(n("0.5")));
		assertEquals("2 coins", formatter.format(n("2")));
		assertEquals("-5 coins", formatter.format(n("-5")));
		assertEquals("1.000 coins", formatter.format(n("1000.00")));
	}

	@Test
	public void fullFormatTruncatesInsteadOfRounding() {
		Map<String, Object> overrides = new HashMap<String, Object>();
		overrides.put("abbreviations.enable.messages", false);
		overrides.put("decimal_places", 0);
		CurrencyFormatter formatter = new CurrencyFormatter(TestSupport.holder(overrides));

		assertEquals("1 coin", formatter.format(n("1.99")));
	}

	@Test
	public void formatFullIgnoresAbbreviations() {
		assertEquals("1.500 coins", withAbbreviations(true).formatFull(n("1500")));
	}

	@Test
	public void compactFormatFallsBackToLetterSuffixes() {
		CurrencyFormatter formatter = withAbbreviations(false);

		assertEquals("999", formatter.formatCompact(n("999")));
		assertEquals("1.5K", formatter.formatCompact(n("1500")));
		assertEquals("1M", formatter.formatCompact(n("1000000")));
		assertEquals("2.5B", formatter.formatCompact(n("2500000000")));
	}

	@Test
	public void plainFormatKeepsTheConfiguredDecimals() {
		assertEquals("1500.50", withAbbreviations(true).formatPlain(n("1500.5")));
	}
}
