package org.coolreader.crengine;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class ReaderBookInfoSnapshotTest {
	@Test
	public void missingBookOrFileCannotProduceSnapshot() {
		assertNull(ReaderBookInfoSnapshot.capture(
				"1.0", 50, "12:00", null));
		assertNull(ReaderBookInfoSnapshot.capture(
				"1.0", 50, "12:00",
				new BookInfo((FileInfo) null)));
	}

	@Test
	public void valueSnapshotBuildsStableReaderInformation() {
		ReaderBookInfoSnapshot snapshot =
				completeSnapshot();

		List<String> items = snapshot.buildItems(
				bookmark("Chapter 4"),
				position(4, 10, 500, 1200, 200));

		assertEquals(Arrays.asList(
				"section=section.system",
				"system.version=Cool Reader 3.2",
				"system.battery=87%",
				"system.time=12:34",
				"section=section.file",
				"file.name=book.fb2",
				"file.path=/library",
				"file.size=2048",
				"file.arcname=books.zip",
				"file.arcpath=/archives",
				"file.arcsize=1024",
				"file.format=FB2",
				"section=section.position",
				"position.page=5 / 10",
				"position.percent=50.0%",
				"position.chapter=Chapter 4",
				"section=section.book",
				"book.authors=Author",
				"book.title=Title",
				"book.series=Series #2",
				"book.language=uk",
				"book.genres=fiction|sf"), items);
		assertThrows(
				UnsupportedOperationException.class,
				() -> items.add("book.title=Mutation"));
	}

	@Test
	public void positionRequiresBothBookmarkAndProperties() {
		ReaderBookInfoSnapshot snapshot =
				completeSnapshot();

		List<String> withoutBookmark =
				snapshot.buildItems(
						null,
						position(0, 1, 0, 100, 100));
		List<String> withoutPosition =
				snapshot.buildItems(
						bookmark("Chapter"),
						null);

		assertFalse(withoutBookmark.contains(
				"section=section.position"));
		assertFalse(withoutPosition.contains(
				"section=section.position"));
		assertTrue(withoutBookmark.contains(
				"section=section.book"));
	}

	@Test
	public void chapterTextIsBoundedAndOptionalValuesAreSkipped() {
		ReaderBookInfoSnapshot snapshot =
				ReaderBookInfoSnapshot.fromValues(
						null, 0, null,
						"/library/plain.txt", 0,
						null, 0,
						DocumentFormat.TXT,
						null, null, null, 0,
						null, null);
		String chapter = repeat("x", 101);

		List<String> items = snapshot.buildItems(
				bookmark(chapter),
				position(0, 0, 0, 0, 0));

		assertTrue(items.contains(
				"position.chapter="
						+ repeat("x", 100)
						+ "..."));
		assertFalse(items.toString().contains("=null"));
		assertFalse(items.toString().contains("Cool Reader"));
	}

	private static ReaderBookInfoSnapshot completeSnapshot() {
		return ReaderBookInfoSnapshot.fromValues(
				"3.2", 87, "12:34",
				"/library/book.fb2", 2048,
				"/archives/books.zip", 1024,
				DocumentFormat.FB2,
				"Author", "Title", "Series", 2,
				"uk", "fiction|sf");
	}

	private static Bookmark bookmark(String title) {
		Bookmark bookmark = new Bookmark();
		bookmark.setTitleText(title);
		return bookmark;
	}

	private static PositionProperties position(
			int pageNumber,
			int pageCount,
			int y,
			int fullHeight,
			int pageHeight) {
		PositionProperties position = new PositionProperties();
		position.pageMode = 1;
		position.pageNumber = pageNumber;
		position.pageCount = pageCount;
		position.y = y;
		position.fullHeight = fullHeight;
		position.pageHeight = pageHeight;
		return position;
	}

	private static String repeat(String value, int count) {
		StringBuilder result = new StringBuilder();
		for (int i = 0; i < count; i++)
			result.append(value);
		return result.toString();
	}
}
