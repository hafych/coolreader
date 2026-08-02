package org.coolreader.crengine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class CoverpageListenerRegistryTest {
	@Test
	public void addRemoveSnapshotAndClose() {
		CoverpageListenerRegistry registry =
				new CoverpageListenerRegistry();
		final int[] calls = { 0 };
		CoverpageManager.CoverpageReadyListener listener =
				files -> calls[0]++;

		registry.add(listener);
		registry.add(listener); // dedupe
		assertEquals(1, registry.snapshot().size());
		registry.snapshot().get(0).onCoverpagesReady(new ArrayList<>());
		assertEquals(1, calls[0]);

		registry.remove(listener);
		assertEquals(0, registry.snapshot().size());

		registry.add(listener);
		assertTrue(registry.close());
		assertTrue(registry.isClosed());
		assertEquals(0, registry.snapshot().size());
		registry.add(listener);
		assertEquals(0, registry.snapshot().size());
		assertFalse(registry.close());
	}
}
