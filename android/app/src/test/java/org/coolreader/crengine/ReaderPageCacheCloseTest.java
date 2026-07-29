/*
 * CoolReader for Android
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 2
 * of the License, or (at your option) any later version.
 */

package org.coolreader.crengine;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ReaderPageCacheCloseTest {
	@Test
	public void initialAndSerializedSlotsPreserveIdentity() {
		Object initialCurrent = new Object();
		Object initialNext = new Object();
		Object serializedCurrent = new Object();
		Object serializedNext = new Object();
		ReaderPageCacheClose<Object> close =
				ReaderPageCacheClose.begin(
						initialCurrent, initialNext);

		assertTrue(close.publishSerialized(
				serializedCurrent, serializedNext));
		ReaderPageCacheClose.Resources<Object> resources =
				close.finish();

		assertSame(
				initialCurrent, resources.initialCurrent());
		assertSame(initialNext, resources.initialNext());
		assertSame(
				serializedCurrent,
				resources.serializedCurrent());
		assertSame(
				serializedNext, resources.serializedNext());
	}

	@Test
	public void serializedPublicationAndFinishAreOneShot() {
		Object first = new Object();
		Object replacement = new Object();
		ReaderPageCacheClose<Object> close =
				ReaderPageCacheClose.begin(null, null);

		assertTrue(close.publishSerialized(first, null));
		assertFalse(close.publishSerialized(
				replacement, replacement));
		ReaderPageCacheClose.Resources<Object> resources =
				close.finish();

		assertSame(first, resources.serializedCurrent());
		assertNull(resources.serializedNext());
		assertNull(close.finish());
	}

	@Test
	public void finishBeforeWorkRejectsLatePublication() {
		Object initial = new Object();
		ReaderPageCacheClose<Object> close =
				ReaderPageCacheClose.begin(initial, null);

		ReaderPageCacheClose.Resources<Object> resources =
				close.finish();

		assertSame(initial, resources.initialCurrent());
		assertNull(resources.serializedCurrent());
		assertFalse(close.publishSerialized(
				new Object(), new Object()));
	}

	@Test
	public void nullAndAliasedSlotsRemainRepresentable() {
		Object shared = new Object();
		ReaderPageCacheClose<Object> close =
				ReaderPageCacheClose.begin(shared, shared);

		assertTrue(close.publishSerialized(null, shared));
		ReaderPageCacheClose.Resources<Object> resources =
				close.finish();

		assertSame(shared, resources.initialCurrent());
		assertSame(shared, resources.initialNext());
		assertNull(resources.serializedCurrent());
		assertSame(shared, resources.serializedNext());
	}
}
