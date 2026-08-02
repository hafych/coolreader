package org.coolreader.tts;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

public class TtsBindingSessionStateTest {
	@Test
	public void bindPendingUnbindAndClose() {
		TtsBindingSessionState state = new TtsBindingSessionState();
		assertTrue(state.beginBinding());
		assertFalse(state.beginBinding());
		assertTrue(state.isBindingRegistered());

		AtomicInteger ran = new AtomicInteger();
		TTSControlBinder.Callback cb = binder -> ran.incrementAndGet();
		state.addPending(cb);
		List<TTSControlBinder.Callback> pending = state.takePending();
		assertEquals(1, pending.size());
		assertEquals(0, state.takePending().size());

		assertTrue(state.unbind());
		assertFalse(state.isBindingRegistered());
		assertNull(state.getBinder());

		assertTrue(state.close());
		assertTrue(state.isClosed());
		assertFalse(state.beginBinding());
		assertFalse(state.close());
	}
}
