package org.coolreader.tts;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class TtsAudioFocusStateTest {
	@Test
	public void gainConsumesPendingResume() {
		TtsAudioFocusState state = new TtsAudioFocusState();
		assertFalse(state.isAuthorized());
		state.onGranted();
		assertTrue(state.isAuthorized());
		state.armResumeOnGain();
		assertTrue(state.snapshot().shouldResume());
		assertTrue(state.onGain());
		assertTrue(state.isAuthorized());
		assertFalse(state.snapshot().shouldResume());
		// Second gain without pending request: no resume.
		assertFalse(state.onGain());
	}

	@Test
	public void lossAndTransientPaths() {
		TtsAudioFocusState state = new TtsAudioFocusState();
		state.onGranted();
		state.onLoss();
		assertFalse(state.isAuthorized());
		assertFalse(state.snapshot().shouldResume());

		state.onGranted();
		state.onTransientLoss();
		assertFalse(state.isAuthorized());
		state.armResumeOnGain();
		assertTrue(state.snapshot().shouldResume());

		state.onDelayed();
		assertFalse(state.isAuthorized());
		assertTrue(state.snapshot().delayed);
		state.onFailed();
		assertFalse(state.snapshot().shouldResume());
	}

	@Test
	public void grantedResultAndClose() {
		TtsAudioFocusState state = new TtsAudioFocusState();
		state.onGrantedWithResult(true);
		assertTrue(state.isAuthorized());
		state.onGrantedWithResult(false);
		assertFalse(state.isAuthorized());
		state.forceAuthorized();
		assertTrue(state.isAuthorized());
		assertTrue(state.close());
		assertTrue(state.isClosed());
		assertFalse(state.isAuthorized());
		assertFalse(state.onGain());
		assertFalse(state.close());
	}
}
