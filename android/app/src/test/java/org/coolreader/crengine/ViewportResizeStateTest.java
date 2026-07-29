package org.coolreader.crengine;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class ViewportResizeStateTest {
	@Test
	public void latestRequestWinsByIdentityWithItsOwnSize() {
		ViewportResizeState state =
				new ViewportResizeState(100, 100);
		ViewportResizeState.Request stale =
				state.request(200, 300);
		ViewportResizeState.Request current =
				state.request(400, 500);

		assertFalse(state.isCurrent(stale));
		assertTrue(state.isCurrent(current));
		assertSize(200, 300, stale.size());
		assertSize(400, 500, current.size());
		assertSize(400, 500, state.size());
	}

	@Test
	public void currentSizeCanBeRescheduledWithoutParallelFields() {
		ViewportResizeState state =
				new ViewportResizeState(100, 100);
		state.request(320, 480);

		ViewportResizeState.Request repeated =
				state.requestCurrent();

		assertNotNull(repeated);
		assertSame(state.size(), repeated.size());
		assertTrue(state.isCurrent(repeated));
	}

	@Test
	public void invalidDimensionsUseStablePositiveFallbacks() {
		ViewportResizeState state =
				new ViewportResizeState(0, -1);

		assertSize(80, 80, state.size());
		assertSize(
				80,
				Integer.MAX_VALUE,
				state.request(
						Integer.MIN_VALUE,
						Integer.MAX_VALUE).size());
	}

	@Test
	public void completionClearsOnlyItsExactRequest() {
		ViewportResizeState state =
				new ViewportResizeState(100, 100);
		ViewportResizeState.Request stale =
				state.request(200, 300);
		ViewportResizeState.Request current =
				state.request(400, 500);

		assertFalse(state.complete(stale));
		assertTrue(state.isCurrent(current));
		assertTrue(state.complete(current));
		assertFalse(state.complete(current));
	}

	@Test
	public void closePermanentlyRejectsQueuedAndNewRequests() {
		ViewportResizeState state =
				new ViewportResizeState(100, 100);
		ViewportResizeState.Request request =
				state.requestCurrent();

		assertTrue(state.close());
		assertFalse(state.close());
		assertFalse(state.isCurrent(request));
		assertFalse(state.complete(request));
		assertNull(state.request(200, 300));
		assertNull(state.requestCurrent());
		assertSize(100, 100, state.size());
	}

	private static void assertSize(
			int width,
			int height,
			ViewportResizeState.Size actual) {
		assertEquals(width, actual.width());
		assertEquals(height, actual.height());
	}
}
