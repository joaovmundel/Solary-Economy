package com.redeskyller.bukkit.solaryeconomy;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import com.redeskyller.bukkit.solaryeconomy.audit.AuditLog;
import com.redeskyller.bukkit.solaryeconomy.command.MoneyCommand;
import com.redeskyller.bukkit.solaryeconomy.command.Reloadable;
import com.redeskyller.bukkit.solaryeconomy.command.sub.AddCommand;
import com.redeskyller.bukkit.solaryeconomy.command.sub.CreateCommand;
import com.redeskyller.bukkit.solaryeconomy.command.sub.DeleteCommand;
import com.redeskyller.bukkit.solaryeconomy.command.sub.HelpCommand;
import com.redeskyller.bukkit.solaryeconomy.command.sub.MagnateCommand;
import com.redeskyller.bukkit.solaryeconomy.command.sub.PayCommand;
import com.redeskyller.bukkit.solaryeconomy.command.sub.ReloadCommand;
import com.redeskyller.bukkit.solaryeconomy.command.sub.RemoveCommand;
import com.redeskyller.bukkit.solaryeconomy.command.sub.SetCommand;
import com.redeskyller.bukkit.solaryeconomy.command.sub.ToggleCommand;
import com.redeskyller.bukkit.solaryeconomy.command.sub.TopCommand;
import com.redeskyller.bukkit.solaryeconomy.config.ConfigException;
import com.redeskyller.bukkit.solaryeconomy.config.ConfigFiles;
import com.redeskyller.bukkit.solaryeconomy.config.PluginSettings;
import com.redeskyller.bukkit.solaryeconomy.config.SettingsHolder;
import com.redeskyller.bukkit.solaryeconomy.config.SettingsLoader;
import com.redeskyller.bukkit.solaryeconomy.economy.AccountSnapshot;
import com.redeskyller.bukkit.solaryeconomy.economy.EconomyService;
import com.redeskyller.bukkit.solaryeconomy.economy.EconomyServiceImpl;
import com.redeskyller.bukkit.solaryeconomy.economy.LeaderboardService;
import com.redeskyller.bukkit.solaryeconomy.format.AmountParser;
import com.redeskyller.bukkit.solaryeconomy.format.CurrencyFormatter;
import com.redeskyller.bukkit.solaryeconomy.hook.Hook;
import com.redeskyller.bukkit.solaryeconomy.hook.LegendchatHook;
import com.redeskyller.bukkit.solaryeconomy.hook.MagnateAnnouncer;
import com.redeskyller.bukkit.solaryeconomy.hook.NoPrefixProvider;
import com.redeskyller.bukkit.solaryeconomy.hook.PlaceholdersHook;
import com.redeskyller.bukkit.solaryeconomy.hook.PrefixProvider;
import com.redeskyller.bukkit.solaryeconomy.hook.VaultEconomyProvider;
import com.redeskyller.bukkit.solaryeconomy.hook.VaultHook;
import com.redeskyller.bukkit.solaryeconomy.hook.VaultPrefixProvider;
import com.redeskyller.bukkit.solaryeconomy.listener.AccountListener;
import com.redeskyller.bukkit.solaryeconomy.log.DebugLog;
import com.redeskyller.bukkit.solaryeconomy.message.Messages;
import com.redeskyller.bukkit.solaryeconomy.storage.DeletedAccountPersister;
import com.redeskyller.bukkit.solaryeconomy.storage.EmergencyDump;
import com.redeskyller.bukkit.solaryeconomy.storage.Storage;
import com.redeskyller.bukkit.solaryeconomy.storage.StorageException;
import com.redeskyller.bukkit.solaryeconomy.storage.StorageExecutor;
import com.redeskyller.bukkit.solaryeconomy.storage.StorageInitializer;
import com.redeskyller.bukkit.solaryeconomy.storage.WriteBehindFlusher;
import com.redeskyller.bukkit.solaryeconomy.update.UpdateChecker;

/**
 * Composition root: the one place that knows every concrete class and wires them together. The
 * plugin class only starts and stops it, and nothing else in the code base reaches for global state.
 * <p>
 * {@link #stop()} is safe after a partial {@link #start()}, which is how a failed startup cleans up.
 * Everything it creates is released, so {@code /reload} and PlugMan can enable the plugin again.
 */
public final class EconomyRuntime implements Reloadable {

	private static final int STARTUP_TIMEOUT_SECONDS = 120;
	private static final int SHUTDOWN_TIMEOUT_SECONDS = 30;
	private static final long TICKS_PER_SECOND = 20L;

	private final JavaPlugin plugin;
	private final Logger logger;
	private final List<Hook> hooks = new ArrayList<Hook>();

	private SettingsHolder settings;
	private Messages messages;
	private CurrencyFormatter formatter;
	private PrefixProvider prefixes = NoPrefixProvider.INSTANCE;
	private StorageExecutor storageExecutor;
	private Storage storage;
	private EconomyServiceImpl economy;
	private WriteBehindFlusher flusher;
	private ScheduledFuture<?> flushTask;
	private LeaderboardService leaderboard;
	private BukkitTask leaderboardTask;
	private AuditLog auditLog;

	public EconomyRuntime(JavaPlugin plugin) {
		this.plugin = plugin;
		this.logger = plugin.getLogger();
	}

	public void start() throws ConfigException, StorageException {
		try {
			loadConfiguration();
			openStorage();
			startBackgroundWork();
			installHooks();
			registerCommand();
			registerListeners();
			checkForUpdates();
		} catch (ConfigException | StorageException | RuntimeException exception) {
			stop();
			throw exception;
		}
	}

	/** Releases everything in reverse order. Never throws, so one failing step cannot block the rest. */
	public synchronized void stop() {
		Bukkit.getScheduler().cancelTasks(this.plugin);
		uninstallHooks();
		flushAndCloseStorage();

		if (this.auditLog != null)
			this.auditLog.close();
		this.flushTask = null;
		this.leaderboardTask = null;
		this.economy = null;
		this.leaderboard = null;
	}

	@Override
	public synchronized void reload() throws ConfigException {
		// everything is read before anything is applied, so a broken file changes nothing
		YamlConfiguration config = ConfigFiles.load(this.plugin, "config.yml", this.logger,
				SettingsLoader.DICTIONARY_PATH);
		YamlConfiguration language = ConfigFiles.load(this.plugin, "mensagens.yml", this.logger);

		PluginSettings previous = this.settings.current();
		PluginSettings updated = new SettingsLoader(this.logger).load(config);
		this.settings.replace(updated);
		this.messages.replace(language);

		if (!previous.getDatabase().equals(updated.getDatabase()))
			this.logger.warning("As configurações do banco de dados (mysql.*) só passam a valer depois de reiniciar o servidor.");
		if (previous.getGeneral().isUseVault() != updated.getGeneral().isUseVault())
			this.logger.warning("'use_vault' só passa a valer depois de reiniciar o servidor.");

		this.auditLog.setEnabled(updated.getGeneral().isAuditLog());
		scheduleFlush();
		scheduleLeaderboardRefresh();
		this.leaderboard.refresh();
		this.logger.info("Configurações recarregadas.");
	}

	public EconomyService getEconomy() {
		return this.economy;
	}

	public LeaderboardService getLeaderboard() {
		return this.leaderboard;
	}

	public CurrencyFormatter getFormatter() {
		return this.formatter;
	}

	private void loadConfiguration() throws ConfigException {
		YamlConfiguration config = ConfigFiles.load(this.plugin, "config.yml", this.logger,
				SettingsLoader.DICTIONARY_PATH);
		this.settings = new SettingsHolder(new SettingsLoader(this.logger).load(config));
		this.messages = new Messages(ConfigFiles.load(this.plugin, "mensagens.yml", this.logger), this.logger);
		this.formatter = new CurrencyFormatter(this.settings);
	}

	private void openStorage() throws StorageException {
		PluginSettings current = this.settings.current();
		this.logger.info("Banco de dados: " + current.getDatabase());

		this.economy = new EconomyServiceImpl(this.settings, this.logger);
		this.storageExecutor = new StorageExecutor(this.logger);
		this.storage = Storage.create(current.getDatabase(), this.plugin.getDataFolder(), this.logger);

		final StorageInitializer initializer = new StorageInitializer(this.storage.getRepository(),
				this.storage.getLegacySource(), current.getMoney(), this.logger);
		List<AccountSnapshot> accounts = this.storageExecutor.call("Inicialização do banco de dados",
				new Callable<List<AccountSnapshot>>() {
					@Override
					public List<AccountSnapshot> call() throws StorageException {
						return initializer.initializeAndLoad();
					}
				}, STARTUP_TIMEOUT_SECONDS);
		this.economy.load(accounts);
	}

	private void startBackgroundWork() {
		this.economy.addListener(new DeletedAccountPersister(this.storage.getRepository(), this.storageExecutor));

		this.auditLog = new AuditLog(this.plugin.getDataFolder(), this.logger);
		this.auditLog.setEnabled(this.settings.current().getGeneral().isAuditLog());
		this.economy.addListener(this.auditLog);

		this.flusher = new WriteBehindFlusher(this.economy, this.storage.getRepository(), this.logger,
				new DebugLog(this.logger, this.settings));
		scheduleFlush();

		this.leaderboard = new LeaderboardService(this.economy, this.settings, this.logger);
		this.leaderboard.refresh();
		scheduleLeaderboardRefresh();
	}

	private void scheduleFlush() {
		if (this.flushTask != null)
			this.flushTask.cancel(false);
		this.flushTask = this.storageExecutor.scheduleWithFixedDelay("Gravação das contas", this.flusher,
				this.settings.current().getGeneral().getSaveIntervalSeconds());
	}

	private void scheduleLeaderboardRefresh() {
		if (this.leaderboardTask != null)
			this.leaderboardTask.cancel();

		final LeaderboardService board = this.leaderboard;
		final Logger log = this.logger;
		long period = this.settings.current().getLeaderboard().getRefreshSeconds() * TICKS_PER_SECOND;
		this.leaderboardTask = Bukkit.getScheduler().runTaskTimerAsynchronously(this.plugin, new Runnable() {
			@Override
			public void run() {
				try {
					board.refresh();
				} catch (RuntimeException exception) {
					log.log(Level.SEVERE, "Não foi possível atualizar o money top.", exception);
				}
			}
		}, period, period);
	}

	private void installHooks() {
		Plugin vault = Bukkit.getPluginManager().getPlugin("Vault");
		if (vault != null) {
			this.prefixes = new VaultPrefixProvider(this.settings, this.logger);
			if (this.settings.current().getGeneral().isUseVault())
				installVaultEconomy();
		} else if (this.settings.current().getGeneral().isUseVault()) {
			this.logger.warning("'use_vault' está ativo, mas o plugin Vault não foi encontrado: outros plugins não "
					+ "conseguirão usar esta economia.");
		}

		this.leaderboard.addListener(new MagnateAnnouncer(this.plugin, this.messages, this.formatter, this.settings,
				this.prefixes));

		if (Bukkit.getPluginManager().getPlugin("Legendchat") != null)
			installLegendchat();
		if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null)
			installPlaceholders();
	}

	// Each optional integration is isolated: a version mismatch there must not take the economy down.

	private void installVaultEconomy() {
		try {
			VaultEconomyProvider provider = new VaultEconomyProvider(this.plugin, this.economy, this.settings,
					this.formatter);
			this.hooks.add(VaultHook.install(this.plugin, provider));
			this.logger.info("Economia registrada no Vault.");
		} catch (RuntimeException | LinkageError error) {
			this.logger.log(Level.SEVERE, "Não foi possível registrar a economia no Vault.", error);
		}
	}

	private void installLegendchat() {
		try {
			this.hooks.add(LegendchatHook.install(this.plugin, this.leaderboard, this.messages, this.settings));
			this.logger.info("Integrado ao Legendchat (tag {solary_economy_magnata}).");
		} catch (RuntimeException | LinkageError error) {
			this.logger.log(Level.WARNING, "Legendchat encontrado, mas a integração falhou.", error);
		}
	}

	private void installPlaceholders() {
		try {
			this.hooks.add(PlaceholdersHook.install(this.plugin, this.economy, this.formatter));
			this.logger.info("Integrado ao PlaceholderAPI.");
		} catch (RuntimeException | LinkageError error) {
			this.logger.log(Level.WARNING, "PlaceholderAPI encontrado, mas a integração falhou.", error);
		}
	}

	private void registerCommand() {
		PluginCommand money = this.plugin.getCommand("money");
		if (money == null)
			throw new IllegalStateException("O comando 'money' não está declarado no plugin.yml.");

		AmountParser parser = new AmountParser(this.settings);
		MoneyCommand command = new MoneyCommand(this.economy, this.formatter, this.messages, this.logger);
		command.register(new HelpCommand(this.messages, command))
				.register(new TopCommand(this.messages, this.leaderboard, this.formatter, this.settings, this.prefixes))
				.register(new CreateCommand(this.messages, this.economy, parser))
				.register(new DeleteCommand(this.messages, this.economy))
				.register(new AddCommand(this.messages, this.economy, parser, this.formatter, this.settings))
				.register(new RemoveCommand(this.messages, this.economy, parser, this.formatter, this.settings))
				.register(new SetCommand(this.messages, this.economy, parser, this.formatter, this.settings))
				.register(new PayCommand(this.messages, this.economy, parser, this.formatter, this.settings))
				.register(new ToggleCommand(this.messages, this.economy))
				.register(new ReloadCommand(this.messages, this, this.logger))
				.register(new MagnateCommand(this.messages, this.leaderboard, this.formatter, this.settings,
						this.prefixes));
		money.setExecutor(command);
		money.setTabCompleter(command);
	}

	private void registerListeners() {
		AccountListener listener = new AccountListener(this.economy, this.logger);
		Bukkit.getPluginManager().registerEvents(listener, this.plugin);

		// players already online (plugin enabled after they joined) get their account too
		for (World world : Bukkit.getWorlds())
			for (Player player : world.getPlayers())
				listener.ensureAccount(player.getName());
	}

	private void checkForUpdates() {
		if (this.settings.current().getGeneral().isCheckUpdates())
			Bukkit.getScheduler().runTaskAsynchronously(this.plugin,
					new UpdateChecker(this.plugin.getDescription().getVersion(), this.logger));
	}

	private void uninstallHooks() {
		List<Hook> installed = new ArrayList<Hook>(this.hooks);
		Collections.reverse(installed);
		for (Hook hook : installed) {
			try {
				hook.uninstall();
			} catch (RuntimeException | LinkageError error) {
				this.logger.log(Level.WARNING, "Falha ao remover uma integração.", error);
			}
		}
		this.hooks.clear();
	}

	/** The final write: whatever is still pending goes to the database before the connection closes. */
	private void flushAndCloseStorage() {
		if (this.storageExecutor == null)
			return;

		if (this.flusher != null) {
			try {
				final WriteBehindFlusher lastFlush = this.flusher;
				this.storageExecutor.call("Gravação final das contas", new Callable<Void>() {
					@Override
					public Void call() {
						lastFlush.run();
						return null;
					}
				}, SHUTDOWN_TIMEOUT_SECONDS);
			} catch (StorageException exception) {
				this.logger.log(Level.SEVERE, "Não foi possível concluir a gravação final das contas.", exception);
			}
			rescueUnsavedAccounts();
		}

		this.storageExecutor.shutdown(SHUTDOWN_TIMEOUT_SECONDS);
		if (this.storage != null)
			this.storage.getRepository().close();
		this.storageExecutor = null;
		this.storage = null;
		this.flusher = null;
	}

	/** If the final write failed the changes are still pending: save them to a file instead of losing them. */
	private void rescueUnsavedAccounts() {
		if (this.economy == null)
			return;

		List<AccountSnapshot> unsaved = this.economy.drainDirty();
		if (unsaved.isEmpty())
			return;

		try {
			File dump = EmergencyDump.write(this.plugin.getDataFolder(), unsaved);
			this.logger.severe(unsaved.size() + " conta(s) não puderam ser gravadas no banco de dados. Os saldos foram "
					+ "salvos em " + dump.getPath() + " para recuperação manual.");
		} catch (IOException exception) {
			this.logger.log(Level.SEVERE, unsaved.size() + " conta(s) não puderam ser gravadas e o arquivo de "
					+ "emergência também falhou. Saldos perdidos: " + unsaved, exception);
		}
	}
}
