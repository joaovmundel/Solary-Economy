package com.redeskyller.bukkit.solaryeconomy.economy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** The money top as of one refresh. Immutable, so it can be shared between threads freely. */
public final class Leaderboard {

	public static final Leaderboard EMPTY = new Leaderboard(Collections.<RankAccount>emptyList());

	private final List<RankAccount> entries;

	public Leaderboard(List<RankAccount> entries) {
		this.entries = Collections.unmodifiableList(new ArrayList<RankAccount>(entries));
	}

	/** Richest first. */
	public List<RankAccount> getEntries() {
		return this.entries;
	}

	/** @return the richest account, or {@code null} when nobody is ranked */
	public RankAccount getMagnate() {
		return this.entries.isEmpty() ? null : this.entries.get(0);
	}
}
