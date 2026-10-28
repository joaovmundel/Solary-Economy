package com.redeskyller.bukkit.solaryeconomy.storage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.redeskyller.bukkit.solaryeconomy.TestSupport;
import com.redeskyller.bukkit.solaryeconomy.economy.AccountSnapshot;

public class StorageInitializerTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	private SqliteFixture db;

	@Before
	public void setUp() {
		this.db = new SqliteFixture(this.folder.getRoot());
	}

	@After
	public void tearDown() {
		this.db.close();
	}

	private List<AccountSnapshot> start() throws StorageException {
		JdbcAccountRepository repository = this.db.repository();
		LegacyTableSource legacy = new LegacyTableSource(this.db.connections, "solaryeconomy");
		return new StorageInitializer(repository, legacy, TestSupport.settings().getMoney(), this.db.log())
				.initializeAndLoad();
	}

	@Test
	public void aFreshInstallStartsEmptyWithoutLookingForOldData() throws Exception {
		assertTrue(start().isEmpty());
	}

	@Test
	public void theOldTableIsImportedOnTheFirstStartAndLeftUntouched() throws Exception {
		this.db.createLegacyTable();
		this.db.insertLegacy("Steve", "1500", 0);
		this.db.insertLegacy("Alice", "250.75", 1);

		List<AccountSnapshot> accounts = start();

		assertEquals(2, accounts.size());
		assertEquals(2, this.db.countRows("solaryeconomy"));
		assertEquals(2, this.db.countRows("solaryeconomy_accounts"));
	}

	@Test
	public void theImportRunsOnlyOnce() throws Exception {
		this.db.createLegacyTable();
		this.db.insertLegacy("Steve", "1500", 0);
		start();

		// the plugin ran for a while: Steve spent money, and a new player appeared
		this.db.execute("UPDATE solaryeconomy_accounts SET balance = '3.00' WHERE account_key = 'steve'");
		this.db.execute("INSERT INTO solaryeconomy_accounts VALUES ('newbie', 'Newbie', '1.00', 0, 0)");

		List<AccountSnapshot> accounts = start();

		assertEquals(2, accounts.size());
		for (AccountSnapshot account : accounts)
			if (account.getKey().equals("steve"))
				assertEquals(0, new BigDecimal("3").compareTo(account.getBalance()));
	}

	@Test
	public void anEmptyOldTableImportsNothing() throws Exception {
		this.db.createLegacyTable();

		assertTrue(start().isEmpty());
	}

	@Test
	public void aFailedImportIsRetriedOnTheNextStart() throws Exception {
		this.db.createLegacyTable();
		this.db.insertLegacy("Steve", "1500", 0);

		// first start: the repository cannot write (simulated by a repository that always fails)
		AccountRepository failing = new AccountRepository() {
			private final AccountRepository real = StorageInitializerTest.this.db.repository();

			@Override
			public void initialize() throws StorageException {
				this.real.initialize();
			}

			@Override
			public long count() throws StorageException {
				return this.real.count();
			}

			@Override
			public List<AccountSnapshot> loadAll() throws StorageException {
				return this.real.loadAll();
			}

			@Override
			public void saveAll(java.util.Collection<AccountSnapshot> accounts) throws StorageException {
				throw new StorageException("disk full (simulated)");
			}

			@Override
			public void delete(String key) {
			}

			@Override
			public void close() {
			}
		};
		try {
			new StorageInitializer(failing, new LegacyTableSource(this.db.connections, "solaryeconomy"),
					TestSupport.settings().getMoney(), this.db.log()).initializeAndLoad();
			fail("the failed import must abort the startup");
		} catch (StorageException expected) {
			assertEquals(Collections.emptyList(), this.db.repository().loadAll());
		}

		assertEquals(1, start().size());
	}
}
