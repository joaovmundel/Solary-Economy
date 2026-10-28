package com.redeskyller.bukkit.solaryeconomy.update;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class UpdateCheckerTest {

	@Test
	public void comparesVersionsNumericallyNotAsText() {
		assertTrue(UpdateChecker.isNewer("1.10.0", "1.9.9"));
		assertTrue(UpdateChecker.isNewer("1.6.1", "1.6.0"));
		assertTrue(UpdateChecker.isNewer("2.0", "1.99.99"));
		assertFalse(UpdateChecker.isNewer("1.9.9", "1.10.0"));
	}

	@Test
	public void equalVersionsAreNotNewerEvenIfWrittenDifferently() {
		assertFalse(UpdateChecker.isNewer("1.6.0", "1.6.0"));
		assertFalse(UpdateChecker.isNewer("v1.6", "1.6.0"));
		assertFalse(UpdateChecker.isNewer("1.6.0.0", "1.6"));
	}

	@Test
	public void ignoresTheLeadingVOfGithubTagsAndTheSuffix() {
		assertTrue(UpdateChecker.isNewer("v1.7.0", "1.6.0"));
		assertTrue(UpdateChecker.isNewer("2.0-beta", "1.9"));
		assertFalse(UpdateChecker.isNewer("1.6.0", "1.6.0-SNAPSHOT"));
	}

	@Test
	public void olderThanCurrentIsNotAnUpdate() {
		assertFalse(UpdateChecker.isNewer("1.5.3", "1.6.0"));
	}

	@Test
	public void anUnparseableVersionNeverTriggersAnUpdateNag() {
		assertFalse(UpdateChecker.isNewer("latest", "1.6.0"));
		assertFalse(UpdateChecker.isNewer("1.6.0", "dev-build"));
		assertFalse(UpdateChecker.isNewer("", "1.6.0"));
	}
}
