package org.coolreader.tts;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.Test;

public class TtsAudiobookStateTest {
	@Test
	public void nullFileDisablesAndSameFileDoesNotRequestStop() {
		TtsAudiobookState state = new TtsAudiobookState();
		assertFalse(state.isEnabled());
		assertFalse(state.set(null, 1.5));
		assertFalse(state.isEnabled());
		assertEquals(1.5, state.startTime(), 0);

		File first = new File("/tmp/a.mp3");
		assertFalse(state.set(first, 2));
		assertTrue(state.isEnabled());
		assertEquals(first, state.audioFile());
		// Same path identity: no stop request.
		assertFalse(state.set(new File("/tmp/a.mp3"), 3));
		assertEquals(3, state.startTime(), 0);
	}

	@Test
	public void differentFileRequestsStop() {
		TtsAudiobookState state = new TtsAudiobookState();
		state.set(new File("/tmp/a.mp3"), 0);
		assertTrue(state.set(new File("/tmp/b.mp3"), 4));
		assertEquals(new File("/tmp/b.mp3"), state.audioFile());
		assertTrue(state.set(null, 0));
		assertFalse(state.isEnabled());
		assertNull(state.audioFile());
	}

	@Test
	public void closeDropsSlot() {
		TtsAudiobookState state = new TtsAudiobookState();
		state.set(new File("/tmp/a.mp3"), 9);
		assertTrue(state.close());
		assertTrue(state.isClosed());
		assertFalse(state.isEnabled());
		assertNull(state.audioFile());
		assertEquals(0, state.startTime(), 0);
		assertFalse(state.set(new File("/tmp/b.mp3"), 1));
		assertFalse(state.close());
	}
}
