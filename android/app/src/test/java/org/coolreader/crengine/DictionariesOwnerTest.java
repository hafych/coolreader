package org.coolreader.crengine;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.coolreader.Dictionaries;
import org.junit.Test;

/**
 * Pure ownership contracts. Real {@link Dictionaries} construction needs an
 * Activity; exclusive install/close is validated with null install rejection
 * and permanent close (same pattern as {@link BaseServiceDependenciesState}).
 */
public class DictionariesOwnerTest {
	@Test
	public void installNullRejectedAndCloseIsPermanent() {
		DictionariesOwner owner = new DictionariesOwner();
		assertFalse(owner.install(null));
		assertNull(owner.get());
		assertTrue(owner.close());
		assertTrue(owner.isClosed());
		assertNull(owner.get());
		assertFalse(owner.install(null));
		assertFalse(owner.close());
	}
}
