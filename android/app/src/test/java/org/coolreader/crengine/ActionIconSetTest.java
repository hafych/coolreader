/*
 * CoolReader for Android
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 2
 * of the License, or (at your option) any later version.
 */

package org.coolreader.crengine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import org.junit.Test;

public class ActionIconSetTest {
	@Test
	public void emptySetUsesImmutableActionDefault() {
		ActionIconSet icons = ActionIconSet.empty();

		assertEquals(ReaderAction.GO_BACK.iconId,
				icons.iconFor(ReaderAction.GO_BACK));
	}

	@Test
	public void activitySnapshotsRemainIndependent() {
		ActionIconSet.Builder firstBuilder = ActionIconSet.builder()
				.override(ReaderAction.GO_BACK, 101);
		ActionIconSet first = firstBuilder.build();
		ActionIconSet second = ActionIconSet.builder()
				.override(ReaderAction.GO_BACK, 202)
				.build();
		firstBuilder.override(ReaderAction.GO_BACK, 303);

		assertEquals(101, first.iconFor(ReaderAction.GO_BACK));
		assertEquals(202, second.iconFor(ReaderAction.GO_BACK));
	}

	@Test
	public void zeroThemeResourceKeepsActionDefault() {
		ActionIconSet icons = ActionIconSet.builder()
				.override(ReaderAction.GO_BACK, 0)
				.build();

		assertEquals(ReaderAction.GO_BACK.iconId,
				icons.iconFor(ReaderAction.GO_BACK));
	}

	@Test
	public void availableActionsCannotBeReplacedThroughReturnedArray() {
		ReaderAction[] first = ReaderAction.availableActions(false);
		ReaderAction original = first[0];
		first[0] = ReaderAction.EXIT;
		ReaderAction[] second = ReaderAction.availableActions(false);

		assertNotSame(first, second);
		assertSame(original, second[0]);
	}

	@Test
	public void deviceSpecificActionIsAddedOnlyToRequestedSnapshot() {
		ReaderAction[] regular = ReaderAction.availableActions(false);
		ReaderAction[] frontlight = ReaderAction.availableActions(true);

		assertEquals(regular.length + 1, frontlight.length);
		assertSame(
				ReaderAction.SHOW_SYSTEM_BACKLIGHT_DIALOG,
				frontlight[frontlight.length - 1]);
		assertSame(
				ReaderAction.NONE,
				ReaderAction.findById(
						ReaderAction.SHOW_SYSTEM_BACKLIGHT_DIALOG.id,
						false));
		assertSame(
				ReaderAction.SHOW_SYSTEM_BACKLIGHT_DIALOG,
				ReaderAction.findById(
						ReaderAction.SHOW_SYSTEM_BACKLIGHT_DIALOG.id,
						true));
	}

	@Test
	public void actionTypePropertiesHaveNoMutableArrayBacking() {
		assertEquals(".", ReaderAction.getTypeString(ReaderAction.NORMAL));
		assertEquals(".long.", ReaderAction.getTypeString(ReaderAction.LONG));
		assertEquals(".dbl.", ReaderAction.getTypeString(ReaderAction.DOUBLE));
		try {
			ReaderAction.getTypeString(-1);
			fail("invalid action type was accepted");
		} catch (IllegalArgumentException expected) {
			// Expected.
		}
	}
}
