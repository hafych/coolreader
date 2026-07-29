/*
 * CoolReader for Android
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 2
 * of the License, or (at your option) any later version.
 */

package org.coolreader.crengine;

import android.app.Activity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.coolreader.plugins.litres.LitresPlugin;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class ActivityOwnershipPolicyTest {
	@Test
	public void servicesRetainNoMutableStaticGraphFields() {
		for (Field field : Services.class.getDeclaredFields()) {
			if (!Modifier.isStatic(field.getModifiers()))
				continue;
			assertTrue(
					"Only immutable infrastructure may be static: "
							+ field.getName(),
					Modifier.isFinal(field.getModifiers()));
		}
	}

	@Test
	public void customToastRetainsNoStaticUiState() {
		for (Field field : ToastView.class.getDeclaredFields()) {
			assertFalse(
					"Toast UI state must belong to its Activity: "
							+ field.getName(),
					Modifier.isStatic(field.getModifiers()));
		}
	}

	@Test
	public void scannerAndCachedStorePluginRetainNoActivity() {
		assertRetainsNoActivity(Scanner.class);
		assertRetainsNoActivity(LitresPlugin.class);
	}

	@Test
	public void processDispatcherPublishesHandlerStateSafely()
			throws Exception {
		assertVolatileField(BackgroundThread.class, "instance");
		assertVolatileField(BackgroundThread.class, "handler");
		assertVolatileField(BackgroundThread.class, "guiHandler");
		int deferredQueueCount = 0;
		for (Field field : BackgroundThread.class.getDeclaredFields()) {
			if (field.getType() == DeferredTaskQueue.class) {
				deferredQueueCount++;
				assertTrue(
						"Deferred queues must belong to one dispatcher: "
								+ field.getName(),
						Modifier.isFinal(field.getModifiers()));
			}
			assertFalse(
					"Legacy mutable dispatcher state remains: "
							+ field.getName(),
					field.getName().equals("delayedTaskId")
							|| field.getName().equals("mStopped"));
		}
		assertEquals(
				"GUI and background handoffs need separate queues",
				2,
				deferredQueueCount);
	}

	@Test
	public void engineProcessSnapshotIsImmutableAndPathStateIsScoped()
			throws Exception {
		assertFinalStaticField(Engine.class, "PROGRESS_STYLE");
		assertFinalStaticField(Engine.class, "DOM_VERSION_CURRENT");
		assertFinalStaticField(Engine.class, "MOUNTED_ROOTS");
		assertFinalStaticField(Engine.class, "MOUNTED_ROOTS_MAP");

		Field pathCorrector =
				Engine.class.getDeclaredField("mPathCorrector");
		assertTrue(Modifier.isFinal(pathCorrector.getModifiers()));
		assertFalse(Modifier.isStatic(pathCorrector.getModifiers()));
		for (String legacy : new String[]{
				"mountedRootsList",
				"mountedRootsMap",
				"pathCorrector",
				"mFonts"}) {
			for (Field field : Engine.class.getDeclaredFields()) {
				assertFalse(
						"Engine retains mutable process field " + legacy,
						field.getName().equals(legacy));
			}
		}
	}

	@Test
	public void hyphenationRegistryHasOneFinalOwnerAndImmutableItems()
			throws Exception {
		Field registry = Engine.HyphDict.class.getDeclaredField("REGISTRY");
		assertTrue(Modifier.isStatic(registry.getModifiers()));
		assertTrue(Modifier.isFinal(registry.getModifiers()));
		Field language =
				Engine.HyphDict.class.getDeclaredField("language");
		assertTrue(Modifier.isFinal(language.getModifiers()));
		for (Field field : Engine.HyphDict.class.getDeclaredFields()) {
			assertFalse(
					"Hyphenation registry exposes mutable static array "
							+ field.getName(),
					Modifier.isStatic(field.getModifiers())
							&& field.getType().isArray());
		}
	}

	@Test
	public void pageCurveTablesHaveOneFinalOwnerAndPrivateStorage()
			throws Exception {
		assertFinalStaticField(ReaderView.class, "PAGE_CURVE_TABLES");
		for (Field field : PageCurveTables.class.getDeclaredFields()) {
			assertFalse(
					"Page-curve storage must be instance-owned: "
							+ field.getName(),
					Modifier.isStatic(field.getModifiers()));
			assertTrue(
					"Page-curve storage must not be replaceable: "
							+ field.getName(),
					Modifier.isFinal(field.getModifiers()));
			assertTrue(
					"Page-curve storage must not escape its owner: "
							+ field.getName(),
					Modifier.isPrivate(field.getModifiers()));
		}
	}

	private static void assertVolatileField(Class<?> type, String name)
			throws Exception {
		Field field = type.getDeclaredField(name);
		assertTrue(
				type.getSimpleName() + "." + name
						+ " must be published across threads",
				Modifier.isVolatile(field.getModifiers()));
	}

	private static void assertFinalStaticField(Class<?> type, String name)
			throws Exception {
		Field field = type.getDeclaredField(name);
		assertTrue(Modifier.isStatic(field.getModifiers()));
		assertTrue(Modifier.isFinal(field.getModifiers()));
	}

	private static void assertRetainsNoActivity(Class<?> type) {
		for (Field field : type.getDeclaredFields()) {
			assertFalse(
					type.getSimpleName() + " retains Activity field "
							+ field.getName(),
					Activity.class.isAssignableFrom(field.getType()));
		}
	}
}
