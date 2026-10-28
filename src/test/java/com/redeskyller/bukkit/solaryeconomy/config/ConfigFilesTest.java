package com.redeskyller.bukkit.solaryeconomy.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.logging.Level;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.redeskyller.bukkit.solaryeconomy.TestSupport;

public class ConfigFilesTest {

	private static final Charset UTF_8 = Charset.forName("UTF-8");

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	private File write(String name, byte[] content) throws IOException {
		File file = this.folder.newFile(name);
		Files.write(file.toPath(), content);
		return file;
	}

	private static ByteArrayInputStream stream(String text) {
		return new ByteArrayInputStream(text.getBytes(UTF_8));
	}

	@Test
	public void readsUtf8EvenWhenThePlatformCharsetIsDifferent() throws Exception {
		File file = write("mensagens.yml", "NO_PERMISSION: \"Voc\u00ea n\u00e3o tem permiss\u00e3o\"\n".getBytes(UTF_8));

		YamlConfiguration yaml = ConfigFiles.load(file, null, TestSupport.logger());

		assertEquals("Voc\u00ea n\u00e3o tem permiss\u00e3o", yaml.getString("NO_PERMISSION"));
	}

	@Test
	public void ignoresAByteOrderMarkLeftByWindowsEditors() throws Exception {
		byte[] bom = { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF };
		byte[] body = "size: 7\n".getBytes(UTF_8);
		byte[] content = new byte[bom.length + body.length];
		System.arraycopy(bom, 0, content, 0, bom.length);
		System.arraycopy(body, 0, content, bom.length, body.length);

		YamlConfiguration yaml = ConfigFiles.load(write("config.yml", content), null, TestSupport.logger());

		assertEquals(7, yaml.getInt("size"));
	}

	@Test
	public void invalidYamlIsReportedAsAConfigExceptionNamingTheFile() throws Exception {
		File file = write("config.yml", "key: [unclosed\n".getBytes(UTF_8));

		try {
			ConfigFiles.load(file, null, TestSupport.logger());
			fail("invalid YAML must not load");
		} catch (ConfigException expected) {
			assertTrue(expected.getMessage(), expected.getMessage().contains("config.yml"));
		}
	}

	@Test
	public void missingKeysFallBackToTheBundledDefaultsAndAreReportedOnce() throws Exception {
		File file = write("config.yml", "size: 5\n".getBytes(UTF_8));
		TestSupport.RecordingLogger logger = TestSupport.logger();

		YamlConfiguration yaml = ConfigFiles.load(file, stream("size: 10\nrefresh: 300\nmore: true\n"), logger);

		assertEquals(5, yaml.getInt("size")); // the admin's value wins
		assertEquals(300, yaml.getInt("refresh")); // the default fills the gap
		assertTrue(yaml.getBoolean("more"));
		assertEquals(1, logger.count(Level.INFO));
		assertTrue(logger.contains(Level.INFO, "refresh"));
		assertTrue(logger.contains(Level.INFO, "more"));
		assertFalse(logger.contains(Level.INFO, "size"));
	}

	@Test
	public void freeFormSectionsAreNotReportedAsMissing() throws Exception {
		File file = write("config.yml", "name: x\n".getBytes(UTF_8));
		TestSupport.RecordingLogger logger = TestSupport.logger();

		ConfigFiles.load(file, stream("name: y\ndictionary:\n  k:\n    divider: 1000\n"), logger, "dictionary");

		assertEquals(0, logger.count(Level.INFO));
	}

	@Test
	public void aFileThatCannotBeReadIsAConfigException() {
		try {
			ConfigFiles.load(new File(this.folder.getRoot(), "absent.yml"), null, TestSupport.logger());
			fail("a missing file must not load");
		} catch (ConfigException expected) {
			assertTrue(expected.getMessage().contains("absent.yml"));
		}
	}
}
