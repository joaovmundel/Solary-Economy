package com.redeskyller.bukkit.solaryeconomy.storage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.redeskyller.bukkit.solaryeconomy.economy.AccountSnapshot;

public class EmergencyDumpTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	@Test
	public void writesEveryUnsavedBalanceAsRecoverableCsv() throws Exception {
		File directory = new File(this.folder.getRoot(), "plugin-data"); // does not exist yet
		File dump = EmergencyDump.write(directory, Arrays.asList(
				new AccountSnapshot("alice", "Alice", new BigDecimal("1234.50"), false),
				new AccountSnapshot("josé;\"x", "José;\"X", new BigDecimal("7"), true)));

		List<String> lines = Files.readAllLines(dump.toPath(), Charset.forName("UTF-8"));

		assertTrue(dump.getName().startsWith("unsaved-accounts-") && dump.getName().endsWith(".csv"));
		assertEquals("account_key;display_name;balance;receiving_disabled", lines.get(0));
		assertEquals("\"alice\";\"Alice\";1234.50;0", lines.get(1));
		assertEquals("\"josé;\"\"x\";\"José;\"\"X\";7;1", lines.get(2));
	}
}
