package com.redeskyller.bukkit.solaryeconomy.economy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class AccountNamesTest {

	@Test
	public void keyIsLowerCaseSoNamesAreCaseInsensitive() {
		assertEquals("steve", AccountNames.keyOf("Steve"));
		assertEquals("steve", AccountNames.keyOf("STEVE"));
		assertEquals(AccountNames.keyOf("Steve"), AccountNames.keyOf("sTeVe"));
	}

	@Test
	public void keyDoesNotDependOnTheDefaultLocale() {
		java.util.Locale original = java.util.Locale.getDefault();
		try {
			java.util.Locale.setDefault(new java.util.Locale("tr", "TR")); // "I".toLowerCase() is a dotless i here
			assertEquals("iron", AccountNames.keyOf("IRON"));
		} finally {
			java.util.Locale.setDefault(original);
		}
	}

	@Test
	public void displayNameKeepsTheOriginalCasingAndDropsOuterSpaces() {
		assertEquals("Steve", AccountNames.displayNameOf("  Steve "));
	}

	@Test
	public void rejectsNamesThatCannotBeAccounts() {
		assertNull(AccountNames.keyOf(null));
		assertNull(AccountNames.keyOf(""));
		assertNull(AccountNames.keyOf("   "));
		assertNull(AccountNames.keyOf("line\nbreak"));
		assertNull(AccountNames.keyOf("tab\tbed"));
		assertNull(AccountNames.keyOf("§4red"));
	}

	@Test
	public void lengthLimitIsTheLegacyColumnSize() {
		StringBuilder atLimit = new StringBuilder();
		for (int i = 0; i < AccountNames.MAX_LENGTH; i++)
			atLimit.append('a');
		assertTrue(AccountNames.isValid(atLimit.toString()));
		assertFalse(AccountNames.isValid(atLimit.toString() + "a"));
	}

	@Test
	public void quotesSemicolonsAndUnicodeAreFineBecauseSqlIsParameterized() {
		assertEquals("a'or'1'='1", AccountNames.keyOf("a'OR'1'='1"));
		assertEquals("x; drop table y;--", AccountNames.keyOf("x; DROP TABLE y;--"));
		assertEquals("josé", AccountNames.keyOf("JosÉ"));
	}
}
