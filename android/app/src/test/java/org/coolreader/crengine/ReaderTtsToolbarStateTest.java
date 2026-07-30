package org.coolreader.crengine;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ReaderTtsToolbarStateTest {
	@Test
	public void onlyOneToolbarCanOwnShutdownAtATime() {
		ReaderTtsToolbarState<Object> state =
				new ReaderTtsToolbarState<>();
		Object first = new Object();
		Object competing = new Object();

		assertTrue(state.startIfIdle(first));
		assertFalse(state.startIfIdle(competing));
		assertFalse(state.finish(competing));
		assertSame(first, state.current());
	}

	@Test
	public void staleCloseCannotClearReplacement() {
		ReaderTtsToolbarState<Object> state =
				new ReaderTtsToolbarState<>();
		Object first = new Object();
		Object second = new Object();
		assertTrue(state.startIfIdle(first));
		assertTrue(state.finish(first));
		assertTrue(state.startIfIdle(second));

		assertFalse(state.finish(first));
		assertSame(second, state.current());
	}

	@Test
	public void activeToolbarRemainsOwnedUntilExactCloseCompletes() {
		ReaderTtsToolbarState<Object> state =
				new ReaderTtsToolbarState<>();
		Object toolbar = new Object();
		assertTrue(state.startIfIdle(toolbar));

		assertSame(toolbar, state.current());
		assertTrue(state.finish(toolbar));
		assertNull(state.current());
		assertFalse(state.finish(toolbar));
	}

	@Test
	public void closeReleasesToolbarAndIsPermanent() {
		ReaderTtsToolbarState<Object> state =
				new ReaderTtsToolbarState<>();
		Object toolbar = new Object();
		assertTrue(state.startIfIdle(toolbar));

		assertSame(toolbar, state.close());
		assertNull(state.close());
		assertNull(state.current());
		assertFalse(state.finish(toolbar));
		assertFalse(state.startIfIdle(new Object()));
	}

	@Test
	public void nullNeverBecomesToolbarIdentity() {
		ReaderTtsToolbarState<Object> state =
				new ReaderTtsToolbarState<>();

		assertFalse(state.startIfIdle(null));
		assertFalse(state.finish(null));
		assertNull(state.current());
	}
}
