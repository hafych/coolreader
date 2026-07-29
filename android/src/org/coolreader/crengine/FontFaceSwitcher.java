package org.coolreader.crengine;

import java.util.Objects;

final class FontFaceSwitcher {
	private FontFaceSwitcher() {
	}

	static String select(
			String current,
			String[] available,
			int direction) {
		if (available == null || available.length == 0)
			return null;
		int currentIndex = -1;
		for (int i = 0; i < available.length; i++) {
			if (Objects.equals(available[i], current)) {
				currentIndex = i;
				break;
			}
		}
		int step = Integer.compare(direction, 0);
		if (currentIndex < 0)
			return available[step < 0 ? available.length - 1 : 0];
		int selected =
				Math.floorMod(currentIndex + step, available.length);
		return available[selected];
	}
}
