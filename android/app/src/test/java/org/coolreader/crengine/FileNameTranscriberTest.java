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

public class FileNameTranscriberTest {
	@Test
	public void legacyCyrillicAndAsciiMappingIsPreserved() {
		FileNameTranscriber transcriber = FileNameTranscriber.legacy();

		assertEquals(
				"Privet_mir_txt",
				transcriber.transcribe("Привет мир.txt"));
		assertEquals("Schuka", transcriber.transcribe("Щука"));
		assertEquals(
				"A-z_09(test)",
				transcriber.transcribe("A-z_09(test)"));
		assertEquals("___", transcriber.transcribe(" /ё"));
	}

	@Test
	public void limitIsAppliedAfterTransliterationExpansion() {
		FileNameTranscriber transcriber = FileNameTranscriber.legacy();

		assertEquals(
				"Sch",
				transcriber.transcribeWithLimit("Щука", 3));
		assertEquals(
				"",
				transcriber.transcribeWithLimit("Щука", 0));
	}

	@Test
	public void storageIsPrivateFinalAndInstanceOwned() {
		for (Field field : FileNameTranscriber.class.getDeclaredFields()) {
			assertTrue(Modifier.isPrivate(field.getModifiers()));
			assertTrue(Modifier.isFinal(field.getModifiers()));
			assertTrue(!Modifier.isStatic(field.getModifiers()));
		}
		for (Class<?> nested :
				FileNameTranscriber.class.getDeclaredClasses()) {
			for (Field field : nested.getDeclaredFields()) {
				assertTrue(Modifier.isPrivate(field.getModifiers()));
				assertTrue(Modifier.isFinal(field.getModifiers()));
				assertTrue(!Modifier.isStatic(field.getModifiers()));
			}
		}
	}

	@Test
	public void invalidInputIsRejected() {
		try {
			FileNameTranscriber.legacy().transcribe(null);
			fail("missing filename was accepted");
		} catch (IllegalArgumentException expected) {
			// Expected.
		}
		try {
			FileNameTranscriber.legacy().transcribeWithLimit("book", -1);
			fail("negative filename limit was accepted");
		} catch (IllegalArgumentException expected) {
			// Expected.
		}
	}
}
