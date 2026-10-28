package com.redeskyller.bukkit.solaryeconomy;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import com.redeskyller.bukkit.solaryeconomy.config.PluginSettings;
import com.redeskyller.bukkit.solaryeconomy.config.SettingsHolder;
import com.redeskyller.bukkit.solaryeconomy.config.SettingsLoader;

/** Shared fixtures: the settings shipped in the jar (optionally overridden) and a logger that records. */
public final class TestSupport {

	private TestSupport() {
	}

	/** A logger that keeps what it is given, so tests can assert on warnings. */
	public static final class RecordingLogger extends Logger {

		private final List<LogRecord> records = Collections.synchronizedList(new ArrayList<LogRecord>());

		public RecordingLogger() {
			super("test-" + System.nanoTime(), null);
			setUseParentHandlers(false);
			setLevel(Level.ALL);
			addHandler(new Handler() {
				@Override
				public void publish(LogRecord record) {
					RecordingLogger.this.records.add(record);
				}

				@Override
				public void flush() {
				}

				@Override
				public void close() {
				}
			});
		}

		public List<LogRecord> records() {
			synchronized (this.records) {
				return new ArrayList<LogRecord>(this.records);
			}
		}

		public int count(Level level) {
			int count = 0;
			for (LogRecord record : records())
				if (record.getLevel().equals(level))
					count++;
			return count;
		}

		public boolean contains(Level level, String fragment) {
			for (LogRecord record : records())
				if (record.getLevel().equals(level) && record.getMessage().contains(fragment))
					return true;
			return false;
		}
	}

	public static RecordingLogger logger() {
		return new RecordingLogger();
	}

	/** The config.yml shipped in the jar, exactly as an admin would get it. */
	public static YamlConfiguration bundledConfig() {
		return load("/config.yml");
	}

	public static YamlConfiguration bundledMessages() {
		return load("/mensagens.yml");
	}

	/** The shipped config with the given paths overridden, registered with the shipped config as defaults. */
	public static YamlConfiguration config(Map<String, Object> overrides) {
		YamlConfiguration config = bundledConfig();
		config.setDefaults(bundledConfig());
		for (Map.Entry<String, Object> override : overrides.entrySet())
			config.set(override.getKey(), override.getValue());
		return config;
	}

	public static PluginSettings settings() {
		return settings(Collections.<String, Object>emptyMap());
	}

	public static PluginSettings settings(Map<String, Object> overrides) {
		return new SettingsLoader(logger()).load(config(overrides));
	}

	public static SettingsHolder holder() {
		return new SettingsHolder(settings());
	}

	public static SettingsHolder holder(Map<String, Object> overrides) {
		return new SettingsHolder(settings(overrides));
	}

	private static YamlConfiguration load(String resource) {
		try (InputStream stream = TestSupport.class.getResourceAsStream(resource)) {
			if (stream == null)
				throw new IllegalStateException("missing test resource " + resource);
			ByteArrayOutputStream buffer = new ByteArrayOutputStream();
			byte[] chunk = new byte[4096];
			int read;
			while ((read = stream.read(chunk)) != -1)
				buffer.write(chunk, 0, read);

			YamlConfiguration yaml = new YamlConfiguration();
			yaml.loadFromString(new String(buffer.toByteArray(), Charset.forName("UTF-8")));
			return yaml;
		} catch (IOException | InvalidConfigurationException exception) {
			throw new IllegalStateException(exception);
		}
	}
}
