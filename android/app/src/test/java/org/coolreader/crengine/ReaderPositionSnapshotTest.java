package org.coolreader.crengine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class ReaderPositionSnapshotTest {
	@Test
	public void missingBookmarkCannotProduceSnapshot() {
		assertNull(ReaderPositionSnapshot.capture(null, 42L));
	}

	@Test
	public void captureOwnsAndNormalizesNativeBookmark() {
		Bookmark source = bookmark(
				"source", Bookmark.TYPE_COMMENT, 7L);

		ReaderPositionSnapshot snapshot =
				ReaderPositionSnapshot.capture(source, 123L);
		source.setStartPos("mutated-source");
		source.setType(Bookmark.TYPE_POSITION);
		source.setTimeStamp(999L);
		Bookmark captured = snapshot.copyBookmark();

		assertEquals("source", captured.getStartPos());
		assertEquals(
				Bookmark.TYPE_LAST_POSITION,
				captured.getType());
		assertEquals(123L, captured.getTimeStamp());
	}

	@Test
	public void callersReceiveIndependentBookmarkCopies() {
		ReaderPositionSnapshot snapshot =
				ReaderPositionSnapshot.capture(
						bookmark(
								"owned",
								Bookmark.TYPE_POSITION,
								1L),
						Long.MAX_VALUE);
		Bookmark first = snapshot.copyBookmark();
		first.setStartPos("mutated-copy");
		first.setTimeStamp(-1L);
		Bookmark second = snapshot.copyBookmark();

		assertEquals("owned", second.getStartPos());
		assertEquals(Long.MAX_VALUE, second.getTimeStamp());
		assertEquals(
				Bookmark.TYPE_LAST_POSITION,
				second.getType());
	}

	private static Bookmark bookmark(
			String startPos, int type, long timestamp) {
		Bookmark bookmark = new Bookmark();
		bookmark.setStartPos(startPos);
		bookmark.setType(type);
		bookmark.setTimeStamp(timestamp);
		return bookmark;
	}
}
