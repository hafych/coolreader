package org.coolreader.crengine;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class FreezableRegistryTest {
	@Test
	public void snapshotsDoNotExposeBuilderStorage() {
		FreezableRegistry<String> registry = new FreezableRegistry<>();
		assertTrue(registry.add("first"));

		List<String> snapshot = registry.snapshot();
		snapshot.clear();

		assertEquals(
				java.util.Collections.singletonList("first"),
				registry.snapshot());
	}

	@Test
	public void freezeIsIdempotentAndRejectsLatePublication() {
		FreezableRegistry<String> registry = new FreezableRegistry<>();
		registry.add("first");
		List<String> firstFreeze = registry.freeze();
		List<String> secondFreeze = registry.freeze();

		assertTrue(registry.isFrozen());
		assertTrue(firstFreeze == secondFreeze);
		assertFalse(registry.add("late"));
		assertEquals(
				java.util.Collections.singletonList("first"),
				registry.snapshot());
		try {
			firstFreeze.add("mutation");
			fail("Frozen registry accepted mutation");
		} catch (UnsupportedOperationException expected) {
			// Expected: the published process snapshot is immutable.
		}
	}

	@Test
	public void nullItemsAreRejectedBeforePublication() {
		FreezableRegistry<String> registry = new FreezableRegistry<>();
		try {
			registry.add(null);
			fail("Null registry item was accepted");
		} catch (IllegalArgumentException expected) {
			assertEquals(0, registry.snapshot().size());
		}
	}

	@Test
	public void closeIsPermanentAndFreezesBuilder() {
		FreezableRegistry<String> registry = new FreezableRegistry<>();
		registry.add("first");
		assertTrue(registry.close());
		assertTrue(registry.isClosed());
		assertTrue(registry.isFrozen());
		assertFalse(registry.add("late"));
		assertEquals(
				java.util.Collections.singletonList("first"),
				registry.snapshot());
		assertFalse(registry.close());
	}
}
