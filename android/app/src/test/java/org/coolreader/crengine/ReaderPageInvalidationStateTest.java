package org.coolreader.crengine;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ReaderPageInvalidationStateTest {
	@Test
	public void initialCachePreparationMustInvalidate() {
		ReaderPageInvalidationState state =
				new ReaderPageInvalidationState();

		assertTrue(state.claim());
		assertFalse(state.claim());
	}

	@Test
	public void repeatedRequestsCoalesceBeforeClaim() {
		ReaderPageInvalidationState state =
				new ReaderPageInvalidationState();
		assertTrue(state.claim());

		state.invalidate();
		state.invalidate();

		assertTrue(state.claim());
		assertFalse(state.claim());
	}

	@Test
	public void requestAfterClaimRemainsPending() {
		ReaderPageInvalidationState state =
				new ReaderPageInvalidationState();

		assertTrue(state.claim());
		state.invalidate();

		assertTrue(state.claim());
		assertFalse(state.claim());
	}

	@Test
	public void closeRejectsPendingAndFutureRequests() {
		ReaderPageInvalidationState state =
				new ReaderPageInvalidationState();
		state.invalidate();

		state.close();
		state.invalidate();

		assertFalse(state.claim());
	}
}
