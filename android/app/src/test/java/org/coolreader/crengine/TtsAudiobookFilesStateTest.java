package org.coolreader.crengine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.Test;

public class TtsAudiobookFilesStateTest {
	@Test
	public void setSnapshotClearAndClose() {
		TtsAudiobookFilesState state = new TtsAudiobookFilesState();
		File word = new File("a.wordtiming");
		File info = new File("a.sentenceinfo");
		File cache = new File("a.sentencetimingcache");
		state.set(word, info, cache);
		assertEquals(word, state.getWordTimingFile());
		assertEquals(info, state.getSentenceInfoFile());
		assertEquals(cache, state.getSentenceTimingCacheFile());
		TtsAudiobookFilesState.Snapshot snap = state.snapshot();
		assertEquals(word, snap.wordTimingFile);
		state.clear();
		assertNull(state.getWordTimingFile());
		state.set(word, info, cache);
		assertTrue(state.close());
		assertTrue(state.isClosed());
		state.set(word, info, cache);
		assertNull(state.getWordTimingFile());
		assertNull(state.snapshot().wordTimingFile);
		assertFalse(state.close());
	}
}
