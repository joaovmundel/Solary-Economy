package com.redeskyller.bukkit.solaryeconomy.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;

/**
 * Online players without {@code Bukkit.getOnlinePlayers()}, whose return type changed from an array
 * to a collection in 1.7/1.8 and would break the same jar on a different server version.
 */
final class PlayerNames {

	private PlayerNames() {
	}

	static List<String> online(String prefix) {
		String wanted = prefix.toLowerCase(Locale.ENGLISH);
		List<String> names = new ArrayList<String>();
		for (World world : Bukkit.getWorlds())
			for (Player player : world.getPlayers())
				if (player.getName().toLowerCase(Locale.ENGLISH).startsWith(wanted))
					names.add(player.getName());
		Collections.sort(names, String.CASE_INSENSITIVE_ORDER);
		return names;
	}
}
