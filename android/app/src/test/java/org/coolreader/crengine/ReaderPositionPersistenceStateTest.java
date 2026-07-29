package org.coolreader.crengine;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ReaderPositionPersistenceStateTest {
	@Test
	public void bookMustBeTheExactCurrentIdentity() {
		ReaderPositionPersistenceState<String> state =
				new ReaderPositionPersistenceState<>();
		String current = new String("book");
		String equalReplacement = new String("book");
		state.replace(current);

		assertNull(state.begin(equalReplacement, "position"));
		assertNotNull(state.begin(current, "position"));
	}

	@Test
	public void completedPositionIsSuppressedIncludingNull() {
		ReaderPositionPersistenceState<Object> state =
				new ReaderPositionPersistenceState<>();
		Object book = new Object();
		state.replace(book);
		ReaderPositionPersistenceState.Request<Object> request =
				state.begin(book, null);

		assertNotNull(request);
		assertTrue(state.complete(request));
		assertNull(state.begin(book, null));
		assertNotNull(state.begin(book, "next"));
	}

	@Test
	public void replacementRejectsOldCompletionAndCancellation() {
		ReaderPositionPersistenceState<Object> state =
				new ReaderPositionPersistenceState<>();
		Object firstBook = new Object();
		Object secondBook = new Object();
		state.replace(firstBook);
		ReaderPositionPersistenceState.Request<Object> first =
				state.begin(firstBook, "first");

		state.replace(secondBook);
		ReaderPositionPersistenceState.Request<Object> second =
				state.begin(secondBook, "second");

		assertFalse(state.complete(first));
		assertFalse(state.cancel(first));
		assertTrue(state.complete(second));
		assertNull(state.begin(secondBook, "second"));
	}

	@Test
	public void newerRequestCannotBeClearedByStaleRequest() {
		ReaderPositionPersistenceState<Object> state =
				new ReaderPositionPersistenceState<>();
		Object book = new Object();
		state.replace(book);
		ReaderPositionPersistenceState.Request<Object> first =
				state.begin(book, "first");
		ReaderPositionPersistenceState.Request<Object> second =
				state.begin(book, "second");

		assertFalse(state.complete(first));
		assertFalse(state.cancel(first));
		assertTrue(state.complete(second));
	}

	@Test
	public void invalidateOnlyAffectsMatchingBook() {
		ReaderPositionPersistenceState<Object> state =
				new ReaderPositionPersistenceState<>();
		Object firstBook = new Object();
		Object secondBook = new Object();
		state.replace(secondBook);
		ReaderPositionPersistenceState.Request<Object> saved =
				state.begin(secondBook, "position");
		assertTrue(state.complete(saved));

		state.invalidate(firstBook);
		assertNull(state.begin(secondBook, "position"));

		state.invalidate(secondBook);
		assertNotNull(state.begin(secondBook, "position"));
	}

	@Test
	public void closeRejectsPendingAndFutureWork() {
		ReaderPositionPersistenceState<Object> state =
				new ReaderPositionPersistenceState<>();
		Object book = new Object();
		state.replace(book);
		ReaderPositionPersistenceState.Request<Object> pending =
				state.begin(book, "position");

		state.close();
		state.replace(book);

		assertFalse(state.complete(pending));
		assertFalse(state.cancel(pending));
		assertNull(state.begin(book, "position"));
	}
}
