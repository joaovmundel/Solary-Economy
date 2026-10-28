package com.redeskyller.bukkit.solaryeconomy.economy;

public interface MagnateChangeListener {

	/**
	 * Called by {@link LeaderboardService#refresh()} on whichever thread ran the refresh.
	 *
	 * @param previous the former magnate, may be {@code null}
	 * @param current the new magnate, never {@code null}
	 */
	void onMagnateChanged(RankAccount previous, RankAccount current);
}
