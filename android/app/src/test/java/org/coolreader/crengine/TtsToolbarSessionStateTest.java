package org.coolreader.crengine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

public class TtsToolbarSessionStateTest {
	@Test
	public void oneShotCleanupFinishAndClose() {
		TtsToolbarSessionState state = new TtsToolbarSessionState();
		AtomicInteger closes = new AtomicInteger();
		state.setOnCloseListener(closes::incrementAndGet);
		assertTrue(state.beginDocumentCleanup());
		assertFalse(state.beginDocumentCleanup());
		assertTrue(state.beginFinishClose());
		assertFalse(state.beginFinishClose());
		Runnable onClose = state.takeOnCloseListener();
		assertNull(state.takeOnCloseListener());
		// set after finish is ignored
		state.setOnCloseListener(closes::incrementAndGet);
		assertNull(state.takeOnCloseListener());
		if (onClose != null)
			onClose.run();
		assertEquals(1, closes.get());
		assertTrue(state.close());
		assertTrue(state.isClosed());
		assertFalse(state.beginDocumentCleanup());
		assertFalse(state.beginFinishClose());
		assertFalse(state.close());
	}
}
