package com.redeskyller.bukkit.solaryeconomy.economy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;

import com.redeskyller.bukkit.solaryeconomy.TestSupport;

public class EconomyServiceImplTest {

	private EconomyServiceImpl economy;

	@Before
	public void setUp() {
		this.economy = new EconomyServiceImpl(TestSupport.holder(), TestSupport.logger());
	}

	private static BigDecimal n(String value) {
		return new BigDecimal(value);
	}

	private void account(String name, String balance) {
		assertEquals(CreateResult.CREATED, this.economy.createAccount(name, n(balance)));
	}

	private void assertBalance(String expected, String name) {
		assertEquals(0, n(expected).compareTo(this.economy.getBalance(name)));
	}

	// ---- accounts ----

	@Test
	public void createdAccountStartsWithTheConfiguredStartBalance() {
		Map<String, Object> overrides = new HashMap<String, Object>();
		overrides.put("start_value", 100);
		EconomyServiceImpl custom = new EconomyServiceImpl(TestSupport.holder(overrides), TestSupport.logger());

		assertEquals(CreateResult.CREATED, custom.createAccount("Steve"));
		assertEquals(0, n("100").compareTo(custom.getBalance("Steve")));
	}

	@Test
	public void namesAreCaseInsensitiveButDisplayKeepsTheFirstCasing() {
		account("Steve", "10");

		assertEquals(CreateResult.ALREADY_EXISTS, this.economy.createAccount("STEVE"));
		assertTrue(this.economy.hasAccount("sTeVe"));
		assertEquals("Steve", this.economy.getAccount("steve").getDisplayName());
		assertEquals("steve", this.economy.getAccount("STEVE").getKey());
	}

	@Test
	public void invalidNamesAndAmountsAreRefusedWithoutCreatingAnything() {
		assertEquals(CreateResult.INVALID_NAME, this.economy.createAccount(null));
		assertEquals(CreateResult.INVALID_NAME, this.economy.createAccount("   "));
		assertEquals(CreateResult.INVALID_NAME, this.economy.createAccount("a§b"));
		assertEquals(CreateResult.INVALID_AMOUNT, this.economy.createAccount("neg", n("-1")));
		assertEquals(CreateResult.INVALID_AMOUNT, this.economy.createAccount("nul", null));
		assertEquals(CreateResult.INVALID_AMOUNT,
				this.economy.createAccount("huge", n("1000000000000000001")));
		assertEquals(0, this.economy.getAccounts().size());
	}

	@Test
	public void deleteRemovesTheAccountOnlyOnce() {
		account("Steve", "10");

		assertTrue(this.economy.deleteAccount("steve"));
		assertFalse(this.economy.hasAccount("Steve"));
		assertFalse(this.economy.deleteAccount("Steve"));
		assertNull(this.economy.getAccount("Steve"));
	}

	@Test
	public void unknownAccountHasZeroBalanceAndCannotBeUsed() {
		assertBalance("0", "nobody");
		assertFalse(this.economy.has("nobody", n("0")));
		assertEquals(TransactionResult.ACCOUNT_NOT_FOUND, this.economy.deposit("nobody", n("1")));
		assertEquals(TransactionResult.ACCOUNT_NOT_FOUND, this.economy.withdraw("nobody", n("1")));
		assertEquals(TransactionResult.ACCOUNT_NOT_FOUND, this.economy.setBalance("nobody", n("1")));
		assertEquals(TransactionResult.ACCOUNT_NOT_FOUND, this.economy.setReceivingDisabled("nobody", true));
	}

	// ---- deposit / withdraw / set ----

	@Test
	public void depositAddsAndTruncatesExtraDecimals() {
		account("Steve", "0");

		assertEquals(TransactionResult.SUCCESS, this.economy.deposit("Steve", n("1.239")));
		assertBalance("1.23", "Steve");
	}

	@Test
	public void withdrawRoundsTheDebitUpSoFractionsNeverFavorThePlayer() {
		account("Steve", "1.00");

		assertEquals(TransactionResult.SUCCESS, this.economy.withdraw("Steve", n("0.001")));
		assertBalance("0.99", "Steve");
	}

	@Test
	public void withdrawNeverOverdrawsAndLeavesTheBalanceUntouched() {
		account("Steve", "10");

		assertEquals(TransactionResult.INSUFFICIENT_FUNDS, this.economy.withdraw("Steve", n("10.01")));
		assertBalance("10", "Steve");
		assertEquals(TransactionResult.SUCCESS, this.economy.withdraw("Steve", n("10")));
		assertBalance("0", "Steve");
	}

	@Test
	public void negativeAndNullAmountsAreInvalidEverywhere() {
		account("Steve", "10");

		assertEquals(TransactionResult.INVALID_AMOUNT, this.economy.deposit("Steve", n("-1")));
		assertEquals(TransactionResult.INVALID_AMOUNT, this.economy.deposit("Steve", null));
		assertEquals(TransactionResult.INVALID_AMOUNT, this.economy.withdraw("Steve", n("-1")));
		assertEquals(TransactionResult.INVALID_AMOUNT, this.economy.setBalance("Steve", n("-1")));
		assertBalance("10", "Steve");
	}

	@Test
	public void depositOfZeroIsASilentNoOp() {
		account("Steve", "10");
		this.economy.drainDirty();

		assertEquals(TransactionResult.SUCCESS, this.economy.deposit("Steve", BigDecimal.ZERO));
		assertTrue(this.economy.drainDirty().isEmpty());
	}

	@Test
	public void balanceCannotExceedTheConfiguredMaximum() {
		Map<String, Object> overrides = new HashMap<String, Object>();
		overrides.put("max_balance", "1000");
		EconomyServiceImpl limited = new EconomyServiceImpl(TestSupport.holder(overrides), TestSupport.logger());
		limited.createAccount("Steve", n("900"));

		assertEquals(TransactionResult.BALANCE_LIMIT_EXCEEDED, limited.deposit("Steve", n("100.01")));
		assertEquals(0, n("900").compareTo(limited.getBalance("Steve")));
		assertEquals(TransactionResult.SUCCESS, limited.deposit("Steve", n("100")));
		assertEquals(TransactionResult.BALANCE_LIMIT_EXCEEDED, limited.setBalance("Steve", n("1001")));
	}

	@Test
	public void hasComparesExactlyWithoutFloatingPointError() {
		account("Steve", "0.30");

		assertTrue(this.economy.has("Steve", n("0.30")));
		assertFalse(this.economy.has("Steve", n("0.31")));
		assertFalse(this.economy.has("Steve", n("-1")));
	}

	@Test
	public void setBalanceReplacesTheValue() {
		account("Steve", "10");

		assertEquals(TransactionResult.SUCCESS, this.economy.setBalance("Steve", n("55.5")));
		assertBalance("55.5", "Steve");
	}

	// ---- transfer ----

	@Test
	public void transferMovesExactlyTheAmount() {
		account("Alice", "100");
		account("Bob", "5");

		assertEquals(TransactionResult.SUCCESS, this.economy.transfer("alice", "BOB", n("40.5")));
		assertBalance("59.5", "Alice");
		assertBalance("45.5", "Bob");
	}

	@Test
	public void transferRefusalsChangeNothing() {
		account("Alice", "100");
		account("Bob", "5");
		this.economy.setReceivingDisabled("Bob", true);

		assertEquals(TransactionResult.INSUFFICIENT_FUNDS, this.economy.transfer("Alice", "Bob", n("100.01")));
		assertEquals(TransactionResult.RECEIVING_DISABLED, this.economy.transfer("Alice", "Bob", n("1")));
		assertEquals(TransactionResult.SAME_ACCOUNT, this.economy.transfer("Alice", "ALICE", n("1")));
		assertEquals(TransactionResult.ACCOUNT_NOT_FOUND, this.economy.transfer("Alice", "Nobody", n("1")));
		assertEquals(TransactionResult.ACCOUNT_NOT_FOUND, this.economy.transfer("Nobody", "Bob", n("1")));
		assertEquals(TransactionResult.INVALID_AMOUNT, this.economy.transfer("Alice", "Bob", BigDecimal.ZERO));
		assertEquals(TransactionResult.INVALID_AMOUNT, this.economy.transfer("Alice", "Bob", n("-3")));
		assertBalance("100", "Alice");
		assertBalance("5", "Bob");
	}

	@Test
	public void transferCannotPushTheReceiverOverTheMaximum() {
		Map<String, Object> overrides = new HashMap<String, Object>();
		overrides.put("max_balance", "100");
		EconomyServiceImpl limited = new EconomyServiceImpl(TestSupport.holder(overrides), TestSupport.logger());
		limited.createAccount("Alice", n("100"));
		limited.createAccount("Bob", n("95"));

		assertEquals(TransactionResult.BALANCE_LIMIT_EXCEEDED, limited.transfer("Alice", "Bob", n("6")));
		assertEquals(0, n("100").compareTo(limited.getBalance("Alice")));
		assertEquals(0, n("95").compareTo(limited.getBalance("Bob")));
	}

	@Test
	public void receivingCanBeSwitchedBackOn() {
		account("Alice", "10");
		account("Bob", "10");

		this.economy.setReceivingDisabled("Bob", true);
		assertTrue(this.economy.isReceivingDisabled("bob"));
		this.economy.setReceivingDisabled("Bob", false);
		assertEquals(TransactionResult.SUCCESS, this.economy.transfer("Alice", "Bob", n("1")));
	}

	// ---- persistence bookkeeping ----

	@Test
	public void loadedAccountsAreCleanAndChangedOnesAreDrainedOnce() {
		this.economy.load(Arrays.asList(new AccountSnapshot("alice", "Alice", n("10"), false),
				new AccountSnapshot("bob", "Bob", n("20"), true)));

		assertTrue(this.economy.drainDirty().isEmpty());
		assertTrue(this.economy.isReceivingDisabled("Bob"));

		this.economy.deposit("Alice", n("5"));
		List<AccountSnapshot> dirty = this.economy.drainDirty();
		assertEquals(1, dirty.size());
		assertEquals("alice", dirty.get(0).getKey());
		assertEquals(0, n("15").compareTo(dirty.get(0).getBalance()));
		assertTrue(this.economy.drainDirty().isEmpty());
	}

	@Test
	public void restoredKeysAreDrainedAgainWithTheirCurrentState() {
		account("Alice", "10");
		List<AccountSnapshot> failedBatch = this.economy.drainDirty();
		this.economy.deposit("Alice", n("1"));

		this.economy.restoreDirty(Collections.singletonList(failedBatch.get(0).getKey()));

		List<AccountSnapshot> retry = this.economy.drainDirty();
		assertEquals(1, retry.size());
		assertEquals(0, n("11").compareTo(retry.get(0).getBalance()));
	}

	@Test
	public void deletedAccountsAreNotDrained() {
		account("Alice", "10");
		this.economy.deleteAccount("Alice");

		assertTrue(this.economy.drainDirty().isEmpty());
	}

	// ---- observers ----

	@Test
	public void listenersSeeEveryChangeWithBeforeAndAfter() {
		final List<BalanceChange> seen = new ArrayList<BalanceChange>();
		this.economy.addListener(new BalanceChangeListener() {
			@Override
			public void onBalanceChange(BalanceChange change) {
				seen.add(change);
			}
		});

		account("Alice", "10");
		account("Bob", "0");
		this.economy.deposit("Alice", n("5"));
		this.economy.withdraw("Alice", n("3"));
		this.economy.setBalance("Alice", n("50"));
		this.economy.transfer("Alice", "Bob", n("20"));
		this.economy.deleteAccount("Bob");

		List<BalanceChange.Type> types = new ArrayList<BalanceChange.Type>();
		for (BalanceChange change : seen)
			types.add(change.getType());
		assertEquals(Arrays.asList(BalanceChange.Type.CREATE, BalanceChange.Type.CREATE, BalanceChange.Type.DEPOSIT,
				BalanceChange.Type.WITHDRAW, BalanceChange.Type.SET, BalanceChange.Type.TRANSFER_OUT,
				BalanceChange.Type.TRANSFER_IN, BalanceChange.Type.DELETE), types);

		BalanceChange deposit = seen.get(2);
		assertEquals("Alice", deposit.getAccount());
		assertEquals(0, n("10").compareTo(deposit.getBalanceBefore()));
		assertEquals(0, n("15").compareTo(deposit.getBalanceAfter()));
		assertEquals("Bob", seen.get(5).getCounterpart());
	}

	@Test
	public void aFailingListenerNeverBreaksTheOperation() {
		this.economy.addListener(new BalanceChangeListener() {
			@Override
			public void onBalanceChange(BalanceChange change) {
				throw new IllegalStateException("boom");
			}
		});

		assertEquals(CreateResult.CREATED, this.economy.createAccount("Alice", n("10")));
		assertEquals(TransactionResult.SUCCESS, this.economy.deposit("Alice", n("1")));
		assertBalance("11", "Alice");
	}

	@Test
	public void refusedOperationsFireNoEvents() {
		final List<BalanceChange> seen = new ArrayList<BalanceChange>();
		account("Alice", "1");
		this.economy.addListener(new BalanceChangeListener() {
			@Override
			public void onBalanceChange(BalanceChange change) {
				seen.add(change);
			}
		});

		this.economy.withdraw("Alice", n("99"));
		this.economy.deposit("Alice", n("-1"));
		this.economy.transfer("Alice", "Alice", n("1"));

		assertTrue(seen.isEmpty());
	}

	// ---- concurrency: the reason balances live behind locks ----

	@Test(timeout = 60000)
	public void concurrentTransfersInBothDirectionsNeitherDeadlockNorChangeTheTotal() throws Exception {
		final String[] names = { "A", "B", "C", "D", "E" };
		for (String name : names)
			account(name, "1000");

		int threads = 8;
		ExecutorService pool = Executors.newFixedThreadPool(threads);
		final CountDownLatch start = new CountDownLatch(1);
		List<Future<Void>> futures = new ArrayList<Future<Void>>();
		for (int t = 0; t < threads; t++) {
			final long seed = t;
			futures.add(pool.submit(new Callable<Void>() {
				@Override
				public Void call() throws Exception {
					Random random = new Random(seed);
					start.await();
					for (int i = 0; i < 5000; i++) {
						String from = names[random.nextInt(names.length)];
						String to = names[random.nextInt(names.length)];
						EconomyServiceImplTest.this.economy.transfer(from, to, n(String.valueOf(1 + random.nextInt(50))));
					}
					return null;
				}
			}));
		}
		start.countDown();
		for (Future<Void> future : futures)
			future.get(50, TimeUnit.SECONDS);
		pool.shutdown();

		BigDecimal total = BigDecimal.ZERO;
		for (AccountSnapshot account : this.economy.getAccounts()) {
			assertTrue(account + " went negative", account.getBalance().signum() >= 0);
			total = total.add(account.getBalance());
		}
		assertEquals(0, n("5000").compareTo(total));
	}

	@Test(timeout = 60000)
	public void concurrentDepositsAndWithdrawalsLoseNoUpdate() throws Exception {
		account("Shared", "0");

		int threads = 8;
		final int rounds = 5000;
		ExecutorService pool = Executors.newFixedThreadPool(threads);
		List<Future<Void>> futures = new ArrayList<Future<Void>>();
		for (int t = 0; t < threads; t++) {
			futures.add(pool.submit(new Callable<Void>() {
				@Override
				public Void call() {
					for (int i = 0; i < rounds; i++) {
						EconomyServiceImplTest.this.economy.deposit("Shared", n("2"));
						EconomyServiceImplTest.this.economy.withdraw("Shared", n("1"));
					}
					return null;
				}
			}));
		}
		for (Future<Void> future : futures)
			future.get(50, TimeUnit.SECONDS);
		pool.shutdown();

		assertBalance(String.valueOf(threads * rounds), "Shared");
		assertNotNull(this.economy.getAccount("Shared"));
	}
}
