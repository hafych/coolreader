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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class ReaderSurfaceMemoryStateTest {
	@Test
	public void firstSizeAcquiresSurfaceBytes() {
		ReaderSurfaceMemoryState state =
				new ReaderSurfaceMemoryState();

		ReaderSurfaceMemoryState.Change change =
				state.resize(100, 50);

		assertEquals(0, change.releasedBytes());
		assertEquals(10000, change.acquiredBytes());
		assertNull(state.resize(100, 50));
	}

	@Test
	public void resizeReleasesPreviousAndAcquiresReplacement() {
		ReaderSurfaceMemoryState state =
				new ReaderSurfaceMemoryState();
		state.resize(100, 50);

		ReaderSurfaceMemoryState.Change change =
				state.resize(200, 50);

		assertEquals(10000, change.releasedBytes());
		assertEquals(20000, change.acquiredBytes());
	}

	@Test
	public void clearIsExactAndIdempotent() {
		ReaderSurfaceMemoryState state =
				new ReaderSurfaceMemoryState();
		state.resize(200, 50);

		ReaderSurfaceMemoryState.Change change =
				state.clear();

		assertEquals(20000, change.releasedBytes());
		assertEquals(0, change.acquiredBytes());
		assertNull(state.clear());
	}

	@Test
	public void byteCountsUseWidenedSurfaceArithmetic() {
		ReaderSurfaceMemoryState state =
				new ReaderSurfaceMemoryState();

		ReaderSurfaceMemoryState.Change change =
				state.resize(100000, 100000);

		assertEquals(
				20000000000L,
				change.acquiredBytes());
	}

	@Test
	public void invalidResizeDoesNotReplaceCurrentState() {
		ReaderSurfaceMemoryState state =
				new ReaderSurfaceMemoryState();
		state.resize(100, 50);
		try {
			state.resize(-1, 50);
		} catch (IllegalArgumentException expected) {
			ReaderSurfaceMemoryState.Change change =
					state.clear();
			assertEquals(10000, change.releasedBytes());
			return;
		}
		throw new AssertionError(
				"negative surface dimensions accepted");
	}
}
