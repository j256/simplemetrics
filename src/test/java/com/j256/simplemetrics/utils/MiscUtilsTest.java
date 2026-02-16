package com.j256.simplemetrics.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class MiscUtilsTest {

	@Test
	public void testCoverage() {
		new MiscUtils();
		assertTrue(MiscUtils.isBlank(null));
		assertTrue(MiscUtils.isBlank(""));
		assertTrue(MiscUtils.isBlank(" "));
		assertEquals(null, MiscUtils.capitalize(null));
		assertEquals("", MiscUtils.capitalize(""));
		assertEquals("The", MiscUtils.capitalize("The"));
		assertEquals("The", MiscUtils.capitalize("the"));
	}
}
