package org.coolreader.crengine;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Exercises {@link CrdbServiceConnectionState} ownership without constructing
 * a real {@link CRDBServiceAccessor} (requires Android Activity). Ensure-once
 * with a live accessor is covered by BaseActivity wiring markers; this test
 * drives the shipped owner entry points for permanent close and factory
 * rejection.
 */
public class CrdbServiceConnectionStateTest {
	@Test
	public void closeIsPermanentAndRejectsFurtherEnsure() {
		CrdbServiceConnectionState state =
				new CrdbServiceConnectionState();
		final int[] creations = { 0 };
		CrdbServiceConnectionState.AccessorFactory factory = () -> {
			creations[0]++;
			return null;
		};

		assertNull(state.get());
		assertFalse(state.isClosed());
		assertNull(state.ensure(factory));
		assertTrue(creations[0] >= 1);
		int afterOpen = creations[0];

		assertNull(state.close());
		assertTrue(state.isClosed());
		assertNull(state.get());
		assertNull(state.ensure(factory));
		assertEquals(afterOpen, creations[0]);
		assertNull(state.close());
	}

	@Test(expected = IllegalArgumentException.class)
	public void ensureRejectsNullFactory() {
		new CrdbServiceConnectionState().ensure(null);
	}

	private static void assertEquals(int expected, int actual) {
		org.junit.Assert.assertEquals(expected, actual);
	}
}
