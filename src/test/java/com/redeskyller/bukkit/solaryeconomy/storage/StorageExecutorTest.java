package com.redeskyller.bukkit.solaryeconomy.storage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

import org.junit.After;
import org.junit.Test;

import com.redeskyller.bukkit.solaryeconomy.TestSupport;

public class StorageExecutorTest {

	private final TestSupport.RecordingLogger logger = TestSupport.logger();
	private final StorageExecutor executor = new StorageExecutor(this.logger);

	@After
	public void tearDown() {
		this.executor.shutdown(5);
	}

	@Test
	public void callReturnsTheResultOfTheTask() throws Exception {
		assertEquals("done", this.executor.call("t", new Callable<String>() {
			@Override
			public String call() {
				return "done";
			}
		}, 5));
	}

	@Test
	public void everythingRunsOnOneDedicatedThread() throws Exception {
		final List<String> names = Collections.synchronizedList(new ArrayList<String>());
		for (int i = 0; i < 5; i++)
			this.executor.execute("t", new StorageTask() {
				@Override
				public void run() {
					names.add(Thread.currentThread().getName());
				}
			});
		this.executor.call("barrier", new Callable<Void>() {
			@Override
			public Void call() {
				names.add(Thread.currentThread().getName());
				return null;
			}
		}, 5);

		assertEquals(6, names.size());
		assertEquals("SolaryEconomy-Storage", names.get(0));
		for (String name : names)
			assertEquals(names.get(0), name);
	}

	@Test
	public void tasksRunInTheOrderTheyWereQueued() throws Exception {
		final List<Integer> order = Collections.synchronizedList(new ArrayList<Integer>());
		for (int i = 0; i < 50; i++) {
			final int n = i;
			this.executor.execute("t", new StorageTask() {
				@Override
				public void run() {
					order.add(n);
				}
			});
		}
		this.executor.call("barrier", new Callable<Void>() {
			@Override
			public Void call() {
				return null;
			}
		}, 5);

		for (int i = 0; i < 50; i++)
			assertEquals(Integer.valueOf(i), order.get(i));
	}

	@Test
	public void aStorageExceptionInCallKeepsItsTypeAndMessage() {
		try {
			this.executor.call("t", new Callable<Void>() {
				@Override
				public Void call() throws StorageException {
					throw new StorageException("db exploded");
				}
			}, 5);
			fail();
		} catch (StorageException expected) {
			assertEquals("db exploded", expected.getMessage());
		}
	}

	@Test
	public void anyOtherFailureInCallIsWrappedWithTheTaskDescription() {
		try {
			this.executor.call("loading", new Callable<Void>() {
				@Override
				public Void call() {
					throw new IllegalStateException("bug");
				}
			}, 5);
			fail();
		} catch (StorageException expected) {
			assertTrue(expected.getMessage(), expected.getMessage().contains("loading"));
			assertTrue(expected.getCause() instanceof IllegalStateException);
		}
	}

	@Test
	public void aSlowTaskTimesOutInsteadOfBlockingTheServerForever() throws Exception {
		final CountDownLatch release = new CountDownLatch(1);
		try {
			this.executor.call("slow", new Callable<Void>() {
				@Override
				public Void call() throws Exception {
					release.await();
					return null;
				}
			}, 1);
			fail();
		} catch (StorageException expected) {
			assertTrue(expected.getMessage(), expected.getMessage().contains("slow"));
		} finally {
			release.countDown();
		}
	}

	@Test
	public void aFailingFireAndForgetTaskIsLoggedAndTheThreadSurvives() throws Exception {
		this.executor.execute("write", new StorageTask() {
			@Override
			public void run() throws Exception {
				throw new StorageException("disk full");
			}
		});
		this.executor.execute("bug", new StorageTask() {
			@Override
			public void run() {
				throw new IllegalStateException("bug");
			}
		});
		assertEquals("alive", this.executor.call("t", new Callable<String>() {
			@Override
			public String call() {
				return "alive";
			}
		}, 5));

		assertEquals(1, this.logger.count(Level.WARNING));
		assertEquals(1, this.logger.count(Level.SEVERE));
	}

	@Test
	public void aRepeatingTaskKeepsRunningEvenAfterItThrows() throws Exception {
		final AtomicInteger runs = new AtomicInteger();
		final CountDownLatch enough = new CountDownLatch(3);
		ScheduledFuture<?> future = this.executor.scheduleWithFixedDelay("flush", new StorageTask() {
			@Override
			public void run() {
				runs.incrementAndGet();
				enough.countDown();
				throw new IllegalStateException("every time");
			}
		}, 1);

		assertTrue("the periodic task must not be cancelled by its own exception", enough.await(10, java.util.concurrent.TimeUnit.SECONDS));
		future.cancel(false);
		assertTrue(runs.get() >= 3);
	}

	@Test
	public void shutdownWaitsForQueuedWorkAndThenRefusesNewWork() throws Exception {
		final AtomicInteger done = new AtomicInteger();
		this.executor.execute("last write", new StorageTask() {
			@Override
			public void run() throws Exception {
				Thread.sleep(300);
				done.incrementAndGet();
			}
		});

		assertTrue(this.executor.shutdown(5));
		assertEquals(1, done.get());

		this.executor.execute("too late", new StorageTask() {
			@Override
			public void run() {
				done.incrementAndGet();
			}
		});
		assertEquals(1, done.get());
		assertFalse(this.logger.contains(Level.SEVERE, "too late"));
		assertTrue(this.logger.contains(Level.WARNING, "too late"));
	}
}
