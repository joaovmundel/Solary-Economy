package com.redeskyller.bukkit.solaryeconomy.economy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.redeskyller.bukkit.solaryeconomy.config.LeaderboardSettings;
import com.redeskyller.bukkit.solaryeconomy.config.SettingsProvider;

/**
 * Builds the money top straight from the in-memory accounts: exact (no SQL {@code CAST}), always
 * current and independent from when the database was last written.
 */
public final class LeaderboardService implements LeaderboardView {

	private static final Comparator<RankAccount> RICHEST_FIRST = new Comparator<RankAccount>() {
		@Override
		public int compare(RankAccount a, RankAccount b) {
			int byBalance = b.getBalance().compareTo(a.getBalance());
			return byBalance != 0 ? byBalance : a.getKey().compareTo(b.getKey());
		}
	};

	private final EconomyService economy;
	private final SettingsProvider settings;
	private final Logger logger;
	private final List<MagnateChangeListener> listeners = new CopyOnWriteArrayList<MagnateChangeListener>();

	private volatile Leaderboard current = Leaderboard.EMPTY;
	private volatile boolean initialized;

	public LeaderboardService(EconomyService economy, SettingsProvider settings, Logger logger) {
		this.economy = economy;
		this.settings = settings;
		this.logger = logger;
	}

	@Override
	public Leaderboard current() {
		return this.current;
	}

	public void addListener(MagnateChangeListener listener) {
		this.listeners.add(listener);
	}

	/**
	 * Recomputes the top. Listeners are told about a new magnate except on the very first refresh
	 * (server start), when "new" would only mean "the one we just loaded".
	 */
	public synchronized Leaderboard refresh() {
		LeaderboardSettings config = this.settings.current().getLeaderboard();

		List<RankAccount> ranked = new ArrayList<RankAccount>();
		for (AccountSnapshot account : this.economy.getAccounts())
			if (account.getDisplayName().length() <= config.getMaxNameLength())
				ranked.add(new RankAccount(account.getKey(), account.getDisplayName(), account.getBalance()));

		Collections.sort(ranked, RICHEST_FIRST);
		if (ranked.size() > config.getSize())
			ranked = new ArrayList<RankAccount>(ranked.subList(0, config.getSize()));

		Leaderboard previous = this.current;
		Leaderboard updated = new Leaderboard(ranked);
		this.current = updated;

		boolean wasInitialized = this.initialized;
		this.initialized = true;
		if (wasInitialized)
			notifyIfMagnateChanged(previous.getMagnate(), updated.getMagnate());
		return updated;
	}

	private void notifyIfMagnateChanged(RankAccount previous, RankAccount current) {
		if (current == null || (previous != null && previous.getKey().equals(current.getKey())))
			return;

		for (MagnateChangeListener listener : this.listeners) {
			try {
				listener.onMagnateChanged(previous, current);
			} catch (RuntimeException exception) {
				this.logger.log(Level.WARNING, "Falha ao avisar sobre o novo magnata " + current.getName(), exception);
			}
		}
	}
}
