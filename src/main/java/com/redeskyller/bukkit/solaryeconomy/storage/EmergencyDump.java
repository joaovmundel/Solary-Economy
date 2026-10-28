package com.redeskyller.bukkit.solaryeconomy.storage;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;

import com.redeskyller.bukkit.solaryeconomy.economy.AccountSnapshot;

/**
 * Last resort when the database is unreachable at shutdown: writes the accounts that could not be
 * saved to a CSV file so that an admin can restore them by hand instead of losing the balances.
 */
public final class EmergencyDump {

	private EmergencyDump() {
	}

	/** @return the file that was written */
	public static File write(File directory, Collection<AccountSnapshot> accounts) throws IOException {
		if (!directory.exists() && !directory.mkdirs())
			throw new IOException("não foi possível criar a pasta " + directory.getPath());

		File file = new File(directory,
				"unsaved-accounts-" + new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date()) + ".csv");
		try (Writer writer = new OutputStreamWriter(Files.newOutputStream(file.toPath()), Charset.forName("UTF-8"))) {
			writer.write("account_key;display_name;balance;receiving_disabled\n");
			for (AccountSnapshot account : accounts)
				writer.write(quote(account.getKey()) + ";" + quote(account.getDisplayName()) + ";"
						+ account.getBalance().toPlainString() + ";" + (account.isReceivingDisabled() ? 1 : 0) + "\n");
		}
		return file;
	}

	private static String quote(String text) {
		return "\"" + text.replace("\"", "\"\"") + "\"";
	}
}
