package org.coolreader.crengine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class SettingsPropertiesStateTest {
	@Test
	public void replacePublishesCloneAndCloseIsPermanent() {
		SettingsPropertiesState state = new SettingsPropertiesState();
		Properties first = new Properties();
		first.setProperty("a", "1");
		Properties previous = state.replace(first);
		assertEquals(0, previous.size());
		assertEquals("1", state.getProperty("a"));

		// Mutation of source after replace does not rewrite published snapshot.
		first.setProperty("a", "mutated");
		assertEquals("1", state.getProperty("a"));

		Properties copy = state.copy();
		assertNotSame(state.snapshot(), copy);
		copy.setProperty("a", "copy-mutated");
		assertEquals("1", state.getProperty("a"));

		Properties second = new Properties();
		second.setProperty("b", "2");
		Properties old = state.replace(second);
		assertEquals("1", old.getProperty("a"));
		assertEquals("2", state.getProperty("b"));

		assertTrue(state.close());
		assertTrue(state.isClosed());
		assertEquals(0, state.copy().size());
		Properties third = new Properties();
		third.setProperty("c", "3");
		state.replace(third);
		assertEquals(0, state.copy().size());
		assertFalse(state.close());
	}

	@Test(expected = IllegalArgumentException.class)
	public void replaceRejectsNull() {
		new SettingsPropertiesState().replace(null);
	}
}
