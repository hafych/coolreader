package org.coolreader.crengine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.database.DataSetObserver;

import org.junit.Test;

public class DataSetObserverRegistryTest {
	@Test
	public void registerNotifyAndClose() {
		DataSetObserverRegistry registry =
				new DataSetObserverRegistry();
		final int[] changes = { 0 };
		DataSetObserver observer = new DataSetObserver() {
			@Override
			public void onChanged() {
				changes[0]++;
			}
		};
		registry.register(observer);
		registry.register(observer); // dedupe
		assertEquals(1, registry.snapshot().size());
		registry.notifyChanged();
		assertEquals(1, changes[0]);
		registry.unregister(observer);
		assertEquals(0, registry.snapshot().size());
		assertTrue(registry.close());
		assertTrue(registry.isClosed());
		registry.register(observer);
		assertEquals(0, registry.snapshot().size());
		assertFalse(registry.close());
	}
}
