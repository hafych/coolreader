package org.coolreader.crengine;

import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public class BatteryStatusTest {
	@Test
	public void rawLevelIsNormalizedAgainstProviderScale() {
		BatteryStatus status = BatteryStatus.fromRawLevel(
				BatteryStatus.STATE_DISCHARGING,
				BatteryStatus.CHARGER_NO,
				50,
				200);

		assertEquals(25, status.getChargeLevel());
		assertEquals(
				BatteryStatus.STATE_DISCHARGING,
				status.getState());
		assertEquals(
				BatteryStatus.CHARGER_NO,
				status.getChargingConnection());
	}

	@Test
	public void invalidAndOutOfRangeLevelsAreSafelyClamped() {
		assertEquals(0, level(-1, 100));
		assertEquals(0, level(50, 0));
		assertEquals(0, level(50, -1));
		assertEquals(100, level(250, 200));
		assertEquals(
				100,
				level(Integer.MAX_VALUE, Integer.MAX_VALUE));
		assertEquals(
				100,
				level(Integer.MAX_VALUE, 1));
	}

	@Test
	public void unavailableStatusMatchesNativeNoBatteryContract() {
		BatteryStatus status = BatteryStatus.unavailable();

		assertEquals(BatteryStatus.STATE_NO_BATTERY, status.getState());
		assertEquals(
				BatteryStatus.CHARGER_NO,
				status.getChargingConnection());
		assertEquals(0, status.getChargeLevel());
	}

	@Test
	public void statusIsAnImmutableComparableSnapshot() {
		BatteryStatus first = BatteryStatus.fromRawLevel(
				BatteryStatus.STATE_CHARGING,
				BatteryStatus.CHARGER_USB,
				40,
				80);
		BatteryStatus equal = BatteryStatus.fromRawLevel(
				BatteryStatus.STATE_CHARGING,
				BatteryStatus.CHARGER_USB,
				50,
				100);
		BatteryStatus different = BatteryStatus.fromRawLevel(
				BatteryStatus.STATE_DISCHARGING,
				BatteryStatus.CHARGER_USB,
				50,
				100);

		assertEquals(first, equal);
		assertEquals(first.hashCode(), equal.hashCode());
		assertNotEquals(first, different);
		assertTrue(Modifier.isFinal(BatteryStatus.class.getModifiers()));
		for (Field field : BatteryStatus.class.getDeclaredFields()) {
			if (Modifier.isStatic(field.getModifiers())) {
				assertTrue(Modifier.isFinal(field.getModifiers()));
				assertTrue(field.getType().isPrimitive());
			} else {
				assertTrue(Modifier.isPrivate(field.getModifiers()));
				assertTrue(Modifier.isFinal(field.getModifiers()));
			}
		}
	}

	private static int level(int rawLevel, int scale) {
		return BatteryStatus.fromRawLevel(
				BatteryStatus.STATE_DISCHARGING,
				BatteryStatus.CHARGER_NO,
				rawLevel,
				scale).getChargeLevel();
	}
}
