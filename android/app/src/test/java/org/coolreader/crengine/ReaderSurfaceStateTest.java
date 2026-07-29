package org.coolreader.crengine;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ReaderSurfaceStateTest {
	@Test
	public void visibleWindowRefreshesWhenSurfaceArrives() {
		ReaderSurfaceState state = new ReaderSurfaceState();

		assertFalse(state.changeVisibility(true));
		assertTrue(state.markSurfaceCreated());
		assertFalse(state.markSurfaceCreated());
		assertTrue(state.isDrawable());
	}

	@Test
	public void existingSurfaceRefreshesOnVisibleTransition() {
		ReaderSurfaceState state = new ReaderSurfaceState();

		assertFalse(state.markSurfaceCreated());
		assertTrue(state.changeVisibility(true));
		assertFalse(state.changeVisibility(true));
	}

	@Test
	public void focusRefreshRequiresItsExactVisibleSurface() {
		ReaderSurfaceState state = new ReaderSurfaceState();
		state.markSurfaceCreated();
		state.changeVisibility(true);
		ReaderSurfaceState.FocusRefresh first =
				state.changeFocus(true);

		assertNotNull(first);
		assertTrue(state.claimFocusRefresh(first));
		assertFalse(state.claimFocusRefresh(first));
		assertNull(state.changeFocus(true));
	}

	@Test
	public void focusReplacementInvalidatesPreviousRequest() {
		ReaderSurfaceState state = visibleSurface();
		ReaderSurfaceState.FocusRefresh first =
				state.changeFocus(true);
		state.changeFocus(false);
		ReaderSurfaceState.FocusRefresh second =
				state.changeFocus(true);

		assertFalse(state.claimFocusRefresh(first));
		assertTrue(state.claimFocusRefresh(second));
	}

	@Test
	public void hiddenOrDestroyedSurfaceRejectsPendingRefresh() {
		ReaderSurfaceState hidden = visibleSurface();
		ReaderSurfaceState.FocusRefresh hiddenRefresh =
				hidden.changeFocus(true);
		hidden.changeVisibility(false);

		assertFalse(hidden.claimFocusRefresh(hiddenRefresh));

		ReaderSurfaceState destroyed = visibleSurface();
		ReaderSurfaceState.FocusRefresh destroyedRefresh =
				destroyed.changeFocus(true);
		destroyed.markSurfaceDestroyed();

		assertFalse(destroyed.claimFocusRefresh(
				destroyedRefresh));
		assertFalse(destroyed.isDrawable());
		assertTrue(destroyed.markSurfaceCreated());
	}

	@Test
	public void closePermanentlyRejectsSurfaceAndRefreshWork() {
		ReaderSurfaceState state = visibleSurface();
		ReaderSurfaceState.FocusRefresh refresh =
				state.changeFocus(true);

		assertTrue(state.close());
		assertFalse(state.close());
		assertTrue(state.isClosed());
		assertFalse(state.isDrawable());
		assertFalse(state.claimFocusRefresh(refresh));
		assertFalse(state.markSurfaceCreated());
		assertFalse(state.changeVisibility(true));
		assertNull(state.changeFocus(true));
	}

	private static ReaderSurfaceState visibleSurface() {
		ReaderSurfaceState state = new ReaderSurfaceState();
		state.markSurfaceCreated();
		state.changeVisibility(true);
		return state;
	}
}
