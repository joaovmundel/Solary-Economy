package com.redeskyller.bukkit.solaryeconomy.config;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Loads YAML files as UTF-8 regardless of the platform charset (Bukkit 1.5.2 has no
 * {@code load(Reader)} and its stream overload uses the platform default), with the copy shipped
 * inside the jar registered as defaults, so a file written by an older version never yields nulls.
 */
public final class ConfigFiles {

	private static final Charset UTF_8 = Charset.forName("UTF-8");

	private ConfigFiles() {
	}

	/** Loads {@code fileName} from the plugin data folder, copying the bundled one there first if needed. */
	public static YamlConfiguration load(JavaPlugin plugin, String fileName, Logger logger, String... ignoredPrefixes)
			throws ConfigException {
		File file = new File(plugin.getDataFolder(), fileName);
		if (!file.exists()) {
			try {
				plugin.getDataFolder().mkdirs();
				plugin.saveResource(fileName, false);
				logger.info("Arquivo " + fileName + " criado com as configurações padrão.");
			} catch (RuntimeException exception) {
				throw new ConfigException("Não foi possível criar " + fileName + ": " + exception.getMessage(),
						exception);
			}
		}

		InputStream bundled = plugin.getResource(fileName);
		try {
			return load(file, bundled, logger, ignoredPrefixes);
		} finally {
			closeQuietly(bundled);
		}
	}

	/**
	 * @param bundledDefaults the default copy of the file, may be {@code null}; not closed by this method
	 * @param ignoredPrefixes paths below these prefixes are free-form and never reported as missing
	 */
	public static YamlConfiguration load(File file, InputStream bundledDefaults, Logger logger,
			String... ignoredPrefixes) throws ConfigException {
		YamlConfiguration yaml = parse(readText(file), file.getName());

		if (bundledDefaults != null) {
			YamlConfiguration defaults;
			try {
				defaults = parse(readText(bundledDefaults), file.getName() + " (padrão do plugin)");
			} catch (IOException exception) {
				throw new ConfigException("Não foi possível ler o padrão embutido de " + file.getName(), exception);
			}
			yaml.setDefaults(defaults);
			reportMissingKeys(yaml, defaults, file.getName(), logger, ignoredPrefixes);
		}
		return yaml;
	}

	private static String readText(File file) throws ConfigException {
		try {
			return decode(Files.readAllBytes(file.toPath()));
		} catch (IOException exception) {
			throw new ConfigException("Não foi possível ler " + file.getPath() + ": " + exception.getMessage(),
					exception);
		}
	}

	private static String readText(InputStream stream) throws IOException {
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		byte[] chunk = new byte[4096];
		int read;
		while ((read = stream.read(chunk)) != -1)
			buffer.write(chunk, 0, read);
		return decode(buffer.toByteArray());
	}

	private static String decode(byte[] bytes) {
		String text = new String(bytes, UTF_8);
		return text.startsWith("﻿") ? text.substring(1) : text;
	}

	private static YamlConfiguration parse(String text, String name) throws ConfigException {
		YamlConfiguration yaml = new YamlConfiguration();
		try {
			yaml.loadFromString(text);
		} catch (InvalidConfigurationException exception) {
			throw new ConfigException(name + " não é um YAML válido: " + exception.getMessage(), exception);
		}
		return yaml;
	}

	private static void reportMissingKeys(YamlConfiguration yaml, YamlConfiguration defaults, String name,
			Logger logger, String[] ignoredPrefixes) {
		List<String> missing = new ArrayList<String>();
		for (String path : defaults.getKeys(true)) {
			if (defaults.isConfigurationSection(path) || isIgnored(path, ignoredPrefixes))
				continue;
			// isSet() ignores the defaults here: they are registered but not copied into the file
			if (!yaml.isSet(path))
				missing.add(path);
		}
		if (!missing.isEmpty())
			logger.info(name + ": usando o valor padrão para " + missing.size() + " chave(s) ausente(s): " + missing);
	}

	private static boolean isIgnored(String path, String[] ignoredPrefixes) {
		for (String prefix : ignoredPrefixes)
			if (path.equals(prefix) || path.startsWith(prefix + "."))
				return true;
		return false;
	}

	private static void closeQuietly(InputStream stream) {
		if (stream == null)
			return;
		try {
			stream.close();
		} catch (IOException ignored) {
			// nothing useful can be done about a failed close of a read-only stream
		}
	}
}
