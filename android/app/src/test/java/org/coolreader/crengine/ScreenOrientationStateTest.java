package org.coolreader.crengine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.pm.ActivityInfo;

import org.junit.Test;

public class ScreenOrientationStateTest {
	@Test
	public void setAndClose() {
		ScreenOrientationState state = new ScreenOrientationState();
		assertEquals(
				ActivityInfo.SCREEN_ORIENTATION_USER,
				state.get());
		state.set(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		assertEquals(
				ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE,
				state.get());
		assertTrue(state.close());
		assertTrue(state.isClosed());
		assertEquals(
				ActivityInfo.SCREEN_ORIENTATION_USER,
				state.get());
		state.set(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		assertEquals(
				ActivityInfo.SCREEN_ORIENTATION_USER,
				state.get());
		assertFalse(state.close());
	}
}
