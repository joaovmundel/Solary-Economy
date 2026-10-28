package com.redeskyller.bukkit.solaryeconomy.hook;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.Plugin;

import com.redeskyller.bukkit.solaryeconomy.config.SettingsProvider;
import com.redeskyller.bukkit.solaryeconomy.economy.EconomyService;
import com.redeskyller.bukkit.solaryeconomy.economy.TransactionResult;
import com.redeskyller.bukkit.solaryeconomy.format.CurrencyFormatter;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import net.milkbowl.vault.economy.EconomyResponse.ResponseType;

/**
 * Vault's view of the economy, used by shops, jobs, factions and the like. Other plugins call this
 * from any thread and with doubles, so every amount is validated here: NaN, infinity and negative
 * values are refused, and a withdrawal can never overdraw an account.
 * <p>
 * The {@link OfflinePlayer} overloads belong to Vault 1.4+. They have no {@code @Override} on purpose:
 * Vault 1.2.x (the one that runs on 1.5.2) does not declare them, so the class must compile against
 * both. They resolve the account by player <em>name</em>, the only identity a 1.5.2 server has.
 */
public final class VaultEconomyProvider implements Economy {

	private final Plugin plugin;
	private final EconomyService economy;
	private final SettingsProvider settings;
	private final CurrencyFormatter formatter;

	public VaultEconomyProvider(Plugin plugin, EconomyService economy, SettingsProvider settings,
			CurrencyFormatter formatter) {
		this.plugin = plugin;
		this.economy = economy;
		this.settings = settings;
		this.formatter = formatter;
	}

	@Override
	public boolean isEnabled() {
		return this.plugin.isEnabled();
	}

	@Override
	public String getName() {
		return this.plugin.getDescription().getName();
	}

	@Override
	public boolean hasBankSupport() {
		return false;
	}

	@Override
	public int fractionalDigits() {
		return this.settings.current().getMoney().getDecimalPlaces();
	}

	@Override
	public String format(double amount) {
		BigDecimal value = toDecimal(amount);
		return this.formatter.format(value == null ? BigDecimal.ZERO : value);
	}

	@Override
	public String currencyNamePlural() {
		return this.settings.current().getCurrency().getPluralName();
	}

	@Override
	public String currencyNameSingular() {
		return this.settings.current().getCurrency().getSingularName();
	}

	@Override
	public boolean hasAccount(String name) {
		return this.economy.hasAccount(name);
	}

	@Override
	public boolean hasAccount(String name, String world) {
		return hasAccount(name);
	}

	public boolean hasAccount(OfflinePlayer player) {
		return hasAccount(nameOf(player));
	}

	public boolean hasAccount(OfflinePlayer player, String world) {
		return hasAccount(nameOf(player));
	}

	@Override
	public double getBalance(String name) {
		return this.economy.getBalance(name).doubleValue();
	}

	@Override
	public double getBalance(String name, String world) {
		return getBalance(name);
	}

	public double getBalance(OfflinePlayer player) {
		return getBalance(nameOf(player));
	}

	public double getBalance(OfflinePlayer player, String world) {
		return getBalance(nameOf(player));
	}

	@Override
	public boolean has(String name, double amount) {
		BigDecimal value = toDecimal(amount);
		return value != null && this.economy.has(name, value);
	}

	@Override
	public boolean has(String name, String world, double amount) {
		return has(name, amount);
	}

	public boolean has(OfflinePlayer player, double amount) {
		return has(nameOf(player), amount);
	}

	public boolean has(OfflinePlayer player, String world, double amount) {
		return has(nameOf(player), amount);
	}

	@Override
	public EconomyResponse withdrawPlayer(String name, double amount) {
		BigDecimal value = toDecimal(amount);
		TransactionResult result = value == null ? TransactionResult.INVALID_AMOUNT
				: this.economy.withdraw(name, value);
		return respond(amount, name, result);
	}

	@Override
	public EconomyResponse withdrawPlayer(String name, String world, double amount) {
		return withdrawPlayer(name, amount);
	}

	public EconomyResponse withdrawPlayer(OfflinePlayer player, double amount) {
		return withdrawPlayer(nameOf(player), amount);
	}

	public EconomyResponse withdrawPlayer(OfflinePlayer player, String world, double amount) {
		return withdrawPlayer(nameOf(player), amount);
	}

	@Override
	public EconomyResponse depositPlayer(String name, double amount) {
		BigDecimal value = toDecimal(amount);
		TransactionResult result = value == null ? TransactionResult.INVALID_AMOUNT
				: this.economy.deposit(name, value);
		return respond(amount, name, result);
	}

	@Override
	public EconomyResponse depositPlayer(String name, String world, double amount) {
		return depositPlayer(name, amount);
	}

	public EconomyResponse depositPlayer(OfflinePlayer player, double amount) {
		return depositPlayer(nameOf(player), amount);
	}

	public EconomyResponse depositPlayer(OfflinePlayer player, String world, double amount) {
		return depositPlayer(nameOf(player), amount);
	}

	@Override
	public boolean createPlayerAccount(String name) {
		return this.economy.createAccount(name).isCreated();
	}

	@Override
	public boolean createPlayerAccount(String name, String world) {
		return createPlayerAccount(name);
	}

	public boolean createPlayerAccount(OfflinePlayer player) {
		return createPlayerAccount(nameOf(player));
	}

	public boolean createPlayerAccount(OfflinePlayer player, String world) {
		return createPlayerAccount(nameOf(player));
	}

	// ---- banks are not supported; Vault expects NOT_IMPLEMENTED, never null ----

	@Override
	public EconomyResponse createBank(String name, String player) {
		return noBanks();
	}

	public EconomyResponse createBank(String name, OfflinePlayer player) {
		return noBanks();
	}

	@Override
	public EconomyResponse deleteBank(String name) {
		return noBanks();
	}

	@Override
	public EconomyResponse bankBalance(String name) {
		return noBanks();
	}

	@Override
	public EconomyResponse bankHas(String name, double amount) {
		return noBanks();
	}

	@Override
	public EconomyResponse bankWithdraw(String name, double amount) {
		return noBanks();
	}

	@Override
	public EconomyResponse bankDeposit(String name, double amount) {
		return noBanks();
	}

	@Override
	public EconomyResponse isBankOwner(String name, String player) {
		return noBanks();
	}

	public EconomyResponse isBankOwner(String name, OfflinePlayer player) {
		return noBanks();
	}

	@Override
	public EconomyResponse isBankMember(String name, String player) {
		return noBanks();
	}

	public EconomyResponse isBankMember(String name, OfflinePlayer player) {
		return noBanks();
	}

	@Override
	public List<String> getBanks() {
		return Collections.emptyList();
	}

	private EconomyResponse respond(double requested, String name, TransactionResult result) {
		double balance = getBalance(name);
		if (result.isSuccess())
			return new EconomyResponse(requested, balance, ResponseType.SUCCESS, "");
		return new EconomyResponse(0, balance, ResponseType.FAILURE, describe(result));
	}

	private static String describe(TransactionResult result) {
		switch (result) {
		case ACCOUNT_NOT_FOUND:
			return "Conta inexistente.";
		case INSUFFICIENT_FUNDS:
			return "Saldo insuficiente.";
		case BALANCE_LIMIT_EXCEEDED:
			return "O saldo máximo da conta seria ultrapassado.";
		default:
			return "Valor inválido.";
		}
	}

	private static EconomyResponse noBanks() {
		return new EconomyResponse(0, 0, ResponseType.NOT_IMPLEMENTED, "O Solary-Economy não suporta bancos.");
	}

	/** @return the exact decimal of {@code amount}, or {@code null} for NaN, infinity or a negative value */
	private static BigDecimal toDecimal(double amount) {
		if (Double.isNaN(amount) || Double.isInfinite(amount) || amount < 0)
			return null;
		return BigDecimal.valueOf(amount);
	}

	private static String nameOf(OfflinePlayer player) {
		return player == null ? null : player.getName();
	}
}
