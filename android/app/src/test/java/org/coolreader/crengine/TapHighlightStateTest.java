package org.coolreader.crengine;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class TapHighlightStateTest {
	private static final TapZoneGeometry.Bounds FIRST =
			TapZoneGeometry.boundsAt(1, 1, 300, 300);
	private static final TapZoneGeometry.Bounds SECOND =
			TapZoneGeometry.boundsAt(299, 299, 300, 300);

	@Test
	public void latestShowWinsAndPublishesOnlyWhenApplied() {
		TapHighlightState state = new TapHighlightState();
		TapHighlightState.Show stale =
				state.requestShow(FIRST, 1);
		TapHighlightState.Show current =
				state.requestShow(SECOND, 2);

		assertNull(state.applyShow(stale));
		TapHighlightState.Transition transition =
				state.applyShow(current);

		assertNotNull(transition);
		assertNull(transition.previous());
		assertSame(current, transition.current());
		assertTrue(state.isVisible(current));
	}

	@Test
	public void replacementTransitionCarriesBothDirtyBounds() {
		TapHighlightState state = new TapHighlightState();
		TapHighlightState.Show first =
				state.requestShow(FIRST, 1);
		assertNotNull(state.applyShow(first));
		TapHighlightState.Show replacement =
				state.requestShow(SECOND, 2);

		TapHighlightState.Transition transition =
				state.applyShow(replacement);

		assertSame(first, transition.previous());
		assertSame(replacement, transition.current());
		assertTrue(transition.hasVisualChange());
		assertSame(FIRST, transition.previous().bounds());
		assertSame(SECOND, transition.current().bounds());
	}

	@Test
	public void ownedHideClearsPriorVisibleAndPendingOwner() {
		TapHighlightState state = new TapHighlightState();
		TapHighlightState.Show visible =
				state.requestShow(FIRST, 1);
		assertNotNull(state.applyShow(visible));
		TapHighlightState.Show pending =
				state.requestShow(SECOND, 2);
		TapHighlightState.Hide hide =
				state.requestOwnedHide(pending);

		TapHighlightState.Transition transition =
				state.applyHide(hide);

		assertSame(visible, transition.previous());
		assertNull(transition.current());
		assertFalse(state.isVisible(visible));
		assertNull(state.applyShow(pending));
	}

	@Test
	public void staleTimerCannotHideReplacement() {
		TapHighlightState state = new TapHighlightState();
		TapHighlightState.Show stale =
				state.requestShow(FIRST, 1);
		TapHighlightState.Show replacement =
				state.requestShow(SECOND, 2);
		assertNotNull(state.applyShow(replacement));

		assertNull(state.requestOwnedHide(stale));
		assertTrue(state.isVisible(replacement));
	}

	@Test
	public void globalHideIsOneShotAndCanCancelPendingShow() {
		TapHighlightState state = new TapHighlightState();
		TapHighlightState.Show pending =
				state.requestShow(FIRST, 1);
		TapHighlightState.Hide hide =
				state.requestHideAll();

		TapHighlightState.Transition transition =
				state.applyHide(hide);

		assertNotNull(transition);
		assertFalse(transition.hasVisualChange());
		assertNull(state.applyHide(hide));
		assertNull(state.applyShow(pending));
	}

	@Test
	public void invalidationRejectsQueuedShowAndClearsVisible() {
		TapHighlightState state = new TapHighlightState();
		TapHighlightState.Show visible =
				state.requestShow(FIRST, 1);
		assertNotNull(state.applyShow(visible));

		state.invalidate();

		assertFalse(state.isVisible(visible));
		assertNull(state.applyShow(visible));
		assertNull(state.requestOwnedHide(visible));
	}

	@Test
	public void invalidBoundsAreNotScheduled() {
		TapHighlightState state = new TapHighlightState();

		assertNull(state.requestShow(
				TapZoneGeometry.boundsAt(0, 0, 0, 1),
				1));
	}

	@Test(expected = IllegalArgumentException.class)
	public void nullBoundsAreRejected() {
		new TapHighlightState().requestShow(null, 1);
	}

	@Test
	public void closePermanentlyRejectsQueuedAndNewWork() {
		TapHighlightState state = new TapHighlightState();
		TapHighlightState.Show show =
				state.requestShow(FIRST, 1);

		assertTrue(state.close());
		assertFalse(state.close());
		assertFalse(state.isCurrent(show));
		assertFalse(state.isVisible(show));
		assertNull(state.applyShow(show));
		assertNull(state.requestShow(SECOND, 2));
		assertNull(state.requestHideAll());
		assertNull(state.requestOwnedHide(show));
	}
}
