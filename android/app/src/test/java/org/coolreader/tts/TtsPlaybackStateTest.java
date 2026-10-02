package org.coolreader.tts;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class TtsPlaybackStateTest {
	@Test
	public void transitionsReportChangeOnlyOnce() {
		TtsPlaybackState state = new TtsPlaybackState();
		assertEquals(TtsPlaybackState.Status.STOPPED, state.status());
		assertFalse(state.isPlaying());
		assertFalse(state.isPaused());

		assertTrue(state.transitionTo(TtsPlaybackState.Status.PLAYING));
		assertTrue(state.isPlaying());
		// Same status: no change.
		assertFalse(state.transitionTo(TtsPlaybackState.Status.PLAYING));

		assertTrue(state.transitionTo(TtsPlaybackState.Status.PAUSED));
		assertTrue(state.isPaused());
		assertTrue(state.transitionTo(TtsPlaybackState.Status.STOPPED));
		assertFalse(state.isPlaying());
		assertFalse(state.isPaused());
	}

	@Test
	public void errorCounterIsBoundedAndResetOnSuccess() {
		TtsPlaybackState state = new TtsPlaybackState();
		assertEquals(0, state.errorCount());
		assertEquals(1, state.recordError());
		assertEquals(2, state.recordError());
		state.recordSuccess();
		assertEquals(0, state.errorCount());
	}

	@Test
	public void resetSilentAndClose() {
		TtsPlaybackState state = new TtsPlaybackState();
		assertTrue(state.transitionTo(TtsPlaybackState.Status.PLAYING));
		state.resetSilent();
		assertEquals(TtsPlaybackState.Status.STOPPED, state.status());
		// After silent reset no stale previous: next PLAYING is a change.
		assertTrue(state.transitionTo(TtsPlaybackState.Status.PLAYING));
		assertTrue(state.close());
		assertTrue(state.isClosed());
		assertFalse(state.transitionTo(TtsPlaybackState.Status.PAUSED));
		assertEquals(0, state.errorCount());
		assertFalse(state.close());
	}

	@Test
	public void parkSilentPreservesPendingTransition() {
		TtsPlaybackState state = new TtsPlaybackState();
		// Legacy prepare parks PAUSED without touching previous (STOPPED).
		state.parkSilent(TtsPlaybackState.Status.PAUSED);
		assertTrue(state.isPaused());
		// Next real PLAYING still reports exactly one change.
		assertTrue(state.transitionTo(TtsPlaybackState.Status.PLAYING));
		assertTrue(state.isPlaying());
	}
}
