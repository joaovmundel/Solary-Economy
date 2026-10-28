package com.redeskyller.bukkit.solaryeconomy.audit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.redeskyller.bukkit.solaryeconomy.TestSupport;
import com.redeskyller.bukkit.solaryeconomy.economy.BalanceChange;

public class AuditLogTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	private static BalanceChange deposit(String account, String amount, String before, String after) {
		return new BalanceChange(BalanceChange.Type.DEPOSIT, account, null, new BigDecimal(amount),
				new BigDecimal(before), new BigDecimal(after));
	}

	private static List<String> lines(File file) throws Exception {
		return Files.readAllLines(file.toPath(), Charset.forName("UTF-8"));
	}

	@Test
	public void writesOneLineWithTimestampAndTheChange() throws Exception {
		AuditLog audit = new AuditLog(this.folder.getRoot(), TestSupport.logger());
		audit.setEnabled(true);

		audit.onBalanceChange(deposit("José", "10", "5", "15"));
		audit.close();

		List<String> lines = lines(new File(this.folder.getRoot(), "transactions.0.log"));
		assertEquals(1, lines.size());
		assertTrue(lines.get(0), lines.get(0).matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2} \\| DEPOSIT \\| José \\| amount=10 \\| 5 -> 15"));
	}

	@Test
	public void transfersRecordBothSides() throws Exception {
		AuditLog audit = new AuditLog(this.folder.getRoot(), TestSupport.logger());
		audit.setEnabled(true);

		audit.onBalanceChange(new BalanceChange(BalanceChange.Type.TRANSFER_OUT, "Alice", "Bob", new BigDecimal("3"),
				new BigDecimal("10"), new BigDecimal("7")));
		audit.close();

		assertTrue(lines(new File(this.folder.getRoot(), "transactions.0.log")).get(0)
				.endsWith("TRANSFER_OUT | Alice | Bob | amount=3 | 10 -> 7"));
	}

	@Test
	public void writesNothingWhileDisabledAndCanBeSwitchedOnAndOff() throws Exception {
		AuditLog audit = new AuditLog(this.folder.getRoot(), TestSupport.logger());

		audit.onBalanceChange(deposit("Ignored", "1", "0", "1"));
		assertFalse(new File(this.folder.getRoot(), "transactions.0.log").exists());

		audit.setEnabled(true);
		audit.setEnabled(true); // idempotent
		audit.onBalanceChange(deposit("First", "1", "0", "1"));
		audit.setEnabled(false);
		audit.onBalanceChange(deposit("Ignored", "1", "0", "1"));
		audit.setEnabled(true);
		audit.onBalanceChange(deposit("Second", "1", "0", "1"));
		audit.close();

		List<String> lines = lines(new File(this.folder.getRoot(), "transactions.0.log"));
		assertEquals(2, lines.size());
		assertTrue(lines.get(0).contains("First"));
		assertTrue(lines.get(1).contains("Second"));
	}

	@Test
	public void aDataFolderWithAPercentSignWorks() throws Exception {
		File odd = new File(this.folder.getRoot(), "plugins 100%");
		AuditLog audit = new AuditLog(odd, TestSupport.logger());
		audit.setEnabled(true);

		audit.onBalanceChange(deposit("Steve", "1", "0", "1"));
		audit.close();

		assertEquals(1, lines(new File(odd, "transactions.0.log")).size());
	}

	@Test
	public void anUnwritableFolderIsReportedAndDoesNotBreakTheEconomy() throws Exception {
		File notAFolder = this.folder.newFile("occupied");
		TestSupport.RecordingLogger logger = TestSupport.logger();
		AuditLog audit = new AuditLog(notAFolder, logger);

		audit.setEnabled(true); // must not throw
		audit.onBalanceChange(deposit("Steve", "1", "0", "1")); // must not throw either

		assertTrue(logger.contains(java.util.logging.Level.WARNING, "log de transa"));
	}
}
