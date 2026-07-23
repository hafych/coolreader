package org.coolreader.crengine;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InterruptedIOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class DeadlineInputStreamTest {
	@Test
	public void readsWithinDeadline() throws Exception {
		DeadlineInputStream input = new DeadlineInputStream(
				new ByteArrayInputStream(new byte[] {1, 2}), 10_000);
		assertEquals(1, input.read());
		assertEquals(2, input.read());
		assertEquals(-1, input.read());
	}

	@Test
	public void rejectsReadAfterDeadline() throws Exception {
		DeadlineInputStream input = new DeadlineInputStream(
				new ByteArrayInputStream(new byte[] {1}), 0);
		try {
			input.read();
			fail("Expired transfer deadline must reject reads");
		} catch (InterruptedIOException expected) {
			assertEquals(
					"HTTP transfer time limit exceeded",
					expected.getMessage());
		}
	}
}
