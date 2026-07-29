package com.s_trace.motion_watchdog;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MotionWatchdogFadeStateTest {
	@Test
	public void fadeReachesZeroWithoutUnderflow() {
		MotionWatchdogFadeState state =
				new MotionWatchdogFadeState(2);

		assertFalse(state.isSilent());
		assertEquals(1, state.step());
		assertEquals(0, state.step());
		assertTrue(state.isSilent());
		assertEquals(0, state.step());
		assertEquals(2, state.originalVolume());
	}

	@Test
	public void zeroVolumeIsAlreadySilent() {
		MotionWatchdogFadeState state =
				new MotionWatchdogFadeState(0);

		assertTrue(state.isSilent());
		assertEquals(0, state.step());
		assertEquals(0, state.originalVolume());
	}

	@Test
	public void malformedNegativeVolumeIsClamped() {
		MotionWatchdogFadeState state =
				new MotionWatchdogFadeState(Integer.MIN_VALUE);

		assertTrue(state.isSilent());
		assertEquals(0, state.currentVolume());
		assertEquals(0, state.originalVolume());
	}
}
