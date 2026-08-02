package org.coolreader.crengine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ActivityProfileStateTest {
	@Test
	public void getOrLoadClampsAndCaches() {
		ActivityProfileState state = new ActivityProfileState();
		final int[] loads = { 0 };

		int first = state.getOrLoad(6, () -> {
			loads[0]++;
			return 99;
		});
		assertEquals(1, first);
		assertEquals(1, loads[0]);

		int second = state.getOrLoad(6, () -> {
			loads[0]++;
			return 3;
		});
		assertEquals(1, second);
		assertEquals(1, loads[0]);
	}

	@Test
	public void setPublishesInRangeOnly() {
		ActivityProfileState state = new ActivityProfileState();
		state.getOrLoad(6, () -> 2);

		assertFalse(state.set(0, 6));
		assertEquals(2, state.get());
		assertFalse(state.set(7, 6));
		assertEquals(2, state.get());
		assertTrue(state.set(5, 6));
		assertEquals(5, state.get());
	}

	@Test
	public void closeIsPermanent() {
		ActivityProfileState state = new ActivityProfileState();
		state.getOrLoad(6, () -> 3);

		assertTrue(state.close());
		assertTrue(state.isClosed());
		assertEquals(ActivityProfileState.UNSET, state.get());
		assertEquals(1, state.getOrLoad(6, () -> 4));
		assertFalse(state.set(2, 6));
		assertFalse(state.close());
	}

	@Test(expected = IllegalArgumentException.class)
	public void getOrLoadRejectsNullLoaderWhenUnset() {
		new ActivityProfileState().getOrLoad(6, null);
	}
}
