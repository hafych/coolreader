package org.coolreader;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class TtsServiceConnectionStateTest {
	@Test
	public void enginePackageUpdatesUntilClose() {
		TtsServiceConnectionState state =
				new TtsServiceConnectionState();

		assertEquals("", state.getEnginePackage());
		state.setEnginePackage("com.example.tts");
		assertEquals("com.example.tts", state.getEnginePackage());
		state.setEnginePackage(null);
		assertEquals("", state.getEnginePackage());
		assertNull(state.getAccessor());
		assertFalse(state.isClosed());
	}

	@Test
	public void closeClearsPackageAndRejectsFurtherUpdates() {
		TtsServiceConnectionState state =
				new TtsServiceConnectionState();
		state.setEnginePackage("com.example.tts");

		assertNull(state.close());
		assertTrue(state.isClosed());
		assertEquals("", state.getEnginePackage());
		assertNull(state.getAccessor());
		assertNull(state.ensureAccessor(() -> null));
		state.setEnginePackage("com.example.other");
		assertEquals("", state.getEnginePackage());
		assertNull(state.close());
	}

	@Test(expected = IllegalArgumentException.class)
	public void ensureAccessorRejectsNullFactory() {
		new TtsServiceConnectionState().ensureAccessor(null);
	}
}
