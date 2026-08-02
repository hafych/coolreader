package org.coolreader.db;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

public class ServiceThreadStateTest {
	@Test
	public void enqueueDrainAndClose() {
		ServiceThreadState state = new ServiceThreadState();
		assertTrue(state.isStopped());
		AtomicInteger ran = new AtomicInteger();
		Runnable task = ran::incrementAndGet;
		assertTrue(state.enqueueIfStopped(task));
		assertEquals(1, state.queueSize());
		List<Runnable> drained = state.drainQueue();
		assertEquals(1, drained.size());
		assertEquals(0, state.queueSize());
		drained.get(0).run();
		assertEquals(1, ran.get());

		// Simulate start
		state.setStopped(false);
		// Without handler, enqueueIfStopped still queues when handler null
		assertTrue(state.enqueueIfStopped(task));

		assertTrue(state.close());
		assertTrue(state.isClosed());
		assertTrue(state.isStopped());
		assertNull(state.getHandler());
		assertTrue(state.enqueueIfStopped(task)); // swallowed
		assertEquals(0, state.queueSize());
		assertFalse(state.close());
	}
}
