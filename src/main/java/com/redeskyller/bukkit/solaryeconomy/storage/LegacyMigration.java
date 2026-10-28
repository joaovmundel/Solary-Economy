package com.redeskyller.bukkit.solaryeconomy.storage;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import com.redeskyller.bukkit.solaryeconomy.config.MoneySettings;
import com.redeskyller.bukkit.solaryeconomy.economy.AccountNames;
import com.redeskyller.bukkit.solaryeconomy.economy.AccountSnapshot;

/**
 * Cleans the old table into valid accounts. Nothing is dropped silently: every skipped, clamped or
 * merged row is reported, because this is somebody's economy.
 */
public final class LegacyMigration {

	private LegacyMigration() {
	}

	public static List<AccountSnapshot> convert(List<LegacyAccount> rows, MoneySettings money, Logger logger) {
		Map<String, AccountSnapshot> byKey = new LinkedHashMap<String, AccountSnapshot>();

		for (LegacyAccount row : rows) {
			String key = AccountNames.keyOf(row.getName());
			if (key == null) {
				logger.warning("Importação: conta ignorada, nome inválido '" + row.getName() + "'.");
				continue;
			}
			BigDecimal balance = parseBalance(row, logger);
			if (balance == null)
				continue;

			AccountSnapshot account = new AccountSnapshot(key, AccountNames.displayNameOf(row.getName()), balance,
					row.isReceivingDisabled());
			AccountSnapshot existing = byKey.get(key);
			if (existing == null) {
				byKey.put(key, account);
				continue;
			}
			// "Joao" and "joao" were different accounts before; now they are one. Keep the richer one.
			AccountSnapshot kept = account.getBalance().compareTo(existing.getBalance()) > 0 ? account : existing;
			AccountSnapshot dropped = kept == account ? existing : account;
			byKey.put(key, kept);
			logger.warning("Importação: as contas '" + existing.getDisplayName() + "' e '" + account.getDisplayName()
					+ "' agora são a mesma conta (o nome não diferencia maiúsculas). Mantido o maior saldo, "
					+ kept.getBalance().toPlainString() + "; o outro era " + dropped.getBalance().toPlainString() + ".");
		}

		List<AccountSnapshot> result = new ArrayList<AccountSnapshot>(byKey.size());
		int negative = 0;
		for (AccountSnapshot account : byKey.values()) {
			AccountSnapshot scaled = scale(account, money, logger);
			if (scaled.getBalance().signum() < 0)
				negative++;
			result.add(scaled);
		}
		if (negative > 0)
			logger.warning("Importação: " + negative + " conta(s) com saldo negativo (versões antigas permitiam "
					+ "sacar mais do que o saldo). Foram mantidas como estão; ajuste com /money set se quiser.");
		return result;
	}

	private static BigDecimal parseBalance(LegacyAccount row, Logger logger) {
		try {
			return new BigDecimal(row.getRawBalance().trim());
		} catch (RuntimeException exception) {
			logger.warning("Importação: conta '" + row.getName() + "' ignorada, saldo ilegível '"
					+ row.getRawBalance() + "'.");
			return null;
		}
	}

	private static AccountSnapshot scale(AccountSnapshot account, MoneySettings money, Logger logger) {
		BigDecimal balance = money.credit(account.getBalance());
		if (money.exceedsLimit(balance)) {
			logger.warning("Importação: o saldo de '" + account.getDisplayName() + "' ("
					+ balance.toPlainString() + ") excede o máximo permitido e foi reduzido a "
					+ money.getMaxBalance().toPlainString() + ".");
			balance = money.credit(money.getMaxBalance());
		}
		return new AccountSnapshot(account.getKey(), account.getDisplayName(), balance, account.isReceivingDisabled());
	}
}
