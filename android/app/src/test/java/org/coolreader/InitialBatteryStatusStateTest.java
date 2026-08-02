package org.coolreader;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.coolreader.crengine.BatteryStatus;
import org.junit.Test;

public class InitialBatteryStatusStateTest {
	@Test
	public void setPublishesImmutableSnapshot() {
		InitialBatteryStatusState state =
				new InitialBatteryStatusState();
		assertEquals(
				BatteryStatus.STATE_NO_BATTERY,
				state.get().getState());

		BatteryStatus next = BatteryStatus.fromRawLevel(
				BatteryStatus.STATE_DISCHARGING,
				BatteryStatus.CHARGER_USB,
				40,
				100);
		assertTrue(state.set(next));
		assertSame(next, state.get());
	}

	@Test
	public void closeRejectsFurtherPublication() {
		InitialBatteryStatusState state =
				new InitialBatteryStatusState();
		BatteryStatus next = BatteryStatus.fromRawLevel(
				BatteryStatus.STATE_CHARGING,
				BatteryStatus.CHARGER_AC,
				80,
				100);

		assertTrue(state.close());
		assertTrue(state.isClosed());
		assertFalse(state.set(next));
		assertEquals(
				BatteryStatus.STATE_NO_BATTERY,
				state.get().getState());
		assertFalse(state.close());
	}

	@Test(expected = IllegalArgumentException.class)
	public void nullStatusIsRejected() {
		new InitialBatteryStatusState().set(null);
	}
}
