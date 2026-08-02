package org.coolreader.crengine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ReaderBatteryStateTest {
	@Test
	public void updatePublishesOnlyWhenSnapshotChanges() {
		BatteryStatus initial = BatteryStatus.fromRawLevel(
				BatteryStatus.STATE_DISCHARGING,
				BatteryStatus.CHARGER_NO,
				50,
				100);
		ReaderBatteryState state =
				new ReaderBatteryState(initial);
		BatteryStatus same = BatteryStatus.fromRawLevel(
				BatteryStatus.STATE_DISCHARGING,
				BatteryStatus.CHARGER_NO,
				50,
				100);
		BatteryStatus next = BatteryStatus.fromRawLevel(
				BatteryStatus.STATE_CHARGING,
				BatteryStatus.CHARGER_USB,
				80,
				100);

		assertNull(state.update(same));
		assertSame(initial, state.snapshot());

		ReaderBatteryState.Change change = state.update(next);
		assertNotNull(change);
		assertSame(initial, change.previous());
		assertSame(next, change.current());
		assertTrue(change.stateChanged());
		assertTrue(change.connectionChanged());
		assertTrue(change.levelChanged());
		assertSame(next, state.snapshot());
	}

	@Test
	public void closeRejectsFurtherPublication() {
		ReaderBatteryState state = new ReaderBatteryState(
				BatteryStatus.unavailable());
		BatteryStatus next = BatteryStatus.fromRawLevel(
				BatteryStatus.STATE_DISCHARGING,
				BatteryStatus.CHARGER_NO,
				10,
				100);

		assertTrue(state.close());
		assertTrue(state.isClosed());
		assertNull(state.update(next));
		assertEquals(
				BatteryStatus.STATE_NO_BATTERY,
				state.snapshot().getState());
		assertFalse(state.close());
	}

	@Test(expected = IllegalArgumentException.class)
	public void nullStatusIsRejected() {
		new ReaderBatteryState(null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void nullUpdateIsRejected() {
		ReaderBatteryState state = new ReaderBatteryState(
				BatteryStatus.unavailable());
		state.update(null);
	}
}
