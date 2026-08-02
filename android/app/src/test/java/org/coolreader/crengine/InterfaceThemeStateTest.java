package org.coolreader.crengine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class InterfaceThemeStateTest {
	@Test
	public void themeAndIconsPublishUntilClose() {
		InterfaceThemeState state = new InterfaceThemeState();
		InterfaceTheme theme = InterfaceThemeCatalog.create(false)
				.findByCode("LIGHT");
		ActionIconSet icons = ActionIconSet.builder()
				.override(ReaderAction.ABOUT, 42)
				.build();

		assertNull(state.getTheme());
		assertEquals(
				ReaderAction.ABOUT.iconId,
				state.iconFor(ReaderAction.ABOUT));

		state.setTheme(theme);
		state.setActionIcons(icons);
		assertSame(theme, state.getTheme());
		assertSame(icons, state.getActionIcons());
		assertEquals(42, state.iconFor(ReaderAction.ABOUT));

		assertTrue(state.close());
		assertTrue(state.isClosed());
		assertNull(state.getTheme());
		assertEquals(
				ReaderAction.ABOUT.iconId,
				state.iconFor(ReaderAction.ABOUT));
		state.setTheme(theme);
		state.setActionIcons(icons);
		assertNull(state.getTheme());
		assertEquals(
				ReaderAction.ABOUT.iconId,
				state.iconFor(ReaderAction.ABOUT));
		assertFalse(state.close());
	}

	@Test
	public void setActionIconsNullBecomesEmpty() {
		InterfaceThemeState state = new InterfaceThemeState();
		state.setActionIcons(
				ActionIconSet.builder()
						.override(ReaderAction.ABOUT, 7)
						.build());
		state.setActionIcons(null);
		assertEquals(
				ReaderAction.ABOUT.iconId,
				state.iconFor(ReaderAction.ABOUT));
	}
}
