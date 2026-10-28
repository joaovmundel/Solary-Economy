package com.redeskyller.bukkit.solaryeconomy.format;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.redeskyller.bukkit.solaryeconomy.TestSupport;

public class AmountParserTest {

	private final AmountParser parser = new AmountParser(TestSupport.holder());

	private void assertParses(String expected, String input) throws InvalidAmountException {
		assertEquals(input, 0, new BigDecimal(expected).compareTo(this.parser.parse(input)));
	}

	private void assertRejected(AmountParser target, String input) {
		try {
			target.parse(input);
			fail("'" + input + "' should have been rejected");
		} catch (InvalidAmountException expected) {
			// the exception is the contract: commands answer "invalid value"
		}
	}

	@Test
	public void parsesPlainNumbersWithDotOrCommaDecimals() throws Exception {
		assertParses("100", "100");
		assertParses("1.5", "1.5");
		assertParses("1.5", "1,5");
		assertParses("1.5", "  1,5  ");
	}

	@Test
	public void expandsAbbreviationsCaseInsensitively() throws Exception {
		assertParses("32000", "32k");
		assertParses("1500", "1,5k");
		assertParses("2500000", "2.5M");
		assertParses("3000000000", "3b");
		assertParses("1000000000000", "1T");
	}

	@Test
	public void dropsDecimalsBeyondTheConfiguredPlaces() throws Exception {
		assertParses("0.12", "0.129");
		assertEquals(2, this.parser.parse("5").scale());
	}

	@Test
	public void anUnknownSuffixIsAnErrorNotTimesOne() {
		// the old parser silently read "5q" as 5
		assertRejected(this.parser, "5q");
		assertRejected(this.parser, "5kk");
		assertRejected(this.parser, "abc");
	}

	@Test
	public void rejectsAnythingThatIsNotAPlainPositiveAmount() {
		assertRejected(this.parser, null);
		assertRejected(this.parser, "");
		assertRejected(this.parser, "   ");
		assertRejected(this.parser, "-5");
		assertRejected(this.parser, "+5");
		assertRejected(this.parser, "1.2.3");
		assertRejected(this.parser, "1 000");
		assertRejected(this.parser, "10 k");
		assertRejected(this.parser, "NaN");
		assertRejected(this.parser, "Infinity");
		assertRejected(this.parser, "0x10");
	}

	@Test
	public void scientificNotationIsRefusedSoAHugeExponentCannotExhaustMemory() {
		assertRejected(this.parser, "1e5");
		assertRejected(this.parser, "1E999999999");
		assertRejected(this.parser, "1e999999999");
	}

	@Test
	public void boundsTheNumberOfDigits() {
		StringBuilder tooManyDigits = new StringBuilder();
		for (int i = 0; i < 41; i++)
			tooManyDigits.append('9');
		assertRejected(this.parser, tooManyDigits.toString());
		assertRejected(this.parser, "1." + tooManyDigits);
	}

	@Test
	public void abbreviationsCanBeSwitchedOffForCommands() throws Exception {
		Map<String, Object> overrides = new HashMap<String, Object>();
		overrides.put("abbreviations.enable.commands", false);
		AmountParser plainOnly = new AmountParser(TestSupport.holder(overrides));

		assertEquals(0, new BigDecimal("5").compareTo(plainOnly.parse("5")));
		assertRejected(plainOnly, "5k");
	}

	@Test
	public void followsTheDecimalPlacesSetting() throws Exception {
		Map<String, Object> overrides = new HashMap<String, Object>();
		overrides.put("decimal_places", 0);
		AmountParser integers = new AmountParser(TestSupport.holder(overrides));

		assertEquals(0, new BigDecimal("12").compareTo(integers.parse("12.99")));
		assertEquals(0, integers.parse("12.99").scale());
	}
}
