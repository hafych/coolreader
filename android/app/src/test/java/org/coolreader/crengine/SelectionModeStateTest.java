/*
 * CoolReader for Android
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 2
 * of the License, or (at your option) any later version.
 */

package org.coolreader.crengine;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SelectionModeStateTest {
	@Test
	public void toggleArmsAndDisarmsMode() {
		SelectionModeState state =
				new SelectionModeState();

		assertFalse(state.isActive());
		assertTrue(state.toggle());
		assertTrue(state.isActive());
		assertFalse(state.toggle());
		assertFalse(state.isActive());
	}

	@Test
	public void successfulSelectionConsumesArmedModeOnce() {
		SelectionModeState state =
				new SelectionModeState();
		assertTrue(state.toggle());

		assertTrue(state.consume());
		assertFalse(state.isActive());
		assertFalse(state.consume());
	}

	@Test
	public void observationDoesNotConsumeMode() {
		SelectionModeState state =
				new SelectionModeState();
		assertTrue(state.toggle());

		assertTrue(state.isActive());
		assertTrue(state.isActive());
		assertTrue(state.consume());
	}

	@Test
	public void closeIsTerminalAndDisarmed() {
		SelectionModeState state =
				new SelectionModeState();
		assertTrue(state.toggle());

		assertTrue(state.close());
		assertFalse(state.close());
		assertFalse(state.isActive());
		assertFalse(state.consume());
		assertFalse(state.toggle());
	}
}
