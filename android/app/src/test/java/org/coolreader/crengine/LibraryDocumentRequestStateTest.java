package org.coolreader.crengine;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class LibraryDocumentRequestStateTest {
	@Test
	public void beginCapturesExactInitialRoot() {
		LibraryDocumentRequestState<String> state =
				new LibraryDocumentRequestState<>();

		LibraryDocumentRequestState.Request<String> request =
				state.begin("content://library");

		assertSame(request, state.peek());
		assertEquals(
				"content://library",
				request.getInitialRoot());
		assertTrue(state.isPending());
	}

	@Test
	public void invalidOrOverlappingRequestIsRejected() {
		LibraryDocumentRequestState<String> state =
				new LibraryDocumentRequestState<>();

		assertNull(state.begin(null));
		LibraryDocumentRequestState.Request<String> first =
				state.begin("content://first");
		assertNull(state.begin("content://second"));
		assertSame(first, state.peek());
	}

	@Test
	public void takeAtomicallyClearsDeliveredOwner() {
		LibraryDocumentRequestState<String> state =
				new LibraryDocumentRequestState<>();
		LibraryDocumentRequestState.Request<String> request =
				state.begin("content://library");

		assertSame(request, state.take());
		assertNull(state.take());
		assertFalse(state.isPending());
	}

	@Test
	public void staleCancelCannotClearReplacement() {
		LibraryDocumentRequestState<String> state =
				new LibraryDocumentRequestState<>();
		LibraryDocumentRequestState.Request<String> stale =
				state.begin("content://old");
		state.take();
		LibraryDocumentRequestState.Request<String> current =
				state.begin("content://new");

		assertFalse(state.cancel(stale));
		assertSame(current, state.peek());
		assertTrue(state.cancel(current));
	}

	@Test
	public void closePermanentlyRejectsLateAndNewWork() {
		LibraryDocumentRequestState<String> state =
				new LibraryDocumentRequestState<>();
		state.begin("content://library");

		assertTrue(state.close());
		assertFalse(state.close());
		assertTrue(state.isClosed());
		assertFalse(state.isPending());
		assertNull(state.begin("content://new"));
	}
}
