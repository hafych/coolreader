package org.coolreader.crengine;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

public class ReplaceableTaskSlotTest {
	@Test
	public void replacementInvalidatesOldWrapper() {
		ReplaceableTaskSlot slot = new ReplaceableTaskSlot();
		AtomicInteger firstRuns = new AtomicInteger();
		AtomicInteger secondRuns = new AtomicInteger();

		ReplaceableTaskSlot.Replacement first =
				slot.replace(firstRuns::incrementAndGet);
		ReplaceableTaskSlot.Replacement second =
				slot.replace(secondRuns::incrementAndGet);

		assertNull(first.previous());
		assertSame(first.current(), second.previous());
		first.current().run();
		assertEquals(0, firstRuns.get());
		second.current().run();
		assertEquals(1, secondRuns.get());
	}

	@Test
	public void claimedWrapperRunsOnlyOnceAndClearsSlot() {
		ReplaceableTaskSlot slot = new ReplaceableTaskSlot();
		AtomicInteger runs = new AtomicInteger();
		Runnable wrapper =
				slot.replace(runs::incrementAndGet).current();

		wrapper.run();
		wrapper.run();

		assertEquals(1, runs.get());
		assertNull(slot.cancel());
	}

	@Test
	public void cancelInvalidatesPendingWrapperIdempotently() {
		ReplaceableTaskSlot slot = new ReplaceableTaskSlot();
		AtomicInteger runs = new AtomicInteger();
		Runnable wrapper =
				slot.replace(runs::incrementAndGet).current();

		assertSame(wrapper, slot.cancel());
		assertNull(slot.cancel());
		wrapper.run();
		assertEquals(0, runs.get());
	}

	@Test
	public void runningDelegateCanScheduleNextGeneration() {
		ReplaceableTaskSlot slot = new ReplaceableTaskSlot();
		AtomicInteger runs = new AtomicInteger();
		Runnable[] next = new Runnable[1];
		Runnable first = slot.replace(() -> {
			runs.incrementAndGet();
			next[0] = slot.replace(runs::incrementAndGet).current();
		}).current();

		first.run();
		assertEquals(1, runs.get());
		next[0].run();
		assertEquals(2, runs.get());
		assertNull(slot.cancel());
	}
}
