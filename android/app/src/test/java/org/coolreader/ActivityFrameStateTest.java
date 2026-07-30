/*
 * CoolReader for Android
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 2
 * of the License, or (at your option) any later version.
 */

package org.coolreader;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class ActivityFrameStateTest {
	@Test
	public void startsWithoutHistoryAndRejectsNull() {
		ActivityFrameState<Object> state =
				new ActivityFrameState<>();

		assertTrue(state.isCurrent(null));
		assertNull(state.previous());
		assertFalse(state.isPrevious(null));
		assertFalse(state.moveTo(null));
	}

	@Test
	public void exactIdentityTransitionPublishesPreviousFrame() {
		ActivityFrameState<Object> state =
				new ActivityFrameState<>();
		Object first = new String("same");
		Object equalButDistinct = new String("same");

		assertTrue(state.moveTo(first));
		assertTrue(state.isCurrent(first));
		assertNull(state.previous());
		assertFalse(state.moveTo(first));
		assertTrue(state.moveTo(equalButDistinct));
		assertTrue(state.isCurrent(equalButDistinct));
		assertSame(first, state.previous());
		assertTrue(state.isPrevious(first));
		assertFalse(state.isPrevious(equalButDistinct));
	}

	@Test
	public void closeClearsFramesAndRejectsStaleTransitions() {
		ActivityFrameState<Object> state =
				new ActivityFrameState<>();
		Object first = new Object();
		Object second = new Object();
		assertTrue(state.moveTo(first));
		assertTrue(state.moveTo(second));

		assertTrue(state.close());
		assertFalse(state.close());
		assertNull(state.previous());
		assertFalse(state.isCurrent(first));
		assertFalse(state.isCurrent(second));
		assertFalse(state.isPrevious(first));
		assertFalse(state.moveTo(first));
	}
}
