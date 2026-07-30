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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class TapHandlerStateTest {
	@Test
	public void firstExactHandlerWinsInstallation() {
		TapHandlerState<Object> state =
				new TapHandlerState<>();
		Object first = new Object();
		Object ignored = new Object();

		assertNull(state.current());
		assertSame(first, state.installIfAbsent(first));
		assertSame(first, state.installIfAbsent(ignored));
		assertSame(first, state.current());
		assertTrue(state.isCurrent(first));
		assertFalse(state.isCurrent(ignored));
		assertNull(state.installIfAbsent(null));
	}

	@Test
	public void onlyExactCurrentHandlerCanReplaceItself() {
		TapHandlerState<Object> state =
				new TapHandlerState<>();
		Object first = new Object();
		Object stale = new Object();
		Object replacement = new Object();
		assertSame(first, state.installIfAbsent(first));

		assertFalse(state.replace(stale, replacement));
		assertFalse(state.replace(first, first));
		assertFalse(state.replace(first, null));
		assertTrue(state.replace(first, replacement));
		assertSame(replacement, state.current());
		assertFalse(state.replace(first, stale));
	}

	@Test
	public void closeClearsAndPermanentlyRejectsHandlers() {
		TapHandlerState<Object> state =
				new TapHandlerState<>();
		Object handler = new Object();
		assertSame(handler, state.installIfAbsent(handler));

		assertTrue(state.close());
		assertFalse(state.close());
		assertNull(state.current());
		assertFalse(state.isCurrent(handler));
		assertNull(state.installIfAbsent(new Object()));
		assertFalse(state.replace(handler, new Object()));
	}
}
