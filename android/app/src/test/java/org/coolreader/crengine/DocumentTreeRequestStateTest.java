package org.coolreader.crengine;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class DocumentTreeRequestStateTest {
	@Test
	public void beginCapturesCommandAndArgumentTogether() {
		DocumentTreeRequestState<String> state =
				new DocumentTreeRequestState<>();

		DocumentTreeRequestState.Request<String> request =
				state.begin(
						DocumentTreeRequestState.Command.DELETE_FILE,
						"book");

		assertSame(request, state.peek());
		assertEquals(
				DocumentTreeRequestState.Command.DELETE_FILE,
				request.getCommand());
		assertEquals("book", request.getArgument());
		assertEquals(0, request.getAttempt());
		assertTrue(state.isPending());
	}

	@Test
	public void folderAttemptIsCapturedWithItsTarget() {
		DocumentTreeRequestState<String> state =
				new DocumentTreeRequestState<>();

		DocumentTreeRequestState.Request<String> request =
				state.begin(
						DocumentTreeRequestState.Command.DELETE_FOLDER,
						"folder",
						2);

		assertSame(request, state.peek());
		assertEquals("folder", request.getArgument());
		assertEquals(2, request.getAttempt());
	}

	@Test
	public void overlappingLaunchCannotReplacePendingTarget() {
		DocumentTreeRequestState<String> state =
				new DocumentTreeRequestState<>();
		DocumentTreeRequestState.Request<String> first =
				state.begin(
						DocumentTreeRequestState.Command.DELETE_FOLDER,
						"first");

		assertNull(state.begin(
				DocumentTreeRequestState.Command.SAVE_LOGCAT,
				"second"));
		assertSame(first, state.peek());
		assertEquals("first", state.peek().getArgument());
	}

	@Test
	public void takeAtomicallyClearsDeliveredRequest() {
		DocumentTreeRequestState<String> state =
				new DocumentTreeRequestState<>();
		DocumentTreeRequestState.Request<String> request =
				state.begin(
						DocumentTreeRequestState.Command.SAVE_LOGCAT,
						"log");

		assertSame(request, state.take());
		assertNull(state.take());
		assertFalse(state.isPending());
	}

	@Test
	public void staleCancelCannotClearNewRequest() {
		DocumentTreeRequestState<String> state =
				new DocumentTreeRequestState<>();
		DocumentTreeRequestState.Request<String> stale =
				state.begin(
						DocumentTreeRequestState.Command.DELETE_FILE,
						"old");
		state.take();
		DocumentTreeRequestState.Request<String> current =
				state.begin(
						DocumentTreeRequestState.Command.DELETE_FILE,
						"new");

		assertFalse(state.cancel(stale));
		assertSame(current, state.peek());
		assertTrue(state.cancel(current));
		assertFalse(state.isPending());
	}

	@Test
	public void invalidCommandCodesAndArgumentsAreRejected() {
		DocumentTreeRequestState<String> state =
				new DocumentTreeRequestState<>();

		assertNull(DocumentTreeRequestState.Command.fromCode(-1));
		assertNull(state.begin(null, "item"));
		assertNull(state.begin(
				DocumentTreeRequestState.Command.DELETE_FILE,
				null));
		assertNull(state.begin(
				DocumentTreeRequestState.Command.DELETE_FOLDER,
				"folder",
				-1));
		for (DocumentTreeRequestState.Command command :
				DocumentTreeRequestState.Command.values()) {
			assertSame(
					command,
					DocumentTreeRequestState.Command.fromCode(
							command.getCode()));
		}
	}
}
