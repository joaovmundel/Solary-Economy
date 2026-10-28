package com.redeskyller.bukkit.solaryeconomy.audit;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import com.redeskyller.bukkit.solaryeconomy.economy.BalanceChange;
import com.redeskyller.bukkit.solaryeconomy.economy.BalanceChangeListener;

/**
 * Journal of every balance change, one line each, in {@code transactions.0.log} (rotated at 1 MB,
 * five files kept). On a server where anyone can join with any name this is the only way to
 * reconstruct what happened to an account after a duplication exploit or a staff dispute.
 * Written synchronously: one small buffered write per transaction, and no line is lost on a crash.
 */
public final class AuditLog implements BalanceChangeListener {

	private static final int FILE_SIZE_BYTES = 1024 * 1024;
	private static final int FILE_COUNT = 5;

	private final File directory;
	private final Logger pluginLogger;
	private final Logger journal = Logger.getLogger("SolaryEconomy.Audit");

	private FileHandler handler;

	public AuditLog(File directory, Logger pluginLogger) {
		this.directory = directory;
		this.pluginLogger = pluginLogger;
		this.journal.setUseParentHandlers(false);
		this.journal.setLevel(Level.INFO);
	}

	/** Starts or stops writing. Safe to call repeatedly with the same value. */
	public synchronized void setEnabled(boolean enabled) {
		if (enabled && this.handler == null)
			open();
		else if (!enabled && this.handler != null)
			close();
	}

	public synchronized void close() {
		if (this.handler == null)
			return;
		this.journal.removeHandler(this.handler);
		this.handler.close();
		this.handler = null;
	}

	@Override
	public synchronized void onBalanceChange(BalanceChange change) {
		if (this.handler != null)
			this.journal.info(change.toString());
	}

	private void open() {
		try {
			if (!this.directory.exists() && !this.directory.mkdirs())
				throw new IOException("não foi possível criar a pasta " + this.directory.getPath());

			// '%' is special in FileHandler patterns, so a data folder containing one must be escaped
			String pattern = this.directory.getPath().replace('\\', '/').replace("%", "%%") + "/transactions.%g.log";
			FileHandler fileHandler = new FileHandler(pattern, FILE_SIZE_BYTES, FILE_COUNT, true);
			fileHandler.setEncoding("UTF-8");
			fileHandler.setFormatter(new LineFormatter());
			this.journal.addHandler(fileHandler);
			this.handler = fileHandler;
		} catch (IOException | SecurityException exception) {
			this.pluginLogger.log(Level.WARNING, "Não foi possível abrir o log de transações; as alterações de saldo "
					+ "não serão registradas em arquivo (audit_log).", exception);
		}
	}

	/** {@code 2026-10-23 15:20:01 | DEPOSIT | Steve | amount=10 | 5 -> 15} */
	private static final class LineFormatter extends Formatter {
		@Override
		public String format(LogRecord record) {
			return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(record.getMillis())) + " | "
					+ record.getMessage() + System.getProperty("line.separator");
		}
	}
}
