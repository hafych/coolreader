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
		assertSize(400, 500, state.requestedSize());
	}

	@Test
	public void currentSizeCanBeRescheduledWithoutParallelFields() {
		ViewportResizeState state =
				new ViewportResizeState(100, 100);
		state.request(320, 480);

		ViewportResizeState.Request repeated =
				state.requestCurrent();

		assertNotNull(repeated);
		assertSame(state.requestedSize(), repeated.size());
		assertTrue(state.isCurrent(repeated));
	}

	@Test
	public void invalidDimensionsUseStablePositiveFallbacks() {
		ViewportResizeState state =
				new ViewportResizeState(0, -1);

		assertSize(80, 80, state.requestedSize());
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
	public void nativeApplyPublishesOneImmutableSizePair() {
		ViewportResizeState state =
				new ViewportResizeState(100, 100);
		ViewportResizeState.Request request =
				state.request(320, 480);

		assertNull(state.appliedSize());
		assertSize(320, 480, state.appliedOrRequestedSize());
		assertFalse(state.requestedIsApplied());
		assertTrue(state.beginApply(request));
		assertFalse(state.requestedIsApplied());
		assertTrue(state.finishApply(request));
		assertSize(320, 480, state.appliedSize());
		assertSame(state.appliedSize(), state.appliedOrRequestedSize());
		assertFalse(state.requestedIsApplied());
		assertTrue(state.completeCurrentApplied());
		assertTrue(state.requestedIsApplied());
	}

	@Test
	public void inFlightResizePreventsFalseAppliedCompletion() {
		ViewportResizeState state =
				new ViewportResizeState(100, 100);
		ViewportResizeState.Request initial =
				state.requestCurrent();
		assertTrue(state.beginApply(initial));
		assertTrue(state.finishApply(initial));
		assertTrue(state.completeCurrentApplied());

		ViewportResizeState.Request inFlight =
				state.request(200, 300);
		assertTrue(state.beginApply(inFlight));
		ViewportResizeState.Request latest =
				state.request(100, 100);

		assertFalse(state.completeIfApplied(latest));
		assertFalse(state.requestedIsApplied());
		assertTrue(state.finishApply(inFlight));
		assertSize(200, 300, state.appliedSize());
		assertTrue(state.isCurrent(latest));
		assertFalse(state.completeCurrentApplied());
		assertTrue(state.beginApply(latest));
		assertTrue(state.finishApply(latest));
		assertTrue(state.completeCurrentApplied());
		assertTrue(state.requestedIsApplied());
	}

	@Test
	public void equivalentLatestRequestClaimsInFlightCompletion() {
		ViewportResizeState state =
				new ViewportResizeState(100, 100);
		ViewportResizeState.Request inFlight =
				state.request(200, 300);
		assertTrue(state.beginApply(inFlight));
		ViewportResizeState.Request latest =
				state.request(200, 300);

		assertTrue(state.finishApply(inFlight));
		assertFalse(state.completeIfApplied(latest));
		assertTrue(state.completeCurrentApplied());
		assertFalse(state.isCurrent(latest));
		assertTrue(state.requestedIsApplied());
	}

	@Test
	public void failedApplyReleasesOnlyItsExactInFlightRequest() {
		ViewportResizeState state =
				new ViewportResizeState(100, 100);
		ViewportResizeState.Request request =
				state.request(200, 300);

		assertTrue(state.beginApply(request));
		assertTrue(state.cancelApply(request));
		assertFalse(state.cancelApply(request));
		assertTrue(state.isCurrent(request));
		assertFalse(state.requestedIsApplied());
	}

	@Test
	public void lazyApplyPublishesRequestedFallbackWithoutARequest() {
		ViewportResizeState state =
				new ViewportResizeState(100, 100);
		ViewportResizeState.Size requested =
				state.requestedSize();

		assertTrue(state.publishApplied(requested));
		assertSame(requested, state.appliedSize());
		assertTrue(state.requestedIsApplied());
		ViewportResizeState.Request repeated =
				state.requestCurrent();
		assertTrue(state.completeIfApplied(repeated));
		assertFalse(state.isCurrent(repeated));
	}

	@Test
	public void closePermanentlyRejectsQueuedAndNewRequests() {
		ViewportResizeState state =
				new ViewportResizeState(100, 100);
		ViewportResizeState.Request request =
				state.requestCurrent();
		assertTrue(state.beginApply(request));

		assertTrue(state.close());
		assertFalse(state.close());
		assertFalse(state.isCurrent(request));
		assertFalse(state.finishApply(request));
		assertFalse(state.cancelApply(request));
		assertFalse(state.publishApplied(request.size()));
		assertFalse(state.complete(request));
		assertNull(state.request(200, 300));
		assertNull(state.requestCurrent());
		assertSize(100, 100, state.requestedSize());
		assertNull(state.appliedSize());
	}

	private static void assertSize(
			int width,
			int height,
			ViewportResizeState.Size actual) {
		assertEquals(width, actual.width());
		assertEquals(height, actual.height());
	}
}
