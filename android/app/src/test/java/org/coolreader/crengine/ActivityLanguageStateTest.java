package org.coolreader.crengine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ActivityLanguageStateTest {
	@Test
	public void setAndClose() {
		ActivityLanguageState state = new ActivityLanguageState();
		assertNull(state.get());
		state.set("ru");
		assertEquals("ru", state.get());
		assertTrue(state.close());
		assertTrue(state.isClosed());
		assertNull(state.get());
		state.set("en");
		assertNull(state.get());
		assertFalse(state.close());
	}
}
