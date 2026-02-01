package com.j256.simplemetrics.utils;

import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.Closeable;
import java.io.IOException;
import java.net.Socket;

import org.easymock.EasyMock;
import org.junit.Test;

public class MiscUtilsTest {

	@Test
	public void testCoverage() {
		new MiscUtils();
		MiscUtils.closeQuietly((Socket) null);
		assertTrue(MiscUtils.isBlank(null));
		assertTrue(MiscUtils.isBlank(""));
		assertTrue(MiscUtils.isBlank(" "));
		assertEquals(null, MiscUtils.capitalize(null));
		assertEquals("", MiscUtils.capitalize(""));
		assertEquals("The", MiscUtils.capitalize("The"));
		assertEquals("The", MiscUtils.capitalize("the"));
	}

	@Test
	public void testCloseQuietlyThrows() throws IOException {
		Closeable closeable = EasyMock.createMock(Closeable.class);
		closeable.close();
		expectLastCall().andThrow(new IOException());
		replay(closeable);
		MiscUtils.closeQuietly(closeable);
		verify(closeable);
	}
}
