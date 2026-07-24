package org.coolreader.crengine;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ServiceLifecycleTest {
	@Test
	public void closeInvalidatesOnlyItsOwnGeneration() {
		ServiceLifecycle first = new ServiceLifecycle(1);
		ServiceLifecycle second = new ServiceLifecycle(2);

		first.close();

		assertFalse(first.isActive());
		assertTrue(second.isActive());
	}
}
