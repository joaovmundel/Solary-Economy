package com.redeskyller.bukkit.solaryeconomy.message;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.bukkit.ChatColor;
import org.bukkit.configuration.Configuration;

/**
 * Looks up messages from mensagens.yml. A key the admin's file lacks falls back to the text shipped
 * in the jar (the file is loaded with those defaults), so upgrading never produces a blank or a
 * {@code NullPointerException}; a key missing from both is reported once and shown as its name.
 */
public final class Messages {

	private final Logger logger;
	private final Set<Message> reportedMissing = Collections
			.newSetFromMap(new ConcurrentHashMap<Message, Boolean>());
	private volatile Configuration source;

	public Messages(Configuration source, Logger logger) {
		this.source = source;
		this.logger = logger;
	}

	/** Swaps the loaded file (reload). */
	public void replace(Configuration newSource) {
		this.source = newSource;
		this.reportedMissing.clear();
	}

	/**
	 * @param replacements alternating placeholder names and values: {@code "player", "Steve", "valor", "10"}
	 *            replaces {@code {player}} and {@code {valor}}
	 */
	public String get(Message message, String... replacements) {
		String text = this.source.getString(message.name());
		if (text == null) {
			if (this.reportedMissing.add(message))
				this.logger.warning("Mensagem '" + message.name() + "' não existe em mensagens.yml nem nos padrões.");
			text = "<" + message.name() + ">";
		}

		// colors first: values (player names!) must never be interpreted as color codes
		text = ChatColor.translateAlternateColorCodes('&', text);
		for (int i = 0; i + 1 < replacements.length; i += 2)
			text = text.replace("{" + replacements[i] + "}", String.valueOf(replacements[i + 1]));
		return text;
	}
}
