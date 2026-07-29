package org.coolreader.tts;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class TtsInitializationStateTest {
	@Test
	public void replacementMakesLatestEngineTheOnlyOwner() {
		TtsInitializationState state = new TtsInitializationState();
		TtsInitializationState.Request first =
				state.replace("engine.one");
		TtsInitializationState.Request second =
				state.replace("engine.two");

		assertEquals("engine.one", first.engine());
		assertEquals("engine.two", second.engine());
		assertFalse(state.isActive(first));
		assertFalse(state.complete(first));
		assertTrue(state.isActive(second));
		assertTrue(state.complete(second));
	}

	@Test
	public void nullDefaultEngineStillHasExactIdentity() {
		TtsInitializationState state = new TtsInitializationState();
		TtsInitializationState.Request request = state.replace(null);

		assertNotNull(request);
		assertNull(request.engine());
		assertTrue(state.complete(request));
		assertFalse(state.complete(request));
	}

	@Test
	public void closeInvalidatesPendingAttemptPermanently() {
		TtsInitializationState state = new TtsInitializationState();
		TtsInitializationState.Request request =
				state.replace("engine");

		assertTrue(state.close());
		assertTrue(state.isClosed());
		assertFalse(state.isActive(request));
		assertFalse(state.complete(request));
		assertFalse(state.close());
		assertNull(state.replace("replacement"));
	}
}
