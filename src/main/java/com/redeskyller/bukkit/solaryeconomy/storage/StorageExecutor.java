package com.redeskyller.bukkit.solaryeconomy.storage;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The only thread that touches the database. Every JDBC call of the plugin is queued here, so the
 * server thread never waits on I/O after startup, the single connection is never shared between
 * threads and operations happen in the order they were requested (a delete can never overtake the
 * write queued before it).
 */
public final class StorageExecutor {

	private final ScheduledThreadPoolExecutor executor;
	private final Logger logger;

	public StorageExecutor(Logger logger) {
		this.logger = logger;
		this.executor = new ScheduledThreadPoolExecutor(1, new ThreadFactory() {
			@Override
			public Thread newThread(Runnable runnable) {
				Thread thread = new Thread(runnable, "SolaryEconomy-Storage");
				// Writes are flushed explicitly in onDisable; a stuck driver must not keep the JVM alive.
				thread.setDaemon(true);
				return thread;
			}
		});
	}

	/** Runs a task on the storage thread and waits for its result. Meant for startup and shutdown only. */
	public <T> T call(final String description, Callable<T> task, long timeoutSeconds) throws StorageException {
		Future<T> future;
		try {
			future = this.executor.submit(task);
		} catch (RejectedExecutionException exception) {
			throw new StorageException("O armazenamento já foi encerrado (" + description + ").", exception);
		}

		try {
			return future.get(timeoutSeconds, TimeUnit.SECONDS);
		} catch (ExecutionException exception) {
			Throwable cause = exception.getCause();
			if (cause instanceof StorageException)
				throw (StorageException) cause;
			throw new StorageException(description + " falhou: " + cause, cause);
		} catch (TimeoutException exception) {
			future.cancel(true);
			throw new StorageException(description + " não terminou em " + timeoutSeconds + "s.", exception);
		} catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			throw new StorageException(description + " foi interrompido.", exception);
		}
	}

	/** Queues a task and returns at once. A failure is logged, never thrown at the caller. */
	public void execute(String description, StorageTask task) {
		try {
			this.executor.execute(guarded(description, task));
		} catch (RejectedExecutionException exception) {
			this.logger.warning("Tarefa descartada, o armazenamento já foi encerrado: " + description);
		}
	}

	/** Runs a task repeatedly, {@code delaySeconds} after the previous run finished. */
	public ScheduledFuture<?> scheduleWithFixedDelay(String description, StorageTask task, long delaySeconds) {
		return this.executor.scheduleWithFixedDelay(guarded(description, task), delaySeconds, delaySeconds,
				TimeUnit.SECONDS);
	}

	/**
	 * Stops accepting work and waits for what is already queued (typically the final flush).
	 *
	 * @return whether everything queued finished in time
	 */
	public boolean shutdown(long timeoutSeconds) {
		this.executor.shutdown();
		try {
			if (this.executor.awaitTermination(timeoutSeconds, TimeUnit.SECONDS))
				return true;
		} catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
		}
		int abandoned = this.executor.shutdownNow().size();
		this.logger.severe("O armazenamento não terminou em " + timeoutSeconds + "s; " + abandoned
				+ " tarefa(s) pendente(s) foram descartadas.");
		return false;
	}

	/** A periodic task that throws is silently cancelled by the JDK, so nothing may escape. */
	private Runnable guarded(final String description, final StorageTask task) {
		return new Runnable() {
			@Override
			public void run() {
				try {
					task.run();
				} catch (StorageException exception) {
					StorageExecutor.this.logger.log(Level.WARNING,
							description + " falhou: " + exception.getMessage(), exception.getCause());
				} catch (Throwable throwable) {
					StorageExecutor.this.logger.log(Level.SEVERE, "Erro inesperado em: " + description, throwable);
				}
			}
		};
	}
}
