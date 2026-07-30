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

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class ReaderPageBitmapLifetimeTest {
	@Test
	public void retireWithoutReaderReleasesImmediately() {
		List<Object> released = new ArrayList<>();
		ReaderPageBitmapLifetime<Object> lifetime =
				new ReaderPageBitmapLifetime<>(
						released::add);
		Object page = new Object();

		assertTrue(lifetime.retire(page));
		assertEquals(1, released.size());
		assertSame(page, released.get(0));
	}

	@Test
	public void activeReadDefersAndDeduplicatesRetirement() {
		List<Object> released = new ArrayList<>();
		ReaderPageBitmapLifetime<Object> lifetime =
				new ReaderPageBitmapLifetime<>(
						released::add);
		ReaderPageBitmapLifetime.Read read =
				lifetime.beginRead();
		Object page = new Object();

		assertNotNull(read);
		assertFalse(lifetime.retire(page));
		assertFalse(lifetime.retire(page));
		assertTrue(released.isEmpty());
		assertTrue(lifetime.finishRead(read));
		assertEquals(1, released.size());
		assertSame(page, released.get(0));
		assertFalse(lifetime.finishRead(read));
	}

	@Test
	public void lastExactReaderReleasesAllDeferredIdentities() {
		List<Object> released = new ArrayList<>();
		ReaderPageBitmapLifetime<Object> lifetime =
				new ReaderPageBitmapLifetime<>(
						released::add);
		ReaderPageBitmapLifetime.Read first =
				lifetime.beginRead();
		ReaderPageBitmapLifetime.Read second =
				lifetime.beginRead();
		Object current = new Object();
		Object next = new Object();

		lifetime.retire(current);
		lifetime.retire(next);
		assertTrue(lifetime.finishRead(first));
		assertTrue(released.isEmpty());
		assertTrue(lifetime.finishRead(second));
		assertEquals(2, released.size());
		assertTrue(released.contains(current));
		assertTrue(released.contains(next));
	}

	@Test
	public void staleAndForeignReadTokensCannotReleaseResources() {
		List<Object> released = new ArrayList<>();
		ReaderPageBitmapLifetime<Object> first =
				new ReaderPageBitmapLifetime<>(
						released::add);
		ReaderPageBitmapLifetime<Object> second =
				new ReaderPageBitmapLifetime<>(
						released::add);
		ReaderPageBitmapLifetime.Read firstRead =
				first.beginRead();
		ReaderPageBitmapLifetime.Read foreign =
				second.beginRead();
		Object page = new Object();

		first.retire(page);
		assertFalse(first.finishRead(foreign));
		assertTrue(released.isEmpty());
		assertTrue(first.finishRead(firstRead));
		assertEquals(1, released.size());
		assertTrue(second.finishRead(foreign));
	}

	@Test
	public void closeRejectsNewReadersButDrainsExistingReader() {
		List<Object> released = new ArrayList<>();
		ReaderPageBitmapLifetime<Object> lifetime =
				new ReaderPageBitmapLifetime<>(
						released::add);
		ReaderPageBitmapLifetime.Read read =
				lifetime.beginRead();
		Object page = new Object();

		lifetime.retire(page);
		assertTrue(lifetime.close());
		assertFalse(lifetime.close());
		assertNull(lifetime.beginRead());
		assertTrue(released.isEmpty());
		assertTrue(lifetime.finishRead(read));
		assertEquals(1, released.size());

		Object late = new Object();
		assertTrue(lifetime.retire(late));
		assertSame(late, released.get(1));
	}
}
