package org.coolreader.crengine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

public class TerminalCallbackPairStateTest {
	@Test
	public void takeBothTakeFailureAndClose() {
		TerminalCallbackPairState state =
				new TerminalCallbackPairState();
		AtomicInteger success = new AtomicInteger();
		AtomicInteger failure = new AtomicInteger();
		state.set(success::incrementAndGet, failure::incrementAndGet);
		TerminalCallbackPairState.Snapshot both = state.takeBoth();
		assertNull(state.takeBoth().success);
		if (both.success != null)
			both.success.run();
		assertEquals(1, success.get());
		assertEquals(0, failure.get());

		state.set(success::incrementAndGet, failure::incrementAndGet);
		Runnable failOnly = state.takeFailure();
		if (failOnly != null)
			failOnly.run();
		assertEquals(1, success.get());
		assertEquals(1, failure.get());

		state.set(success::incrementAndGet, failure::incrementAndGet);
		assertTrue(state.close());
		assertTrue(state.isClosed());
		assertNull(state.takeBoth().success);
		assertNull(state.takeFailure());
		assertFalse(state.close());
	}
}
