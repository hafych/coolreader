package org.coolreader.crengine;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ProgressUiStateTest {
	@Test
	public void latestShowRequestWinsByIdentity() {
		ProgressUiState state = new ProgressUiState();
		ProgressUiState.Token first = state.requestShow();
		ProgressUiState.Token second = state.requestShow();

		assertFalse(state.isCurrent(first));
		assertFalse(state.markVisible(first));
		assertTrue(state.isCurrent(second));
		assertTrue(state.markVisible(second));
		assertTrue(state.isVisible());
	}

	@Test
	public void globalHideInvalidatesPendingShowAndClearsVisible()
			throws Exception {
		ProgressUiState state = new ProgressUiState();
		ProgressUiState.Token show = state.requestShow();
		assertTrue(state.markVisible(show));
		ProgressUiState.Token hide = state.requestHideAll();

		assertFalse(state.isCurrent(show));
		assertTrue(state.applyHideAll(hide));
		assertFalse(state.isVisible());
		assertFalse(state.applyHideAll(hide));
	}

	@Test
	public void failedLatestShowClearsDismissedPreviousState() {
		ProgressUiState state = new ProgressUiState();
		ProgressUiState.Token visible = state.requestShow();
		assertTrue(state.markVisible(visible));
		ProgressUiState.Token failed = state.requestShow();

		state.markShowFailed(failed);

		assertFalse(state.isVisible());
		assertFalse(state.isCurrent(failed));
	}

	@Test
	public void ownerCanCancelItsPendingShowWithoutDismissingAnother()
			throws Exception {
		ProgressUiState state = new ProgressUiState();
		ProgressUiState.Token visible = state.requestShow();
		assertTrue(state.markVisible(visible));
		ProgressUiState.Token pending = state.requestShow();

		ProgressUiState.OwnedHide hide =
				state.requestOwnedHide(pending);

		assertNotNull(hide);
		assertFalse(state.applyOwnedHide(hide));
		assertTrue(state.isVisible());
		assertFalse(state.markVisible(pending));
	}

	@Test
	public void ownerCannotHideAReplacementGeneration() {
		ProgressUiState state = new ProgressUiState();
		ProgressUiState.Token owner = state.requestShow();
		assertTrue(state.markVisible(owner));
		ProgressUiState.Token replacement = state.requestShow();
		assertTrue(state.markVisible(replacement));

		assertNull(state.requestOwnedHide(owner));
		assertTrue(state.isVisible());
		assertTrue(state.isCurrent(replacement));
	}

	@Test
	public void visibleOwnerCanHideExactlyItself() {
		ProgressUiState state = new ProgressUiState();
		ProgressUiState.Token owner = state.requestShow();
		assertTrue(state.markVisible(owner));

		ProgressUiState.OwnedHide hide =
				state.requestOwnedHide(owner);

		assertNotNull(hide);
		assertTrue(state.applyOwnedHide(hide));
		assertFalse(state.isVisible());
	}

	@Test
	public void dismissCallbackClearsOnlyItsVisibleOwner() {
		ProgressUiState state = new ProgressUiState();
		ProgressUiState.Token old = state.requestShow();
		assertTrue(state.markVisible(old));
		ProgressUiState.Token current = state.requestShow();
		assertTrue(state.markVisible(current));

		assertFalse(state.markDismissed(old));
		assertTrue(state.isVisible());
		assertTrue(state.markDismissed(current));
		assertFalse(state.isVisible());
	}

	@Test
	public void closePermanentlyRejectsNewUiWork() {
		ProgressUiState state = new ProgressUiState();
		ProgressUiState.Token owner = state.requestShow();
		assertTrue(state.markVisible(owner));

		assertTrue(state.close());
		assertFalse(state.close());
		assertFalse(state.isVisible());
		assertFalse(state.isCurrent(owner));
		assertNull(state.requestShow());
		assertNull(state.requestHideAll());
		assertNull(state.requestOwnedHide(owner));
	}
}
