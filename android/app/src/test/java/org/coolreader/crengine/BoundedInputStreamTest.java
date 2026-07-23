package org.coolreader.crengine;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class BoundedInputStreamTest {
	@Test
	public void acceptsInputAtLimit() throws Exception {
		BoundedInputStream input = new BoundedInputStream(
				new ByteArrayInputStream(new byte[] {1, 2, 3}), 3);
		assertEquals(1, input.read());
		assertEquals(2, input.read());
		assertEquals(3, input.read());
		assertEquals(-1, input.read());
	}

	@Test(expected = IOException.class)
	public void rejectsInputBeyondLimit() throws Exception {
		BoundedInputStream input = new BoundedInputStream(
				new ByteArrayInputStream(new byte[] {1, 2}), 1);
		assertEquals(1, input.read());
		input.read();
	}

	@Test(expected = IOException.class)
	public void skipCannotBypassLimit() throws Exception {
		BoundedInputStream input = new BoundedInputStream(
				new ByteArrayInputStream(new byte[] {1, 2, 3}), 2);
		assertEquals(2, input.skip(3));
		assertEquals(0, input.available());
		input.read();
	}
}
