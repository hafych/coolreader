package org.coolreader.crengine;

public final class BatteryStatus {
	// Always sync these constants with crengine/include/lvdocview.h.
	public static final int STATE_NO_BATTERY = -2;
	public static final int STATE_CHARGING = -1;
	public static final int STATE_DISCHARGING = -3;
	public static final int CHARGER_NO = 1;
	public static final int CHARGER_AC = 2;
	public static final int CHARGER_USB = 3;
	public static final int CHARGER_WIRELESS = 4;

	private final int state;
	private final int chargingConnection;
	private final int chargeLevel;

	private BatteryStatus(
			int state,
			int chargingConnection,
			int chargeLevel) {
		this.state = state;
		this.chargingConnection = chargingConnection;
		this.chargeLevel = chargeLevel;
	}

	public static BatteryStatus unavailable() {
		return new BatteryStatus(
				STATE_NO_BATTERY, CHARGER_NO, 0);
	}

	public static BatteryStatus fromRawLevel(
			int state,
			int chargingConnection,
			int rawLevel,
			int scale) {
		return new BatteryStatus(
				state,
				chargingConnection,
				normalizeLevel(rawLevel, scale));
	}

	private static int normalizeLevel(int rawLevel, int scale) {
		if (rawLevel <= 0 || scale <= 0)
			return 0;
		long percent = (long) rawLevel * 100 / scale;
		return (int) Math.min(percent, 100);
	}

	public int getState() {
		return state;
	}

	public int getChargingConnection() {
		return chargingConnection;
	}

	public int getChargeLevel() {
		return chargeLevel;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other)
			return true;
		if (!(other instanceof BatteryStatus))
			return false;
		BatteryStatus status = (BatteryStatus) other;
		return state == status.state
				&& chargingConnection == status.chargingConnection
				&& chargeLevel == status.chargeLevel;
	}

	@Override
	public int hashCode() {
		int result = state;
		result = 31 * result + chargingConnection;
		result = 31 * result + chargeLevel;
		return result;
	}
}
