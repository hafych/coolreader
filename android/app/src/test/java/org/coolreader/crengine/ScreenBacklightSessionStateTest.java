package org.coolreader.crengine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ScreenBacklightSessionStateTest {
	@Test
	public void timestampsAndTimerUntilClose() {
		ScreenBacklightSessionState state =
				new ScreenBacklightSessionState();
		Runnable first = new Runnable() {
			@Override
			public void run() {
			}
		};
		Runnable second = new Runnable() {
			@Override
			public void run() {
			}
		};

		state.setLastUserActivityTime(100);
		state.setLastUpdateTimeStamp(200);
		state.setTimerTask(first);
		assertEquals(100, state.getLastUserActivityTime());
		assertEquals(200, state.getLastUpdateTimeStamp());
		assertSame(first, state.getTimerTask());

		assertNull(state.clearForRelease());
		assertNull(state.getTimerTask());
		assertEquals(0, state.getLastUpdateTimeStamp());
		assertEquals(100, state.getLastUserActivityTime());
		assertNull(state.getWakeLock());

		state.setTimerTask(second);
		assertTrue(state.close());
		assertTrue(state.isClosed());
		assertNull(state.getTimerTask());
		assertNull(state.getWakeLock());
		assertEquals(0, state.getLastUserActivityTime());
		state.setTimerTask(first);
		state.setLastUserActivityTime(9);
		assertNull(state.getTimerTask());
		assertEquals(0, state.getLastUserActivityTime());
		assertFalse(state.close());
	}
}
