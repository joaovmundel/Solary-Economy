package com.redeskyller.bukkit.solaryeconomy.storage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;

import org.junit.Before;
import org.junit.Test;

import com.redeskyller.bukkit.solaryeconomy.TestSupport;
import com.redeskyller.bukkit.solaryeconomy.economy.AccountSnapshot;
import com.redeskyller.bukkit.solaryeconomy.economy.EconomyServiceImpl;
import com.redeskyller.bukkit.solaryeconomy.log.DebugLog;

public class WriteBehindFlusherTest {

	/** An in-memory repository that can be told to fail. */
	private static final class FakeRepository implements AccountRepository {
		final List<AccountSnapshot> saved = new ArrayList<AccountSnapshot>();
		boolean failing;
		int attempts;

		@Override
		public void initialize() {
		}

		@Override
		public long count() {
			return this.saved.size();
		}

		@Override
		public List<AccountSnapshot> loadAll() {
			return this.saved;
		}

		@Override
		public void saveAll(Collection<AccountSnapshot> accounts) throws StorageException {
			this.attempts++;
			if (this.failing)
				throw new StorageException("database is down (simulated)");
			this.saved.addAll(accounts);
		}

		@Override
		public void delete(String key) {
		}

		@Override
		public void close() {
		}
	}

	private final TestSupport.RecordingLogger logger = TestSupport.logger();
	private final FakeRepository repository = new FakeRepository();
	private EconomyServiceImpl economy;
	private WriteBehindFlusher flusher;

	@Before
	public void setUp() {
		this.economy = new EconomyServiceImpl(TestSupport.holder(), this.logger);
		this.flusher = new WriteBehindFlusher(this.economy, this.repository, this.logger,
				new DebugLog(this.logger, TestSupport.holder()));
	}

	@Test
	public void writesOnlyWhatChangedAndNothingWhenIdle() {
		this.economy.createAccount("Alice", new BigDecimal("10"));
		this.economy.createAccount("Bob", new BigDecimal("20"));

		this.flusher.run();
		this.flusher.run(); // nothing changed: no write

		assertEquals(2, this.repository.saved.size());
		assertEquals(1, this.repository.attempts);

		this.economy.deposit("Alice", new BigDecimal("5"));
		this.flusher.run();

		assertEquals(3, this.repository.saved.size());
		assertEquals("alice", this.repository.saved.get(2).getKey());
		assertEquals(0, new BigDecimal("15").compareTo(this.repository.saved.get(2).getBalance()));
	}

	@Test
	public void anOutageLosesNothingAndTheBatchIsRetriedWithTheLatestBalance() {
		this.economy.createAccount("Alice", new BigDecimal("10"));
		this.repository.failing = true;

		this.flusher.run();
		this.economy.deposit("Alice", new BigDecimal("5")); // changes while the database is down
		this.flusher.run();
		assertTrue(this.repository.saved.isEmpty());

		this.repository.failing = false;
		this.flusher.run();

		assertEquals(1, this.repository.saved.size());
		assertEquals(0, new BigDecimal("15").compareTo(this.repository.saved.get(0).getBalance()));
	}

	@Test
	public void aLongOutageIsLoggedOnTheFirstFailureAndThenOnlyEveryTenth() {
		this.economy.createAccount("Alice", new BigDecimal("10"));
		this.repository.failing = true;

		for (int i = 0; i < 25; i++)
			this.flusher.run();

		assertEquals(25, this.repository.attempts);
		assertEquals(3, this.logger.count(Level.SEVERE)); // failures 1, 10 and 20
	}

	@Test
	public void recoveryIsAnnouncedOnce() {
		this.economy.createAccount("Alice", new BigDecimal("10"));
		this.repository.failing = true;
		this.flusher.run();
		this.flusher.run();

		this.repository.failing = false;
		this.flusher.run();
		this.economy.deposit("Alice", BigDecimal.ONE);
		this.flusher.run();

		assertEquals(1, this.logger.count(Level.INFO));
		assertTrue(this.logger.contains(Level.INFO, "de volta"));
	}
}
