package com.redeskyller.bukkit.solaryeconomy.storage;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.redeskyller.bukkit.solaryeconomy.economy.AccountSnapshot;
import com.redeskyller.bukkit.solaryeconomy.economy.DirtyAccountSource;
import com.redeskyller.bukkit.solaryeconomy.log.DebugLog;

/**
 * Writes the accounts changed since the last run in one transaction. Runs on the storage thread,
 * periodically and once more at shutdown.
 * <p>
 * If the database is down the batch goes back to the pending set and is retried on the next run, so
 * an outage costs nothing but delay. The failure is logged loudly on the first occurrence and then
 * every tenth one, not on every run, so a long outage does not flood the console.
 */
public final class WriteBehindFlusher implements StorageTask {

	private static final int LOG_EVERY_N_FAILURES = 10;

	private final DirtyAccountSource source;
	private final AccountRepository repository;
	private final Logger logger;
	private final DebugLog debugLog;

	/** Touched only by the storage thread. */
	private int consecutiveFailures;

	public WriteBehindFlusher(DirtyAccountSource source, AccountRepository repository, Logger logger,
			DebugLog debugLog) {
		this.source = source;
		this.repository = repository;
		this.logger = logger;
		this.debugLog = debugLog;
	}

	@Override
	public void run() {
		List<AccountSnapshot> batch = this.source.drainDirty();
		if (batch.isEmpty())
			return;

		try {
			this.repository.saveAll(batch);
		} catch (StorageException exception) {
			this.source.restoreDirty(keysOf(batch));
			reportFailure(batch.size(), exception);
			return;
		}

		if (this.consecutiveFailures > 0)
			this.logger.info("Banco de dados de volta: " + batch.size() + " conta(s) gravada(s) após "
					+ this.consecutiveFailures + " falha(s).");
		this.consecutiveFailures = 0;
		this.debugLog.debug(batch.size() + " conta(s) gravada(s).");
	}

	private void reportFailure(int accounts, StorageException exception) {
		this.consecutiveFailures++;
		if (this.consecutiveFailures == 1 || this.consecutiveFailures % LOG_EVERY_N_FAILURES == 0)
			this.logger.log(Level.SEVERE, "Não foi possível gravar " + accounts + " conta(s) (falha seguida nº "
					+ this.consecutiveFailures + "). As alterações continuam em memória e serão gravadas assim que o "
					+ "banco de dados voltar.", exception);
	}

	private static List<String> keysOf(List<AccountSnapshot> batch) {
		List<String> keys = new ArrayList<String>(batch.size());
		for (AccountSnapshot account : batch)
			keys.add(account.getKey());
		return keys;
	}
}
