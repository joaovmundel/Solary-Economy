package com.redeskyller.bukkit.solaryeconomy.economy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.redeskyller.bukkit.solaryeconomy.TestSupport;
import com.redeskyller.bukkit.solaryeconomy.config.SettingsHolder;

public class LeaderboardServiceTest {

	private static final class Announcements implements MagnateChangeListener {
		final List<String> changes = new ArrayList<String>();

		@Override
		public void onMagnateChanged(RankAccount previous, RankAccount current) {
			this.changes.add((previous == null ? "none" : previous.getName()) + "->" + current.getName());
		}
	}

	private static EconomyServiceImpl economyWith(SettingsHolder settings, String... nameAndBalance) {
		EconomyServiceImpl economy = new EconomyServiceImpl(settings, TestSupport.logger());
		for (int i = 0; i < nameAndBalance.length; i += 2)
			economy.createAccount(nameAndBalance[i], new BigDecimal(nameAndBalance[i + 1]));
		return economy;
	}

	@Test
	public void ranksRichestFirstAndBreaksTiesByName() {
		SettingsHolder settings = TestSupport.holder();
		EconomyServiceImpl economy = economyWith(settings, "Carl", "50", "Alice", "100", "Bob", "100", "Dave", "70");
		LeaderboardService board = new LeaderboardService(economy, settings, TestSupport.logger());

		List<RankAccount> entries = board.refresh().getEntries();

		assertEquals("Alice", entries.get(0).getName());
		assertEquals("Bob", entries.get(1).getName());
		assertEquals("Dave", entries.get(2).getName());
		assertEquals("Carl", entries.get(3).getName());
	}

	@Test
	public void keepsOnlyTheConfiguredNumberOfEntries() {
		Map<String, Object> overrides = new HashMap<String, Object>();
		overrides.put("economy_top.size", 2);
		SettingsHolder settings = TestSupport.holder(overrides);
		EconomyServiceImpl economy = economyWith(settings, "A", "1", "B", "2", "C", "3");
		LeaderboardService board = new LeaderboardService(economy, settings, TestSupport.logger());

		assertEquals(2, board.refresh().getEntries().size());
		assertEquals("C", board.current().getMagnate().getName());
	}

	@Test
	public void leavesOutAccountsWithLongNamesSuchAsFactions() {
		Map<String, Object> overrides = new HashMap<String, Object>();
		overrides.put("economy_top.name_size", 8);
		SettingsHolder settings = TestSupport.holder(overrides);
		EconomyServiceImpl economy = economyWith(settings, "Player", "10", "SuperLongFactionName", "999999");
		LeaderboardService board = new LeaderboardService(economy, settings, TestSupport.logger());

		List<RankAccount> entries = board.refresh().getEntries();

		assertEquals(1, entries.size());
		assertEquals("Player", entries.get(0).getName());
	}

	@Test
	public void startsEmptyWithNoMagnate() {
		SettingsHolder settings = TestSupport.holder();
		LeaderboardService board = new LeaderboardService(economyWith(settings), settings, TestSupport.logger());

		assertSame(Leaderboard.EMPTY, board.current());
		assertNull(board.current().getMagnate());
		assertTrue(board.refresh().getEntries().isEmpty());
	}

	@Test
	public void rankingIsFreshAfterBalancesChange() {
		SettingsHolder settings = TestSupport.holder();
		EconomyServiceImpl economy = economyWith(settings, "Alice", "10", "Bob", "5");
		LeaderboardService board = new LeaderboardService(economy, settings, TestSupport.logger());
		board.refresh();
		assertEquals("Alice", board.current().getMagnate().getName());

		economy.deposit("Bob", new BigDecimal("100"));

		assertEquals("Alice", board.current().getMagnate().getName()); // not refreshed yet, by design
		assertEquals("Bob", board.refresh().getMagnate().getName());
	}

	@Test
	public void announcesANewMagnateButNotTheFirstOneLoadedAtStartup() {
		SettingsHolder settings = TestSupport.holder();
		EconomyServiceImpl economy = economyWith(settings, "Alice", "10", "Bob", "5");
		LeaderboardService board = new LeaderboardService(economy, settings, TestSupport.logger());
		Announcements announcements = new Announcements();
		board.addListener(announcements);

		board.refresh(); // startup: Alice is simply who we loaded
		board.refresh(); // nothing changed
		assertTrue(announcements.changes.isEmpty());

		economy.deposit("Bob", new BigDecimal("100"));
		board.refresh();
		board.refresh(); // same magnate again: no repeat
		assertEquals(java.util.Arrays.asList("Alice->Bob"), announcements.changes);
	}

	@Test
	public void aFailingListenerDoesNotBreakTheRefresh() {
		SettingsHolder settings = TestSupport.holder();
		EconomyServiceImpl economy = economyWith(settings, "Alice", "10", "Bob", "5");
		LeaderboardService board = new LeaderboardService(economy, settings, TestSupport.logger());
		board.addListener(new MagnateChangeListener() {
			@Override
			public void onMagnateChanged(RankAccount previous, RankAccount current) {
				throw new IllegalStateException("boom");
			}
		});
		board.refresh();

		economy.deposit("Bob", new BigDecimal("100"));

		assertEquals("Bob", board.refresh().getMagnate().getName());
	}
}
