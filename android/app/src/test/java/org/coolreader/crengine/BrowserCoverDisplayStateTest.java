package org.coolreader.crengine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class BrowserCoverDisplayStateTest {
	@Test
	public void computeDimensionsMatchesHistoricalPolicy() {
		BrowserCoverDisplayState.Dimensions medium =
				BrowserCoverDisplayState.computeDimensions(
						480, 320, BrowserCoverDisplayState.SIZE_MEDIUM);
		assertEquals(93, medium.height);
		assertEquals(69, medium.width);

		BrowserCoverDisplayState.Dimensions small =
				BrowserCoverDisplayState.computeDimensions(
						480, 320, BrowserCoverDisplayState.SIZE_SMALL);
		assertEquals(80, small.height);
		assertEquals(60, small.width);

		BrowserCoverDisplayState.Dimensions big =
				BrowserCoverDisplayState.computeDimensions(
						480, 320, BrowserCoverDisplayState.SIZE_BIG);
		assertEquals(106, big.height);
		assertEquals(79, big.width);
	}

	@Test
	public void screenSizeOptionEnabledUntilClose() {
		BrowserCoverDisplayState state = new BrowserCoverDisplayState();
		assertTrue(state.isEnabled());
		assertEquals(90, state.getCoverWidth());
		assertEquals(120, state.getCoverHeight());

		BrowserCoverDisplayState.Dimensions changed =
				state.setScreenSize(480, 320);
		assertNotNull(changed);
		assertEquals(69, state.getCoverWidth());
		assertEquals(93, state.getCoverHeight());
		assertNull(state.setScreenSize(480, 320));

		assertTrue(state.setEnabled(false));
		assertFalse(state.isEnabled());
		assertFalse(state.setEnabled(false));

		BrowserCoverDisplayState.Dimensions sized =
				state.setSizeOption(BrowserCoverDisplayState.SIZE_SMALL);
		assertNotNull(sized);
		assertEquals(80, sized.height);
		assertEquals(60, sized.width);
		assertNull(state.setSizeOption(BrowserCoverDisplayState.SIZE_SMALL));

		assertTrue(state.close());
		assertTrue(state.isClosed());
		assertNull(state.setScreenSize(800, 600));
		assertNull(state.setSizeOption(BrowserCoverDisplayState.SIZE_BIG));
		assertFalse(state.setEnabled(true));
		assertFalse(state.isEnabled());
		assertFalse(state.close());
	}
}
