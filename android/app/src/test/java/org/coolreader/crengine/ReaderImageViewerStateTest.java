package org.coolreader.crengine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ReaderImageViewerStateTest {
	@Test
	public void snapshotsDoNotExposeOwnedGeometry() {
		ReaderImageViewerState state = new ReaderImageViewerState();
		ImageInfo source = image(10, 20);
		ReaderImageViewerState.Session session =
				state.replace(source);

		source.x = 100;
		ImageInfo first = state.snapshot(session);
		first.y = 200;
		ImageInfo second = state.snapshot(session);

		assertEquals(10, second.x);
		assertEquals(20, second.y);
	}

	@Test
	public void staleSessionCannotReadUpdateOrFinishReplacement() {
		ReaderImageViewerState state = new ReaderImageViewerState();
		ReaderImageViewerState.Session first =
				state.replace(image(1, 2));
		ReaderImageViewerState.Session second =
				state.replace(image(3, 4));

		assertNull(state.snapshot(first));
		assertFalse(state.update(first, image(5, 6)));
		assertFalse(state.finish(first));
		assertTrue(state.isActive(second));
		assertEquals(3, state.snapshot(second).x);
	}

	@Test
	public void updateCopiesCandidateAndRejectsDuplicate() {
		ReaderImageViewerState state = new ReaderImageViewerState();
		ReaderImageViewerState.Session session =
				state.replace(image(1, 2));
		ImageInfo candidate = image(7, 8);

		assertTrue(state.update(session, candidate));
		candidate.x = 99;
		assertEquals(7, state.snapshot(session).x);
		assertFalse(state.update(session, image(7, 8)));
	}

	@Test
	public void bufferSnapshotUsesPositiveLatestDimensions() {
		ReaderImageViewerState state = new ReaderImageViewerState();
		ReaderImageViewerState.Session session =
				state.replace(image(1, 2));

		ImageInfo buffered =
				state.snapshotForBuffer(session, 0, -1);
		buffered.bufWidth = 50;

		ImageInfo stored = state.snapshot(session);
		assertEquals(1, stored.bufWidth);
		assertEquals(1, stored.bufHeight);
	}

	@Test
	public void finishIsExactAndCloseIsPermanent() {
		ReaderImageViewerState state = new ReaderImageViewerState();
		ReaderImageViewerState.Session session =
				state.replace(image(1, 2));

		assertTrue(state.finish(session));
		assertFalse(state.finish(session));
		assertNotNull(state.replace(image(3, 4)));

		state.close();

		assertNull(state.replace(image(5, 6)));
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
