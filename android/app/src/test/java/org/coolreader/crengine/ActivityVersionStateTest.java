package org.coolreader.crengine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ActivityVersionStateTest {
	@Test
	public void setAndClose() {
		ActivityVersionState state = new ActivityVersionState("3.1");
		assertEquals("3.1", state.get());
		state.set("3.2.1");
		assertEquals("3.2.1", state.get());
		state.set(null);
		assertEquals("3.2.1", state.get());
		state.set("");
		assertEquals("3.2.1", state.get());

		assertTrue(state.close());
		assertTrue(state.isClosed());
		state.set("9.9");
		assertEquals("3.2.1", state.get());
		assertFalse(state.close());
	}
}
