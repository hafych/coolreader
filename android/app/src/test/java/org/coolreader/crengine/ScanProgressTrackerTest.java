package org.coolreader.crengine;

import org.junit.Test;

import java.util.ArrayList;

import static org.junit.Assert.assertEquals;

public class ScanProgressTrackerTest {
	@Test
	public void phasesAreMappedAndMonotonic() {
		RecordingSink sink = new RecordingSink();
		ScanProgressTracker progress =
				new ScanProgressTracker(sink);

		progress.setDiscoveryProgress(1_500);
		progress.setDiscoveryProgress(1_000);
		progress.setDiscoveryProgress(3_500);
		progress.setMetadataProgress(32, 64);
		progress.setMetadataProgress(64, 64);

		assertEquals(
				java.util.Arrays.asList(
						1_500, 1_500, 3_000, 6_500, 10_000),
				sink.values);
	}

	@Test
	public void hideIsIdempotentAndFinal() {
		RecordingSink sink = new RecordingSink();
		ScanProgressTracker progress =
				new ScanProgressTracker(sink);

		progress.hide();
		progress.hide();
		progress.setMetadataProgress(1, 1);

		assertEquals(1, sink.hideCount);
		assertEquals(0, sink.values.size());
	}

	private static final class RecordingSink
			implements ScanProgressTracker.Sink {
		final ArrayList<Integer> values = new ArrayList<>();
		int hideCount;

		@Override
		public void setProgress(int progress) {
			values.add(progress);
		}

		@Override
		public void hide() {
			hideCount++;
		}
	}
}
