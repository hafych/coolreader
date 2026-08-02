package org.coolreader.crengine;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ScannerScanOptionsStateTest {
	@Test
	public void optionsUntilClose() {
		ScannerScanOptionsState state = new ScannerScanOptionsState();
		assertTrue(state.isDirScanEnabled());
		assertTrue(state.isHideEmptyDirs());

		state.setDirScanEnabled(false);
		state.setHideEmptyDirs(false);
		assertFalse(state.isDirScanEnabled());
		assertFalse(state.isHideEmptyDirs());

		assertTrue(state.close());
		assertTrue(state.isClosed());
		state.setDirScanEnabled(true);
		state.setHideEmptyDirs(true);
		assertFalse(state.isDirScanEnabled());
		assertFalse(state.isHideEmptyDirs());
		assertFalse(state.close());
	}
}
