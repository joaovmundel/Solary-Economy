package com.redeskyller.bukkit.solaryeconomy.storage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;

import org.junit.Test;

import com.redeskyller.bukkit.solaryeconomy.TestSupport;
import com.redeskyller.bukkit.solaryeconomy.economy.AccountSnapshot;

public class LegacyMigrationTest {

	private final TestSupport.RecordingLogger logger = TestSupport.logger();

	private List<AccountSnapshot> convert(LegacyAccount... rows) {
		return LegacyMigration.convert(Arrays.asList(rows), TestSupport.settings().getMoney(), this.logger);
	}

	private static AccountSnapshot find(List<AccountSnapshot> accounts, String key) {
		for (AccountSnapshot account : accounts)
			if (account.getKey().equals(key))
				return account;
		return null;
	}

	@Test
	public void convertsNamesToKeysAndKeepsTheToggleFlag() {
		List<AccountSnapshot> result = convert(new LegacyAccount("Steve", "1500", false),
				new LegacyAccount("Notch", "20.5", true));

		assertEquals(2, result.size());
		assertEquals("Steve", find(result, "steve").getDisplayName());
		assertTrue(find(result, "notch").isReceivingDisabled());
		assertEquals(0, new BigDecimal("20.5").compareTo(find(result, "notch").getBalance()));
		assertEquals(0, this.logger.count(Level.WARNING));
	}

	@Test
	public void accountsThatDifferOnlyByCaseAreMergedKeepingTheRicherOneAndSayingSo() {
		List<AccountSnapshot> result = convert(new LegacyAccount("Steve", "900", false),
				new LegacyAccount("steve", "1500", false), new LegacyAccount("STEVE", "10", false));

		assertEquals(1, result.size());
		assertEquals(0, new BigDecimal("1500").compareTo(result.get(0).getBalance()));
		assertEquals("steve", result.get(0).getDisplayName());
		assertEquals(2, this.logger.count(Level.WARNING));
		assertTrue(this.logger.contains(Level.WARNING, "mesma conta"));
	}

	@Test
	public void unreadableBalancesAndInvalidNamesAreSkippedWithAWarningEach() {
		List<AccountSnapshot> result = convert(new LegacyAccount("Bad", "abc", false),
				new LegacyAccount("Null", null, false), new LegacyAccount("", "5", false),
				new LegacyAccount(null, "5", false), new LegacyAccount("Fine", "5", false));

		assertEquals(1, result.size());
		assertEquals("fine", result.get(0).getKey());
		assertEquals(4, this.logger.count(Level.WARNING));
	}

	@Test
	public void floatingPointGarbageFromTheOldBigDecimalDoubleConstructorIsTrimmed() {
		List<AccountSnapshot> result = convert(
				new LegacyAccount("Notch", "0.1000000000000000055511151231257827021181583404541015625", false));

		assertEquals(0, new BigDecimal("0.10").compareTo(result.get(0).getBalance()));
		assertEquals(2, result.get(0).getBalance().scale());
	}

	@Test
	public void balancesAboveTheMaximumAreClampedWithAWarning() {
		List<AccountSnapshot> result = convert(new LegacyAccount("Rich", "99999999999999999999999999999999999999", false));

		assertEquals(0, new BigDecimal("1000000000000000000").compareTo(result.get(0).getBalance()));
		assertTrue(this.logger.contains(Level.WARNING, "excede"));
	}

	@Test
	public void negativeBalancesAreKeptButReportedOnce() {
		List<AccountSnapshot> result = convert(new LegacyAccount("A", "-5", false), new LegacyAccount("B", "-9", false),
				new LegacyAccount("C", "3", false));

		assertEquals(3, result.size());
		assertEquals(0, new BigDecimal("-5").compareTo(find(result, "a").getBalance()));
		assertEquals(1, this.logger.count(Level.WARNING));
		assertTrue(this.logger.contains(Level.WARNING, "2 conta(s) com saldo negativo"));
	}

	@Test
	public void namesWithSqlInTheMiddleAreJustNames() {
		List<AccountSnapshot> result = convert(new LegacyAccount("evil'; DROP TABLE x;--", "5", false));

		assertNotNull(find(result, "evil'; drop table x;--"));
		assertNull(find(result, "x"));
	}
}
