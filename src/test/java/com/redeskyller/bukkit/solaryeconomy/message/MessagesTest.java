package com.redeskyller.bukkit.solaryeconomy.message;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.logging.Level;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.Test;

import com.redeskyller.bukkit.solaryeconomy.TestSupport;

public class MessagesTest {

	@Test
	public void everyMessageIsDefinedInTheShippedFile() {
		YamlConfiguration shipped = TestSupport.bundledMessages();

		for (Message message : Message.values())
			assertNotNull("mensagens.yml has no " + message, shipped.getString(message.name()));
	}

	@Test
	public void everyShippedKeyIsAKnownMessage() {
		YamlConfiguration shipped = TestSupport.bundledMessages();

		for (String key : shipped.getKeys(false))
			Message.valueOf(key); // throws if the file has a key the code never reads
	}

	@Test
	public void translatesColorCodesAndFillsPlaceholders() {
		Messages messages = new Messages(TestSupport.bundledMessages(), TestSupport.logger());

		assertEquals("§aMoney: 10", messages.get(Message.MONEY, "valor", "10"));
		assertEquals("§aMoney de Steve: 4k", messages.get(Message.MONEY_OTHER, "player", "Steve", "valor", "4k"));
	}

	@Test
	public void valuesAreNeverInterpretedAsColorCodes() {
		Messages messages = new Messages(TestSupport.bundledMessages(), TestSupport.logger());

		// in offline mode a player may be called "&4Admin"
		assertEquals("§aMoney de &4Admin: 1", messages.get(Message.MONEY_OTHER, "player", "&4Admin", "valor", "1"));
	}

	@Test
	public void aKeyMissingFromTheAdminsFileUsesTheShippedText() {
		YamlConfiguration adminsFile = new YamlConfiguration();
		adminsFile.set("MONEY", "&bSaldo: {valor}");
		adminsFile.setDefaults(TestSupport.bundledMessages());
		Messages messages = new Messages(adminsFile, TestSupport.logger());

		assertEquals("§bSaldo: 5", messages.get(Message.MONEY, "valor", "5"));
		assertTrue(messages.get(Message.NO_PERMISSION).contains("permiss"));
	}

	@Test
	public void aKeyMissingEverywhereIsShownByNameAndReportedOnlyOnce() {
		TestSupport.RecordingLogger logger = TestSupport.logger();
		Messages messages = new Messages(new YamlConfiguration(), logger);

		assertEquals("<MONEY>", messages.get(Message.MONEY));
		assertEquals("<MONEY>", messages.get(Message.MONEY));

		assertEquals(1, logger.count(Level.WARNING));
		assertFalse(logger.contains(Level.WARNING, "NullPointer"));
	}

	@Test
	public void replaceSwapsTheLoadedFile() {
		YamlConfiguration first = new YamlConfiguration();
		first.set("MONEY", "one");
		YamlConfiguration second = new YamlConfiguration();
		second.set("MONEY", "two");
		Messages messages = new Messages(first, TestSupport.logger());

		messages.replace(second);

		assertEquals("two", messages.get(Message.MONEY));
	}
}
