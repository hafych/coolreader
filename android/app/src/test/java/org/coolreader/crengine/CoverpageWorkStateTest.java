package org.coolreader.crengine;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;

import org.junit.Test;

public class CoverpageWorkStateTest {
	@Test
	public void taskOwnershipAndClose() {
		CoverpageWorkState state = new CoverpageWorkState();
		Runnable first = new Runnable() {
			@Override
			public void run() {
			}
		};
		Runnable second = new Runnable() {
			@Override
			public void run() {
			}
		};

		state.setLastCheckCacheTask(first);
		state.setLastScanFileTask(first);
		state.setLastReadyNotifyTask(first);
		assertSame(first, state.getLastCheckCacheTask());
		assertSame(first, state.getLastScanFileTask());
		assertSame(first, state.getLastReadyNotifyTask());

		// Stale task does not claim queue work.
		assertNull(state.nextCheckCacheIfCurrent(second));
		assertNull(state.nextScanFileIfCurrent(second));

		state.setLastCheckCacheTask(second);
		assertSame(second, state.getLastCheckCacheTask());
		assertNull(state.nextCheckCacheIfCurrent(first));

		ArrayList<CoverpageManager.ImageItem> empty =
				state.drainReady();
		assertTrue(empty.isEmpty());

		assertTrue(state.close());
		assertTrue(state.isClosed());
		assertNull(state.getLastCheckCacheTask());
		assertNull(state.getLastScanFileTask());
		assertNull(state.getLastReadyNotifyTask());
		state.setLastCheckCacheTask(first);
		assertNull(state.getLastCheckCacheTask());
		assertFalse(state.addCheckCacheOnTop(null));
		assertFalse(state.close());
	}
}
