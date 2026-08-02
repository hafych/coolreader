package org.coolreader.crengine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

public class CancelActionStateTest {
	@Test
	public void setTakeAndClose() {
		CancelActionState state = new CancelActionState();
		AtomicInteger runs = new AtomicInteger();
		Runnable action = runs::incrementAndGet;
		state.set(action);
		assertEquals(action, state.get());
		Runnable taken = state.take();
		assertEquals(action, taken);
		assertNull(state.get());
		assertNull(state.take());
		state.set(action);
		state.clear();
		assertNull(state.get());
		state.set(action);
		assertTrue(state.close());
		assertTrue(state.isClosed());
		assertNull(state.get());
		assertNull(state.take());
		state.set(action);
		assertNull(state.get());
		assertEquals(0, runs.get());
		assertFalse(state.close());
	}
}
