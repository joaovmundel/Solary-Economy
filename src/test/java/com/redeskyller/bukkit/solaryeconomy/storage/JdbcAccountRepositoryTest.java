package com.redeskyller.bukkit.solaryeconomy.storage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.redeskyller.bukkit.solaryeconomy.economy.AccountSnapshot;

public class JdbcAccountRepositoryTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	private SqliteFixture db;
	private JdbcAccountRepository repository;

	@Before
	public void setUp() throws Exception {
		this.db = new SqliteFixture(this.folder.getRoot());
		this.repository = this.db.repository();
		this.repository.initialize();
	}

	@After
	public void tearDown() {
		this.db.close();
	}

	private static AccountSnapshot account(String key, String display, String balance, boolean disabled) {
		return new AccountSnapshot(key, display, new BigDecimal(balance), disabled);
	}

	private static AccountSnapshot find(List<AccountSnapshot> accounts, String key) {
		for (AccountSnapshot account : accounts)
			if (account.getKey().equals(key))
				return account;
		return null;
	}

	@Test
	public void initializeIsIdempotentAndStartsEmpty() throws Exception {
		this.repository.initialize();
		this.repository.initialize();

		assertEquals(0, this.repository.count());
		assertTrue(this.repository.loadAll().isEmpty());
	}

	@Test
	public void savedAccountsComeBackExactly() throws Exception {
		this.repository.saveAll(Arrays.asList(account("alice", "Alice", "1234.56", false),
				account("bob", "Bob", "0.00", true),
				account("rich", "Rich", "999999999999999999.99", false)));

		List<AccountSnapshot> loaded = this.repository.loadAll();

		assertEquals(3, loaded.size());
		assertEquals(3, this.repository.count());
		assertEquals(0, new BigDecimal("1234.56").compareTo(find(loaded, "alice").getBalance()));
		assertEquals("Alice", find(loaded, "alice").getDisplayName());
		assertTrue(find(loaded, "bob").isReceivingDisabled());
		assertFalse(find(loaded, "alice").isReceivingDisabled());
		assertEquals(0, new BigDecimal("999999999999999999.99").compareTo(find(loaded, "rich").getBalance()));
	}

	@Test
	public void savingTheSameKeyAgainUpdatesInsteadOfDuplicating() throws Exception {
		this.repository.saveAll(Collections.singletonList(account("alice", "Alice", "10", false)));
		this.repository.saveAll(Collections.singletonList(account("alice", "Alice", "25.5", true)));

		List<AccountSnapshot> loaded = this.repository.loadAll();

		assertEquals(1, loaded.size());
		assertEquals(0, new BigDecimal("25.5").compareTo(loaded.get(0).getBalance()));
		assertTrue(loaded.get(0).isReceivingDisabled());
	}

	@Test
	public void deleteRemovesOnlyThatAccount() throws Exception {
		this.repository.saveAll(Arrays.asList(account("alice", "Alice", "1", false), account("bob", "Bob", "2", false)));

		this.repository.delete("alice");

		List<AccountSnapshot> loaded = this.repository.loadAll();
		assertEquals(1, loaded.size());
		assertEquals("bob", loaded.get(0).getKey());
		this.repository.delete("never-existed"); // not an error
	}

	@Test
	public void hostileNamesAreStoredAsPlainDataAndTheTableSurvives() throws Exception {
		String[] hostile = { "a' OR '1'='1", "x'; DROP TABLE solaryeconomy_accounts;--", "\"quoted\"",
				"emojiéñ中", "100%_\\wild" };
		List<AccountSnapshot> batch = new ArrayList<AccountSnapshot>();
		for (String name : hostile)
			batch.add(account(name.toLowerCase(), name, "5", false));
		batch.add(account("victim", "Victim", "777", false));

		this.repository.saveAll(batch);
		this.repository.saveAll(Collections.singletonList(account("a' or '1'='1", "a' OR '1'='1", "1", false)));
		this.repository.delete("x'; drop table solaryeconomy_accounts;--");

		List<AccountSnapshot> loaded = this.repository.loadAll();
		assertEquals(5, loaded.size());
		assertEquals(0, new BigDecimal("777").compareTo(find(loaded, "victim").getBalance()));
		assertEquals(0, new BigDecimal("1").compareTo(find(loaded, "a' or '1'='1").getBalance()));
		assertEquals("emojiéñ中", find(loaded, "emojiéñ中").getDisplayName());
	}

	@Test
	public void aFailedBatchIsRolledBackCompletely() throws Exception {
		this.repository.saveAll(Collections.singletonList(account("alice", "Alice", "10", false)));

		ConnectionProvider failingCommit = new ConnectionProvider() {
			@Override
			public Connection get() throws SQLException {
				return failOnCommit(JdbcAccountRepositoryTest.this.db.connections.get());
			}

			@Override
			public void close() {
			}
		};
		JdbcAccountRepository failing = new JdbcAccountRepository(failingCommit, new SqliteDialect(),
				"solaryeconomy_accounts", this.db.log());

		try {
			failing.saveAll(Arrays.asList(account("alice", "Alice", "9999", false), account("bob", "Bob", "5", false)));
			fail("the commit failure must surface");
		} catch (StorageException expected) {
			assertTrue(expected.getMessage(), expected.getMessage().contains("2 conta"));
		}

		List<AccountSnapshot> loaded = this.repository.loadAll();
		assertEquals(1, loaded.size());
		assertEquals(0, new BigDecimal("10").compareTo(loaded.get(0).getBalance()));
		assertTrue("the connection must be usable (auto-commit restored)", this.db.connections.get().getAutoCommit());
	}

	@Test
	public void damagedRowsAreSkippedWithAWarningInsteadOfFailingTheLoad() throws Exception {
		this.repository.saveAll(Collections.singletonList(account("good", "Good", "3", false)));
		this.db.execute("INSERT INTO solaryeconomy_accounts VALUES ('broken', 'Broken', 'abc', 0, 0)");
		this.db.execute("INSERT INTO solaryeconomy_accounts VALUES ('UPPER', 'UPPER', '5', 0, 0)");
		this.db.execute("INSERT INTO solaryeconomy_accounts VALUES ('mismatch', 'Something Else', '7', 0, 0)");

		List<AccountSnapshot> loaded = this.repository.loadAll();

		assertEquals(2, loaded.size());
		assertEquals("Good", find(loaded, "good").getDisplayName());
		assertEquals("mismatch", find(loaded, "mismatch").getDisplayName()); // display must match the key
		assertEquals(2, this.db.logger.count(Level.WARNING));
	}

	@Test(expected = IllegalArgumentException.class)
	public void aTableNameThatIsNotAnIdentifierIsRefused() {
		new JdbcAccountRepository(this.db.connections, new SqliteDialect(), "x; DROP TABLE y", this.db.log());
	}

	@Test
	public void theUpsertAvoidsSyntaxMissingFromTheSqliteBundledWithTheServer() {
		// sqlite 3.7.2 has no ON CONFLICT ... DO UPDATE (3.24+); the dialect must not need it
		assertFalse(new SqliteDialect().upsert("t").toUpperCase().contains("ON CONFLICT"));
	}

	@Test
	public void mysqlDialectUsesABinaryKeyAndANativeUpsert() {
		MySqlDialect mysql = new MySqlDialect();

		assertTrue(mysql.createTable("t").contains("utf8_bin"));
		assertTrue(mysql.createTable("t").contains("DECIMAL(40,8)"));
		assertTrue(mysql.upsert("t").contains("ON DUPLICATE KEY UPDATE"));
	}

	/** A JDBC connection whose commit always fails, to prove the repository rolls back. */
	private static Connection failOnCommit(final Connection real) {
		return (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(), new Class<?>[] { Connection.class },
				new InvocationHandler() {
					@Override
					public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
						if (method.getName().equals("commit"))
							throw new SQLException("disk full (simulated)");
						try {
							return method.invoke(real, args);
						} catch (InvocationTargetException exception) {
							throw exception.getCause();
						}
					}
				});
	}
}
