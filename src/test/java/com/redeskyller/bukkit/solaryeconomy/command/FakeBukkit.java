package com.redeskyller.bukkit.solaryeconomy.command;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Just enough of a Bukkit server and of players for the command tests, built with dynamic proxies so
 * the tests run without a Minecraft server (the Bukkit API classes only need to be on the classpath).
 */
final class FakeBukkit {

	private static final Map<String, Player> ONLINE = new HashMap<String, Player>();
	private static boolean installed;

	private FakeBukkit() {
	}

	/** Installs the fake server once per JVM ({@code Bukkit.setServer} refuses a second one). */
	static synchronized void install() {
		if (installed)
			return;

		Bukkit.setServer((Server) Proxy.newProxyInstance(Server.class.getClassLoader(), new Class<?>[] { Server.class },
				new InvocationHandler() {
					@Override
					public Object invoke(Object proxy, Method method, Object[] args) {
						String name = method.getName();
						if (name.equals("getLogger"))
							return Logger.getLogger("FakeBukkit");
						if (name.equals("getName") || name.equals("getVersion") || name.equals("getBukkitVersion"))
							return "fake";
						if (name.equals("getPlayerExact"))
							return online((String) args[0]);
						if (name.equals("getWorlds"))
							return Collections.emptyList();
						if (name.equals("hashCode"))
							return System.identityHashCode(proxy);
						if (name.equals("equals"))
							return proxy == args[0];
						if (name.equals("toString"))
							return "FakeServer";
						return defaultValue(method.getReturnType());
					}
				}));
		installed = true;
	}

	static synchronized Player online(String name) {
		return ONLINE.get(name.toLowerCase(Locale.ENGLISH));
	}

	static synchronized void clearOnline() {
		ONLINE.clear();
	}

	/** The console: every permission, and it records what it was told. */
	static Sender console() {
		return new Sender("CONSOLE", false, null);
	}

	/** A player holding exactly the given permissions; it is also registered as online. */
	static Sender player(String name, String... permissions) {
		Sender sender = new Sender(name, true, new HashSet<String>(java.util.Arrays.asList(permissions)));
		synchronized (FakeBukkit.class) {
			ONLINE.put(name.toLowerCase(Locale.ENGLISH), (Player) sender.proxy);
		}
		return sender;
	}

	/** A command sender (console or player) that remembers every message. */
	static final class Sender {
		final List<String> messages = new ArrayList<String>();
		final Object proxy;

		private Sender(final String name, boolean player, final Set<String> permissions) {
			final Sender self = this;
			InvocationHandler handler = new InvocationHandler() {
				@Override
				public Object invoke(Object proxy, Method method, Object[] args) {
					String method_ = method.getName();
					if (method_.equals("getName"))
						return name;
					if (method_.equals("hasPermission"))
						return permissions == null || permissions.contains(args[0]);
					if (method_.equals("sendMessage")) {
						if (args[0] instanceof String[])
							for (String line : (String[]) args[0])
								self.messages.add(line);
						else
							self.messages.add((String) args[0]);
						return null;
					}
					if (method_.equals("hashCode"))
						return System.identityHashCode(proxy);
					if (method_.equals("equals"))
						return proxy == args[0];
					if (method_.equals("toString"))
						return name;
					return defaultValue(method.getReturnType());
				}
			};
			Class<?>[] types = player ? new Class<?>[] { Player.class } : new Class<?>[] { CommandSender.class };
			this.proxy = Proxy.newProxyInstance(Player.class.getClassLoader(), types, handler);
		}

		CommandSender sender() {
			return (CommandSender) this.proxy;
		}

		/** The last message without color codes or trailing spaces (some shipped messages end in one). */
		String last() {
			return strip(this.messages.get(this.messages.size() - 1)).trim();
		}

		List<String> plain() {
			List<String> lines = new ArrayList<String>();
			for (String message : this.messages)
				lines.add(strip(message).trim());
			return lines;
		}
	}

	static String strip(String text) {
		return text.replaceAll("§[0-9a-fk-or]", "");
	}

	private static Object defaultValue(Class<?> type) {
		if (type == boolean.class)
			return Boolean.FALSE;
		if (type == int.class)
			return Integer.valueOf(0);
		if (type == long.class)
			return Long.valueOf(0);
		if (type == double.class)
			return Double.valueOf(0);
		return null;
	}
}
