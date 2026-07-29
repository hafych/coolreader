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
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.text.DateFormat;

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
		for (Class<?> nested : ReaderView.class.getDeclaredClasses()) {
			assertFalse(
					"Process dispatcher handoff must not belong to ReaderView",
					nested.getSimpleName().equals("Sync"));
		}
		for (Field field : BlockingResult.class.getDeclaredFields()) {
			assertFalse(
					"Blocking handoff state must be instance-owned: "
							+ field.getName(),
					Modifier.isStatic(field.getModifiers()));
			assertTrue(
					"Blocking handoff state must remain encapsulated: "
							+ field.getName(),
					Modifier.isPrivate(field.getModifiers()));
		}
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

	@Test
	public void readerBitmapMemoryStateBelongsToOneGeneration()
			throws Exception {
		for (String name : new String[]{"runtime", "factory"}) {
			Field field = ReaderView.class.getDeclaredField(name);
			assertFalse(Modifier.isStatic(field.getModifiers()));
			assertTrue(Modifier.isFinal(field.getModifiers()));
		}
		Field surfaceMemory =
				ReaderView.class.getDeclaredField("hackMemorySize");
		assertFalse(Modifier.isStatic(surfaceMemory.getModifiers()));
		assertEquals(long.class, surfaceMemory.getType());

		for (Field field : VMRuntimeHack.class.getDeclaredFields()) {
			assertFalse(
					"VMRuntime state must be reader-owned: " + field.getName(),
					Modifier.isStatic(field.getModifiers()));
			assertTrue(
					"VMRuntime state must remain encapsulated: "
							+ field.getName(),
					Modifier.isPrivate(field.getModifiers()));
			if (!field.getName().equals("totalSize"))
				assertTrue(
						"VMRuntime bindings must be immutable: "
								+ field.getName(),
						Modifier.isFinal(field.getModifiers()));
		}
		Field totalSize = VMRuntimeHack.class.getDeclaredField("totalSize");
		assertEquals(long.class, totalSize.getType());
		for (String methodName : new String[]{"trackAlloc", "trackFree"}) {
			Method method =
					VMRuntimeHack.class.getDeclaredMethod(
							methodName, long.class);
			assertTrue(Modifier.isSynchronized(method.getModifiers()));
		}
	}

	@Test
	public void readerActionUiConfigurationIsImmutableAndActivityOwned()
			throws Exception {
		Field actionIcons =
				BaseActivity.class.getDeclaredField("actionIcons");
		assertFalse(Modifier.isStatic(actionIcons.getModifiers()));
		assertTrue(Modifier.isPrivate(actionIcons.getModifiers()));

		for (Field field : ReaderAction.class.getDeclaredFields()) {
			if (!Modifier.isStatic(field.getModifiers())) {
				assertTrue(
						"ReaderAction definitions must be immutable: "
								+ field.getName(),
						Modifier.isFinal(field.getModifiers()));
			}
			assertFalse(
					"ReaderAction must not expose mutable static arrays: "
							+ field.getName(),
					Modifier.isStatic(field.getModifiers())
							&& field.getType().isArray());
		}
		for (Field field : ActionIconSet.class.getDeclaredFields()) {
			assertFalse(Modifier.isStatic(field.getModifiers()));
			assertTrue(Modifier.isPrivate(field.getModifiers()));
			assertTrue(Modifier.isFinal(field.getModifiers()));
		}
		for (Field field : DefaultInputActions.class.getDeclaredFields()) {
			assertFalse(Modifier.isStatic(field.getModifiers()));
			assertTrue(Modifier.isPrivate(field.getModifiers()));
			assertTrue(Modifier.isFinal(field.getModifiers()));
		}

		Class<?> settingsManager = null;
		for (Class<?> nested : BaseActivity.class.getDeclaredClasses()) {
			if (nested.getSimpleName().equals("SettingsManager"))
				settingsManager = nested;
		}
		assertTrue(settingsManager != null);
		Field defaults =
				settingsManager.getDeclaredField("defaultInputActions");
		assertFalse(Modifier.isStatic(defaults.getModifiers()));
		assertTrue(Modifier.isFinal(defaults.getModifiers()));
	}

	@Test
	public void optionsDialogKeepsUiConfigurationGenerationScoped()
			throws Exception {
		for (String name : new String[]{
				"mBacklightLevels",
				"mBacklightLevelsTitles",
				"mMotionTimeouts",
				"mMotionTimeoutsTitles",
				"mPagesPerFullSwipe",
				"mPagesPerFullSwipeTitles"}) {
			Field field = OptionsDialog.class.getDeclaredField(name);
			assertFalse(Modifier.isStatic(field.getModifiers()));
			assertTrue(Modifier.isFinal(field.getModifiers()));
		}
		for (String legacy : new String[]{
				"showIcons",
				"isHtmlFormat"}) {
			for (Field field : OptionsDialog.class.getDeclaredFields()) {
				assertFalse(
						"OptionsDialog retains legacy process UI state "
								+ legacy,
						field.getName().equals(legacy));
			}
		}
		for (String name : new String[]{
				"isTextFormat",
				"isEpubFormat",
				"isFormatWithEmbeddedStyle"}) {
			Field field = OptionsDialog.class.getDeclaredField(name);
			assertFalse(Modifier.isStatic(field.getModifiers()));
		}
	}

	@Test
	public void backlightTimeoutStateBelongsToOneActivityGeneration()
			throws Exception {
		Field control =
				BaseActivity.class.getDeclaredField("backlightControl");
		assertFalse(Modifier.isStatic(control.getModifiers()));
		assertTrue(Modifier.isFinal(control.getModifiers()));
		for (Field field : BaseActivity.class.getDeclaredFields()) {
			assertFalse(
					"BaseActivity retains process-wide backlight state "
							+ field.getName(),
					field.getName().equals("lastUserActivityTime")
							|| field.getName().equals("backlightTimerTask"));
		}
		Class<?> controlClass = null;
		for (Class<?> nested : BaseActivity.class.getDeclaredClasses()) {
			if (nested.getSimpleName().equals("ScreenBacklightControl"))
				controlClass = nested;
		}
		assertTrue(controlClass != null);
		for (String name : new String[]{
				"lastUserActivityTime",
				"backlightTimerTask"}) {
			Field field = controlClass.getDeclaredField(name);
			assertFalse(Modifier.isStatic(field.getModifiers()));
			assertTrue(Modifier.isPrivate(field.getModifiers()));
		}
	}

	@Test
	public void nookEpdReflectionGraphBelongsToControllerGeneration()
			throws Exception {
		Field bindings =
				N2EpdController.class.getDeclaredField("bindings");
		assertFalse(Modifier.isStatic(bindings.getModifiers()));
		assertTrue(Modifier.isFinal(bindings.getModifiers()));
		Field controller =
				N2EpdController.class.getDeclaredField("mEpdController");
		assertFalse(Modifier.isStatic(controller.getModifiers()));
		for (Field field : N2EpdController.class.getDeclaredFields()) {
			if (!Modifier.isStatic(field.getModifiers()))
				continue;
			assertTrue(
					"Nook EPD static fields must be immutable constants: "
							+ field.getName(),
					Modifier.isFinal(field.getModifiers()));
			assertTrue(
					"Nook EPD reflection state must not be process-wide: "
							+ field.getName(),
					field.getType().isPrimitive());
		}
		for (Field field :
				NookEpdControllerBindings.class.getDeclaredFields()) {
			assertFalse(
					"Nook EPD binding state must be instance-owned: "
							+ field.getName(),
					Modifier.isStatic(field.getModifiers()));
			assertTrue(
					"Nook EPD bindings must be immutable: "
							+ field.getName(),
					Modifier.isFinal(field.getModifiers()));
			assertTrue(
					"Nook EPD bindings must remain encapsulated: "
							+ field.getName(),
					Modifier.isPrivate(field.getModifiers()));
		}
	}

	@Test
	public void opdsTimestampParsingRetainsNoSharedFormatter() {
		for (Field field : OPDSUtil.OPDSHandler.class.getDeclaredFields()) {
			assertFalse(
					"OPDS parser retains process-wide mutable formatter "
							+ field.getName(),
					Modifier.isStatic(field.getModifiers())
							&& DateFormat.class.isAssignableFrom(
									field.getType()));
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
