package org.coolreader.crengine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ScreenBacklightDurationStateTest {
	@Test
	public void durationAndEnabledUntilClose() {
		ScreenBacklightDurationState state =
				new ScreenBacklightDurationState(180000);
		assertEquals(180000, state.getDurationMs());
		assertTrue(state.isEnabled());

		state.setDurationMs(0);
		assertEquals(0, state.getDurationMs());
		assertFalse(state.isEnabled());

		state.setDurationMs(60000);
		assertTrue(state.isEnabled());
		assertTrue(state.close());
		assertTrue(state.isClosed());
		// closed: isEnabled false even if duration remains
		assertFalse(state.isEnabled());
		state.setDurationMs(1);
		assertEquals(60000, state.getDurationMs());
		assertFalse(state.close());
	}
}
