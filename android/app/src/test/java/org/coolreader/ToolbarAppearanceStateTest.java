package org.coolreader;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ToolbarAppearanceStateTest {
	@Test
	public void setAndGetTrackAppearanceId() {
		ToolbarAppearanceState state =
				new ToolbarAppearanceState();
		assertEquals("0", state.get());
		state.set("2");
		assertEquals("2", state.get());
		state.set(null);
		assertEquals("0", state.get());
		state.set("");
		assertEquals("0", state.get());
	}

	@Test
	public void closeIsPermanent() {
		ToolbarAppearanceState state =
				new ToolbarAppearanceState();
		state.set("3");
		assertTrue(state.close());
		assertTrue(state.isClosed());
		assertEquals("0", state.get());
		state.set("1");
		assertEquals("0", state.get());
		assertFalse(state.close());
	}
}
