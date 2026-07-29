package org.coolreader.crengine;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class LibraryRootRequestStateTest {
	@Test
	public void addRequestOwnsNullablePreviousRoot() {
		LibraryRootRequestState<String> state =
				new LibraryRootRequestState<>();

		LibraryRootRequestState.Request<String> request =
				state.begin(null);

		assertSame(request, state.peek());
		assertNull(request.getPreviousRoot());
		assertTrue(state.isPending());
	}

	@Test
	public void reselectRequestCapturesExactPreviousRoot() {
		LibraryRootRequestState<String> state =
				new LibraryRootRequestState<>();

		LibraryRootRequestState.Request<String> request =
				state.begin("content://old");

		assertEquals(
				"content://old",
				request.getPreviousRoot());
	}

	@Test
	public void overlappingRequestCannotReplaceOwner() {
		LibraryRootRequestState<String> state =
				new LibraryRootRequestState<>();
		LibraryRootRequestState.Request<String> first =
				state.begin("content://first");

		assertNull(state.begin("content://second"));
		assertSame(first, state.peek());
	}

	@Test
	public void takeAtomicallyClearsDeliveredOwner() {
		LibraryRootRequestState<String> state =
				new LibraryRootRequestState<>();
		LibraryRootRequestState.Request<String> request =
				state.begin(null);

		assertSame(request, state.take());
		assertNull(state.take());
		assertFalse(state.isPending());
	}

	@Test
	public void staleCancelCannotClearReplacement() {
		LibraryRootRequestState<String> state =
				new LibraryRootRequestState<>();
		LibraryRootRequestState.Request<String> stale =
				state.begin("content://old");
		state.take();
		LibraryRootRequestState.Request<String> current =
				state.begin("content://new");

		assertFalse(state.cancel(stale));
		assertSame(current, state.peek());
		assertTrue(state.cancel(current));
	}

	@Test
	public void closePermanentlyRejectsRestoredOrNewWork() {
		LibraryRootRequestState<String> state =
				new LibraryRootRequestState<>();
		state.begin(null);

		assertTrue(state.close());
		assertFalse(state.close());
		assertTrue(state.isClosed());
		assertFalse(state.isPending());
		assertNull(state.begin("content://new"));
	}
}
