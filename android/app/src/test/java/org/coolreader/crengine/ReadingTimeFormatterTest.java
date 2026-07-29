/*
 * CoolReader for Android
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 2
 * of the License, or (at your option) any later version.
 */

package org.coolreader.crengine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Locale;

public class ReadingTimeFormatterTest {
	@Test
	public void normalDurationsPreserveHoursAndPaddedMinutes() {
		assertEquals(
				"0:00",
				ReadingTimeFormatter.format(0, Locale.US));
		assertEquals(
				"0:59",
				ReadingTimeFormatter.format(
						59L * 60L * 1_000L,
						Locale.US));
		assertEquals(
				"1:00",
				ReadingTimeFormatter.format(
						60L * 60L * 1_000L,
						Locale.US));
		assertEquals(
				"25:05",
				ReadingTimeFormatter.format(
						(25L * 60L + 5L) * 60L * 1_000L,
						Locale.US));
	}

	@Test
	public void durationsBeyondIntMillisecondsRemainAccurate() {
		assertEquals(
				"720:00",
				ReadingTimeFormatter.format(
						30L * 24L * 60L * 60L * 1_000L,
						Locale.US));
	}

	@Test
	public void longMaximumDoesNotNarrowOrOverflow() {
		assertEquals(
				"2562047788015:12",
				ReadingTimeFormatter.format(Long.MAX_VALUE, Locale.US));
	}

	@Test
	public void negativePersistedDurationIsClampedToZero() {
		assertEquals(
				"0:00",
				ReadingTimeFormatter.format(-60_000L, Locale.US));
	}

	@Test
	public void missingLocaleIsRejected() {
		try {
			ReadingTimeFormatter.format(0, null);
			fail("missing reading-time locale was accepted");
		} catch (IllegalArgumentException expected) {
			// Expected.
		}
	}

	@Test
	public void formatterRetainsOnlyPrimitiveConstants() {
		for (Field field : ReadingTimeFormatter.class.getDeclaredFields()) {
			assertTrue(Modifier.isStatic(field.getModifiers()));
			assertTrue(Modifier.isFinal(field.getModifiers()));
			assertTrue(field.getType().isPrimitive());
		}
	}
}
