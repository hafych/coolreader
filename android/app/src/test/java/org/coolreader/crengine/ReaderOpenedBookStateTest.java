package org.coolreader.crengine;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ReaderOpenedBookStateTest {
	private static BookInfo book() {
		return new BookInfo((FileInfo) null);
	}

	@Test
	public void bindKeepsClosedUntilPublishOpened() {
		ReaderOpenedBookState state = new ReaderOpenedBookState();
		BookInfo book = book();

		ReaderOpenedBookState.Snapshot bound = state.bind(book);

		assertSame(book, bound.book());
		assertFalse(bound.isOpened());
		assertSame(book, state.book());
		assertFalse(state.isOpened());
	}

	@Test
	public void publishOpenedPublishesBookAndOpenedTogether() {
		ReaderOpenedBookState state = new ReaderOpenedBookState();
		BookInfo book = book();

		ReaderOpenedBookState.Snapshot published =
				state.publishOpened(book);

		assertSame(book, published.book());
		assertTrue(published.isOpened());
		assertTrue(state.isOpened());
		assertSame(book, state.book());
	}

	@Test
	public void bindCanReplaceBookWhileOpened() {
		ReaderOpenedBookState state = new ReaderOpenedBookState();
		BookInfo first = book();
		BookInfo second = book();
		state.publishOpened(first);

		ReaderOpenedBookState.Snapshot replaced = state.bind(second);

		assertSame(second, replaced.book());
		assertTrue(replaced.isOpened());
		assertSame(second, state.book());
		assertTrue(state.isOpened());
	}

	@Test
	public void markClosedClearsOpenedButKeepsBookIdentity() {
		ReaderOpenedBookState state = new ReaderOpenedBookState();
		BookInfo book = book();
		state.publishOpened(book);

		assertTrue(state.markClosed());
		assertFalse(state.isOpened());
		assertSame(book, state.book());
		assertFalse(state.markClosed());
	}

	@Test
	public void clearIfOnlyClearsExactBook() {
		ReaderOpenedBookState state = new ReaderOpenedBookState();
		BookInfo first = book();
		BookInfo second = book();
		state.publishOpened(first);

		assertFalse(state.clearIf(second));
		assertSame(first, state.book());
		assertTrue(state.isOpened());

		assertTrue(state.clearIf(first));
		assertNull(state.book());
		assertFalse(state.isOpened());
	}

	@Test
	public void closeIsPermanentAndReleasesIdentity() {
		ReaderOpenedBookState state = new ReaderOpenedBookState();
		BookInfo book = book();
		state.publishOpened(book);

		assertSame(book, state.close().book());
		assertNull(state.book());
		assertFalse(state.isOpened());
		assertTrue(state.isClosed());
		assertNull(state.bind(book()).book());
		assertNull(state.publishOpened(book()).book());
		assertFalse(state.markClosed());
		assertFalse(state.clearIf(book));
	}

	@Test
	public void publishOpenedRejectsNullBook() {
		ReaderOpenedBookState state = new ReaderOpenedBookState();
		BookInfo book = book();
		state.publishOpened(book);

		ReaderOpenedBookState.Snapshot rejected =
				state.publishOpened(null);

		assertSame(book, rejected.book());
		assertTrue(rejected.isOpened());
	}
}
