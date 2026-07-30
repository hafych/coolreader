package org.coolreader.crengine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ReaderImageViewerStateTest {
	@Test
	public void snapshotsDoNotExposeOwnedGeometry() {
		ReaderImageViewerState<Object> state =
				new ReaderImageViewerState<>();
		ImageInfo source = image(10, 20);
		Object viewer = new Object();
		assertTrue(state.startIfIdle(viewer, source));

		source.x = 100;
		ImageInfo first = state.snapshot(viewer);
		first.y = 200;
		ImageInfo second = state.snapshot(viewer);

		assertEquals(10, second.x);
		assertEquals(20, second.y);
	}

	@Test
	public void onlyExactViewerCanOwnAnActiveSession() {
		ReaderImageViewerState<Object> state =
				new ReaderImageViewerState<>();
		Object first = new Object();
		Object competing = new Object();
		assertTrue(state.startIfIdle(first, image(1, 2)));
		assertFalse(state.startIfIdle(
				competing, image(3, 4)));
		assertSame(first, state.current());
		assertTrue(state.isActive(first));
		assertFalse(state.isActive(competing));
	}

	@Test
	public void staleViewerCannotReadUpdateOrFinishReplacement() {
		ReaderImageViewerState<Object> state =
				new ReaderImageViewerState<>();
		Object first = new Object();
		Object second = new Object();
		assertTrue(state.startIfIdle(first, image(1, 2)));
		assertTrue(state.finish(first));
		assertTrue(state.startIfIdle(second, image(3, 4)));

		assertNull(state.snapshot(first));
		assertFalse(state.update(first, image(5, 6)));
		assertFalse(state.finish(first));
		assertTrue(state.isActive(second));
		assertEquals(3, state.snapshot(second).x);
	}

	@Test
	public void updateCopiesCandidateAndRejectsDuplicate() {
		ReaderImageViewerState<Object> state =
				new ReaderImageViewerState<>();
		Object viewer = new Object();
		assertTrue(state.startIfIdle(viewer, image(1, 2)));
		ImageInfo candidate = image(7, 8);

		assertTrue(state.update(viewer, candidate));
		candidate.x = 99;
		assertEquals(7, state.snapshot(viewer).x);
		assertFalse(state.update(viewer, image(7, 8)));
	}

	@Test
	public void bufferSnapshotUsesPositiveLatestDimensions() {
		ReaderImageViewerState<Object> state =
				new ReaderImageViewerState<>();
		Object viewer = new Object();
		assertTrue(state.startIfIdle(viewer, image(1, 2)));

		ImageInfo buffered =
				state.snapshotForBuffer(viewer, 0, -1);
		buffered.bufWidth = 50;

		ImageInfo stored = state.snapshot(viewer);
		assertEquals(1, stored.bufWidth);
		assertEquals(1, stored.bufHeight);
	}

	@Test
	public void finishIsExactAndCloseIsPermanent() {
		ReaderImageViewerState<Object> state =
				new ReaderImageViewerState<>();
		Object first = new Object();
		Object second = new Object();
		assertTrue(state.startIfIdle(first, image(1, 2)));

		assertTrue(state.finish(first));
		assertFalse(state.finish(first));
		assertTrue(state.startIfIdle(second, image(3, 4)));

		assertSame(second, state.close());

		assertNull(state.close());
		assertNull(state.current());
		assertFalse(state.isActive(second));
		assertFalse(state.startIfIdle(
				new Object(), image(5, 6)));
	}

	@Test
	public void nullNeverBecomesViewerIdentity() {
		ReaderImageViewerState<Object> state =
				new ReaderImageViewerState<>();

		assertFalse(state.startIfIdle(null, image(1, 2)));
		assertFalse(state.startIfIdle(new Object(), null));
		assertFalse(state.isActive(null));
		assertNull(state.snapshot(null));
		assertFalse(state.finish(null));
	}

	private static ImageInfo image(int x, int y) {
		ImageInfo image = new ImageInfo();
		image.width = 100;
		image.height = 200;
		image.scaledWidth = 100;
		image.scaledHeight = 200;
		image.x = x;
		image.y = y;
		image.bufWidth = 300;
		image.bufHeight = 400;
		image.bufDpi = 160;
		return image;
	}
}
