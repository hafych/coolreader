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
import android.os.HandlerThread;
import android.view.View;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.coolreader.CoolReader;
import org.coolreader.Dictionaries;
import org.coolreader.plugins.litres.LitresPlugin;
import org.junit.Test;

import com.s_trace.motion_watchdog.MotionWatchdogHandler;

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
	public void delayedExecutorOwnsOneShotReplacementSlot()
			throws Exception {
		Field tasks = DelayedExecutor.class.getDeclaredField("tasks");
		assertFalse(Modifier.isStatic(tasks.getModifiers()));
		assertTrue(Modifier.isPrivate(tasks.getModifiers()));
		assertTrue(Modifier.isFinal(tasks.getModifiers()));
		assertEquals(ReplaceableTaskSlot.class, tasks.getType());
		for (Field field : DelayedExecutor.class.getDeclaredFields()) {
			assertFalse(
					"DelayedExecutor retains legacy callback slot",
					field.getName().equals("currentTask"));
		}
		for (Field field :
				ReplaceableTaskSlot.class.getDeclaredFields()) {
			assertFalse(Modifier.isStatic(field.getModifiers()));
			assertTrue(Modifier.isPrivate(field.getModifiers()));
		}
		for (Class<?> nested :
				ReplaceableTaskSlot.class.getDeclaredClasses()) {
			for (Field field : nested.getDeclaredFields()) {
				if (field.isSynthetic())
					continue;
				assertTrue(Modifier.isPrivate(field.getModifiers()));
				assertTrue(Modifier.isFinal(field.getModifiers()));
			}
		}
		for (Method method :
				ReplaceableTaskSlot.class.getDeclaredMethods()) {
			if (method.getName().equals("replace")
					|| method.getName().equals("cancel")
					|| method.getName().equals("claim")) {
				assertTrue(
						method.getName() + " must serialize slot state",
						Modifier.isSynchronized(
								method.getModifiers()));
			}
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
	public void engineTextureCatalogHasImmutableMetadata()
			throws Exception {
		Field catalog =
				Engine.class.getDeclaredField("BUILT_IN_TEXTURES");
		assertTrue(Modifier.isStatic(catalog.getModifiers()));
		assertTrue(Modifier.isPrivate(catalog.getModifiers()));
		assertTrue(Modifier.isFinal(catalog.getModifiers()));
		assertFinalStaticField(Engine.class, "NO_TEXTURE");

		for (Field field :
				BackgroundTextureCatalog.class.getDeclaredFields()) {
			assertFalse(Modifier.isStatic(field.getModifiers()));
			assertTrue(Modifier.isPrivate(field.getModifiers()));
			assertTrue(Modifier.isFinal(field.getModifiers()));
		}
		for (Field field :
				BackgroundTextureInfo.class.getDeclaredFields()) {
			if (Modifier.isStatic(field.getModifiers())) {
				assertTrue(Modifier.isFinal(field.getModifiers()));
				assertFalse(field.getType().isArray());
			} else {
				assertTrue(Modifier.isPrivate(field.getModifiers()));
				assertTrue(Modifier.isFinal(field.getModifiers()));
			}
		}
		for (Field field : Engine.class.getDeclaredFields()) {
			assertFalse(
					"Engine retains the mutable texture array",
					field.getName().equals("internalTextures"));
		}
	}

	@Test
	public void documentFormatMetadataIsPrivateAndImmutable() {
		for (Field field : DocumentFormat.class.getDeclaredFields()) {
			if (Modifier.isStatic(field.getModifiers()))
				continue;
			assertTrue(Modifier.isPrivate(field.getModifiers()));
			assertTrue(Modifier.isFinal(field.getModifiers()));
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
	public void tapZoneGeometryUsesImmutableValueBounds() {
		assertTrue(Modifier.isFinal(
				TapZoneGeometry.class.getModifiers()));
		for (Field field : TapZoneGeometry.class.getDeclaredFields()) {
			assertTrue(Modifier.isStatic(field.getModifiers()));
			assertTrue(Modifier.isPrivate(field.getModifiers()));
			assertTrue(Modifier.isFinal(field.getModifiers()));
			assertTrue(field.getType().isPrimitive());
		}
		for (Field field :
				TapZoneGeometry.Bounds.class.getDeclaredFields()) {
			if (Modifier.isStatic(field.getModifiers())) {
				assertTrue(Modifier.isPrivate(field.getModifiers()));
				assertTrue(Modifier.isFinal(field.getModifiers()));
				assertEquals(
						TapZoneGeometry.Bounds.class,
						field.getType());
			} else {
				assertTrue(Modifier.isPrivate(field.getModifiers()));
				assertTrue(Modifier.isFinal(field.getModifiers()));
				assertTrue(field.getType().isPrimitive());
			}
		}
	}

	@Test
	public void einkRefreshLeasesBelongToOneReader()
			throws Exception {
		Field owner =
				ReaderView.class.getDeclaredField("einkRefreshLeases");
		assertFalse(Modifier.isStatic(owner.getModifiers()));
		assertTrue(Modifier.isPrivate(owner.getModifiers()));
		assertTrue(Modifier.isFinal(owner.getModifiers()));

		for (Field field :
				EinkRefreshLeaseTracker.class.getDeclaredFields()) {
			assertFalse(Modifier.isStatic(field.getModifiers()));
			assertTrue(Modifier.isPrivate(field.getModifiers()));
			if (java.util.Set.class.isAssignableFrom(field.getType()))
				assertTrue(Modifier.isFinal(field.getModifiers()));
		}
		for (String methodName : new String[]{
				"acquire",
				"release",
				"isActive"}) {
			Method method;
			if (methodName.equals("acquire")) {
				method = EinkRefreshLeaseTracker.class.getDeclaredMethod(
						methodName, int.class, int.class);
			} else if (methodName.equals("release")) {
				method = EinkRefreshLeaseTracker.class.getDeclaredMethod(
						methodName, int.class);
			} else {
				method = EinkRefreshLeaseTracker.class.getDeclaredMethod(
						methodName);
			}
			assertTrue(Modifier.isSynchronized(method.getModifiers()));
		}
		for (Field field : ReaderView.class.getDeclaredFields()) {
			assertFalse(
					"ReaderView retains inline E-Ink lease state "
							+ field.getName(),
					field.getName().equals("savedEinkUpdateInterval")
							|| field.getName().equals("einkModeClients"));
		}
	}

	@Test
	public void batteryStatusIsAnAtomicReaderSnapshot()
			throws Exception {
		Field readerStatus =
				ReaderView.class.getDeclaredField("batteryStatus");
		assertFalse(Modifier.isStatic(readerStatus.getModifiers()));
		assertTrue(Modifier.isPrivate(readerStatus.getModifiers()));
		assertTrue(Modifier.isVolatile(readerStatus.getModifiers()));
		assertEquals(BatteryStatus.class, readerStatus.getType());

		Field initialStatus =
				CoolReader.class.getDeclaredField(
						"initialBatteryStatus");
		assertFalse(Modifier.isStatic(initialStatus.getModifiers()));
		assertTrue(Modifier.isPrivate(initialStatus.getModifiers()));
		assertEquals(BatteryStatus.class, initialStatus.getType());

		for (Field field : BatteryStatus.class.getDeclaredFields()) {
			if (Modifier.isStatic(field.getModifiers())) {
				assertTrue(Modifier.isFinal(field.getModifiers()));
				assertTrue(field.getType().isPrimitive());
			} else {
				assertTrue(Modifier.isPrivate(field.getModifiers()));
				assertTrue(Modifier.isFinal(field.getModifiers()));
			}
		}
		for (String legacy : new String[]{
				"mBatteryState",
				"mBatteryChargingConn",
				"mBatteryChargeLevel"}) {
			for (Field field : ReaderView.class.getDeclaredFields()) {
				assertFalse(
						"ReaderView retains parallel battery field "
								+ legacy,
						field.getName().equals(legacy));
			}
		}
		for (String legacy : new String[]{
				"initialBatteryState",
				"initialBatteryChargeConn",
				"initialBatteryLevel"}) {
			for (Field field : CoolReader.class.getDeclaredFields()) {
				assertFalse(
						"CoolReader retains parallel battery field "
								+ legacy,
						field.getName().equals(legacy));
			}
		}
	}

	@Test
	public void mainProgressIsAnAtomicReaderSnapshot()
			throws Exception {
		Field owner =
				ReaderView.class.getDeclaredField("progressState");
		assertFalse(Modifier.isStatic(owner.getModifiers()));
		assertTrue(Modifier.isPrivate(owner.getModifiers()));
		assertTrue(Modifier.isFinal(owner.getModifiers()));
		assertEquals(ReaderProgressState.class, owner.getType());

		Field snapshot =
				ReaderProgressState.class.getDeclaredField("snapshot");
		assertFalse(Modifier.isStatic(snapshot.getModifiers()));
		assertTrue(Modifier.isPrivate(snapshot.getModifiers()));
		assertTrue(Modifier.isVolatile(snapshot.getModifiers()));
		for (Field field :
				ReaderProgressState.Snapshot.class.getDeclaredFields()) {
			if (Modifier.isStatic(field.getModifiers())) {
				assertTrue(Modifier.isPrivate(field.getModifiers()));
				assertTrue(Modifier.isFinal(field.getModifiers()));
			} else {
				assertTrue(Modifier.isPrivate(field.getModifiers()));
				assertTrue(Modifier.isFinal(field.getModifiers()));
			}
		}
		Method show = ReaderProgressState.class.getDeclaredMethod(
				"show", int.class, int.class, String.class);
		Method hide =
				ReaderProgressState.class.getDeclaredMethod("hide");
		assertTrue(Modifier.isSynchronized(show.getModifiers()));
		assertTrue(Modifier.isSynchronized(hide.getModifiers()));
		for (String legacy : new String[]{
				"currentProgressPosition",
				"currentProgressTitleId",
				"currentProgressTitle"}) {
			for (Field field : ReaderView.class.getDeclaredFields()) {
				assertFalse(
						"ReaderView retains parallel progress field "
								+ legacy,
						field.getName().equals(legacy));
			}
		}
	}

	@Test
	public void delayedWorkBelongsToOneReaderGeneration()
			throws Exception {
		for (String name : new String[]{
				"animationScheduler",
				"autoScrollScheduler",
				"autoScrollSessions",
				"gcTask",
				"animationUpdateLock"}) {
			Field field = ReaderView.class.getDeclaredField(name);
			assertFalse(Modifier.isStatic(field.getModifiers()));
			assertTrue(Modifier.isPrivate(field.getModifiers()));
			assertTrue(Modifier.isFinal(field.getModifiers()));
		}
		Field animation =
				ReaderView.class.getDeclaredField("currentAnimation");
		assertFalse(Modifier.isStatic(animation.getModifiers()));
		assertTrue(Modifier.isPrivate(animation.getModifiers()));
		assertTrue(Modifier.isVolatile(animation.getModifiers()));
		Field autoScrollSpeed =
				ReaderView.class.getDeclaredField(
						"autoScrollSpeed");
		assertTrue(Modifier.isVolatile(
				autoScrollSpeed.getModifiers()));
		for (Field field : ReaderView.class.getDeclaredFields()) {
			assertFalse(
					"ReaderView retains a racy autoscroll pointer",
					field.getName().equals(
							"currentAutoScrollAnimation"));
		}
		assertTrue(Modifier.isFinal(
				AutoScrollSessionState.class.getModifiers()));
		for (Field field :
				AutoScrollSessionState.class
						.getDeclaredFields()) {
			assertFalse(Modifier.isStatic(
					field.getModifiers()));
			assertTrue(Modifier.isPrivate(
					field.getModifiers()));
		}
		Method close =
				AutoScrollSessionState.class.getDeclaredMethod(
						"close");
		assertTrue(Modifier.isSynchronized(
				close.getModifiers()));
		Method teardown =
				ReaderView.class.getDeclaredMethod(
						"cancelDelayedReaderWork");
		assertTrue(Modifier.isPrivate(teardown.getModifiers()));
	}

	@Test
	public void ttsDialogOwnsItsCloseableWorkLifecycle()
			throws Exception {
		Field lifecycle =
				TTSToolbarDlg.class.getDeclaredField(
						"workLifecycle");
		assertFalse(Modifier.isStatic(lifecycle.getModifiers()));
		assertTrue(Modifier.isPrivate(lifecycle.getModifiers()));
		assertTrue(Modifier.isFinal(lifecycle.getModifiers()));

		Field pollingHandler =
				TTSToolbarDlg.class.getDeclaredField(
						"audioBookPosHandler");
		assertFalse(
				Modifier.isStatic(pollingHandler.getModifiers()));
		assertTrue(
				Modifier.isPrivate(pollingHandler.getModifiers()));
		assertTrue(
				Modifier.isFinal(pollingHandler.getModifiers()));

		Field motionWatchdog =
				TTSToolbarDlg.class.getDeclaredField(
						"mMotionWatchdog");
		assertFalse(Modifier.isStatic(
				motionWatchdog.getModifiers()));
		assertTrue(Modifier.isPrivate(
				motionWatchdog.getModifiers()));
		assertEquals(
				MotionWatchdogHandler.class,
				motionWatchdog.getType());
		for (String name : new String[]{
				"startMotionWatchdog",
				"stopMotionWatchdog"}) {
			Method method =
					TTSToolbarDlg.class.getDeclaredMethod(name);
			assertTrue(Modifier.isPrivate(method.getModifiers()));
			assertTrue(
					Modifier.isSynchronized(
							method.getModifiers()));
		}

		Field ownedThread =
				MotionWatchdogHandler.class.getDeclaredField(
						"mHandlerThread");
		assertEquals(HandlerThread.class, ownedThread.getType());
		assertTrue(Modifier.isPrivate(
				ownedThread.getModifiers()));
		assertTrue(Modifier.isFinal(
				ownedThread.getModifiers()));
		Method close =
				MotionWatchdogHandler.class.getDeclaredMethod(
						"close");
		assertTrue(Modifier.isPublic(close.getModifiers()));

		for (Field field :
				CloseableTaskGate.class.getDeclaredFields()) {
			assertFalse(Modifier.isStatic(field.getModifiers()));
			assertTrue(Modifier.isPrivate(field.getModifiers()));
		}
	}

	@Test
	public void repeatTouchListenerOwnsOneShotCallbacks()
			throws Exception {
		assertTrue(
				View.OnAttachStateChangeListener.class
						.isAssignableFrom(
								RepeatOnTouchListener.class));
		for (String name : new String[]{
				"handler",
				"repeatTasks",
				"initialInterval",
				"normalInterval",
				"clickListener"}) {
			Field field =
					RepeatOnTouchListener.class
							.getDeclaredField(name);
			assertFalse(Modifier.isStatic(field.getModifiers()));
			assertTrue(Modifier.isPrivate(field.getModifiers()));
			assertTrue(Modifier.isFinal(field.getModifiers()));
		}
		for (Field field :
				RepeatOnTouchListener.class.getDeclaredFields()) {
			assertFalse(Modifier.isStatic(field.getModifiers()));
		}
	}

	@Test
	public void fontFaceNavigationIsAStatelessPureBoundary() {
		assertTrue(Modifier.isFinal(
				FontFaceSwitcher.class.getModifiers()));
		assertEquals(
				0,
				FontFaceSwitcher.class.getDeclaredFields().length);
	}

	@Test
	public void documentPositionPolicyIsAStatelessPureBoundary() {
		assertTrue(Modifier.isFinal(
				DocumentPositionPolicy.class.getModifiers()));
		assertEquals(
				0,
				DocumentPositionPolicy.class
						.getDeclaredFields().length);
	}

	@Test
	public void gestureAccelerationIsReaderOwnedAndImmutable()
			throws Exception {
		Field acceleration =
				ReaderView.class.getDeclaredField("gestureAcceleration");
		assertFalse(Modifier.isStatic(acceleration.getModifiers()));
		assertTrue(Modifier.isPrivate(acceleration.getModifiers()));
		assertTrue(Modifier.isFinal(acceleration.getModifiers()));

		for (Field field : GestureAcceleration.class.getDeclaredFields()) {
			if (field.getType().isArray()) {
				assertFalse(Modifier.isStatic(field.getModifiers()));
				assertTrue(Modifier.isPrivate(field.getModifiers()));
				assertTrue(Modifier.isFinal(field.getModifiers()));
			}
		}
		for (Field field : ReaderView.class.getDeclaredFields()) {
			assertFalse(
					"ReaderView retains the legacy acceleration array",
					field.getName().equals("accelerationShape"));
		}
	}

	@Test
	public void animationTimingBelongsToReaderAndUsesPrivateState()
			throws Exception {
		Field timing =
				ReaderView.class.getDeclaredField("animationTiming");
		assertFalse(Modifier.isStatic(timing.getModifiers()));
		assertTrue(Modifier.isPrivate(timing.getModifiers()));
		assertTrue(Modifier.isFinal(timing.getModifiers()));

		for (Field field : AnimationTiming.class.getDeclaredFields()) {
			if (Modifier.isStatic(field.getModifiers())) {
				assertTrue(Modifier.isFinal(field.getModifiers()));
				assertTrue(field.getType().isPrimitive());
			} else {
				assertTrue(Modifier.isPrivate(field.getModifiers()));
				if (field.getType().isArray())
					assertTrue(Modifier.isFinal(field.getModifiers()));
			}
		}
		for (Class<?> nested : ReaderView.class.getDeclaredClasses()) {
			assertFalse(
					"Animation timing must not remain nested in ReaderView",
					nested.getSimpleName().equals("RingBuffer"));
		}
	}

	@Test
	public void readingTimeBelongsToReaderAndHasNoReadMutation()
			throws Exception {
		Field tracker =
				ReaderView.class.getDeclaredField("readingTimeTracker");
		assertFalse(Modifier.isStatic(tracker.getModifiers()));
		assertTrue(Modifier.isPrivate(tracker.getModifiers()));
		assertTrue(Modifier.isFinal(tracker.getModifiers()));

		for (Field field : ReadingTimeTracker.class.getDeclaredFields()) {
			if (Modifier.isStatic(field.getModifiers())) {
				assertTrue(Modifier.isFinal(field.getModifiers()));
				assertTrue(field.getType().isPrimitive());
			} else {
				assertTrue(Modifier.isPrivate(field.getModifiers()));
			}
		}
		for (String methodName : new String[]{
				"start",
				"stop",
				"elapsed",
				"setElapsed",
				"isRunning"}) {
			Method method = methodName.equals("setElapsed")
					|| methodName.equals("start")
					|| methodName.equals("stop")
					|| methodName.equals("elapsed")
					? ReadingTimeTracker.class.getDeclaredMethod(
							methodName, long.class)
					: ReadingTimeTracker.class.getDeclaredMethod(methodName);
			assertTrue(Modifier.isSynchronized(method.getModifiers()));
		}
		for (String legacy : new String[]{
				"statStartTime",
				"statTimeElapsed"}) {
			for (Field field : ReaderView.class.getDeclaredFields()) {
				assertFalse(
						"ReaderView retains inline reading-time state "
								+ legacy,
						field.getName().equals(legacy));
			}
		}
	}

	@Test
	public void styleOptionsHaveOneImmutableTypedCatalog()
			throws Exception {
		assertFinalStaticField(
				OptionsDialog.class, "STYLE_OPTION_CATALOG");
		for (Field field : StyleOptionCatalog.class.getDeclaredFields()) {
			assertFalse(Modifier.isStatic(field.getModifiers()));
			assertTrue(Modifier.isPrivate(field.getModifiers()));
			assertTrue(Modifier.isFinal(field.getModifiers()));
		}
		for (Field field :
				StyleOptionCatalog.Entry.class.getDeclaredFields()) {
			assertFalse(Modifier.isStatic(field.getModifiers()));
			assertTrue(Modifier.isPrivate(field.getModifiers()));
			assertTrue(Modifier.isFinal(field.getModifiers()));
		}
		for (Field field : OptionsDialog.class.getDeclaredFields()) {
			assertFalse(
					"OptionsDialog retains parallel style arrays",
					field.getName().equals("styleCodes")
							|| field.getName().equals("styleTitles"));
		}
	}

	@Test
	public void dictionaryDefinitionsHaveOneImmutableCatalog()
			throws Exception {
		Field catalog =
				Dictionaries.class.getDeclaredField("DICTIONARY_CATALOG");
		assertTrue(Modifier.isStatic(catalog.getModifiers()));
		assertTrue(Modifier.isPrivate(catalog.getModifiers()));
		assertTrue(Modifier.isFinal(catalog.getModifiers()));

		for (Field field :
				Dictionaries.DictInfo.class.getDeclaredFields()) {
			assertFalse(Modifier.isStatic(field.getModifiers()));
			assertTrue(
					"Dictionary definition must be immutable: "
							+ field.getName(),
					Modifier.isFinal(field.getModifiers()));
		}
		for (Field field : Dictionaries.class.getDeclaredFields()) {
			assertFalse(
					"Dictionaries exposes mutable static array "
							+ field.getName(),
					Modifier.isStatic(field.getModifiers())
							&& field.getType().isArray());
		}

		Class<?> catalogClass =
				Class.forName("org.coolreader.DictionaryCatalog");
		for (Field field : catalogClass.getDeclaredFields()) {
			assertFalse(Modifier.isStatic(field.getModifiers()));
			assertTrue(Modifier.isPrivate(field.getModifiers()));
			assertTrue(Modifier.isFinal(field.getModifiers()));
		}
	}

	@Test
	public void utilsLookupCatalogsHaveImmutablePrivateStorage()
			throws Exception {
		for (String name : new String[]{
				"AUDIO_FILE_SELECTOR",
				"FILE_NAME_TRANSCRIBER"}) {
			Field owner = Utils.class.getDeclaredField(name);
			assertTrue(Modifier.isStatic(owner.getModifiers()));
			assertTrue(Modifier.isPrivate(owner.getModifiers()));
			assertTrue(Modifier.isFinal(owner.getModifiers()));
		}
		for (Class<?> owner : new Class<?>[]{
				AudioFileSelector.class,
				FileNameTranscriber.class}) {
			for (Field field : owner.getDeclaredFields()) {
				assertFalse(Modifier.isStatic(field.getModifiers()));
				assertTrue(Modifier.isPrivate(field.getModifiers()));
				assertTrue(Modifier.isFinal(field.getModifiers()));
			}
		}
		for (Class<?> nested :
				FileNameTranscriber.class.getDeclaredClasses()) {
			for (Field field : nested.getDeclaredFields()) {
				assertFalse(Modifier.isStatic(field.getModifiers()));
				assertTrue(Modifier.isPrivate(field.getModifiers()));
				assertTrue(Modifier.isFinal(field.getModifiers()));
			}
		}
		for (Field field : Utils.class.getDeclaredFields()) {
			assertFalse(
					"Utils exposes a legacy mutable lookup array",
					field.getName().equals("AUDIO_FILE_EXTS")
							|| field.getName().equals("substTables"));
		}
		for (Class<?> nested : OPDSUtil.class.getDeclaredClasses()) {
			assertFalse(
					"Filename transliteration must not belong to OPDS",
					nested.getSimpleName().equals("SubstTable"));
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
	public void baseActivityConfigurationIsImmutableAndGenerationScoped()
			throws Exception {
		for (String name : new String[]{
				"PREF_FILE",
				"PREF_LAST_BOOK",
				"PREF_LAST_LOCATION",
				"PREF_LAST_NOTIFICATION_MASK",
				"PREF_LAST_LOGCAT",
				"PREF_HELP_FILE"}) {
			assertFinalStaticField(BaseActivity.class, name);
		}

		Field systemLocale =
				BaseActivity.class.getDeclaredField("systemLocale");
		assertFalse(Modifier.isStatic(systemLocale.getModifiers()));
		assertTrue(Modifier.isPrivate(systemLocale.getModifiers()));
		assertTrue(Modifier.isFinal(systemLocale.getModifiers()));
		assertEquals(java.util.Locale.class, systemLocale.getType());
		for (Field field : BaseActivity.class.getDeclaredFields()) {
			assertFalse(
					"BaseActivity retains a process locale snapshot",
					field.getName().equals("defaultLocale"));
		}
		for (Field field : AppLocaleSelection.class.getDeclaredFields()) {
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
		Field debugReset =
				settingsManager.getDeclaredField("DEBUG_RESET_OPTIONS");
		assertTrue(Modifier.isStatic(debugReset.getModifiers()));
		assertTrue(Modifier.isFinal(debugReset.getModifiers()));
	}

	@Test
	public void interfaceThemesAreImmutableAndActivityOwned()
			throws Exception {
		Field catalog =
				BaseActivity.class.getDeclaredField("interfaceThemes");
		assertFalse(Modifier.isStatic(catalog.getModifiers()));
		assertTrue(Modifier.isPrivate(catalog.getModifiers()));
		assertTrue(Modifier.isFinal(catalog.getModifiers()));

		assertTrue(Modifier.isFinal(
				InterfaceTheme.class.getModifiers()));
		for (Field field : InterfaceTheme.class.getDeclaredFields()) {
			assertFalse(Modifier.isStatic(field.getModifiers()));
			assertTrue(Modifier.isPrivate(field.getModifiers()));
			assertTrue(Modifier.isFinal(field.getModifiers()));
		}
		for (Class<?> nested : InterfaceTheme.class.getDeclaredClasses()) {
			for (Field field : nested.getDeclaredFields()) {
				assertFalse(Modifier.isStatic(field.getModifiers()));
				assertTrue(Modifier.isPrivate(field.getModifiers()));
				assertTrue(Modifier.isFinal(field.getModifiers()));
			}
		}
		for (Field field :
				InterfaceThemeCatalog.class.getDeclaredFields()) {
			assertFalse(Modifier.isStatic(field.getModifiers()));
			assertTrue(Modifier.isPrivate(field.getModifiers()));
			assertTrue(Modifier.isFinal(field.getModifiers()));
		}
	}

	@Test
	public void profileSettingsFilteringIsImmutableAndSettingsOwned()
			throws Exception {
		Class<?> settingsManager = null;
		for (Class<?> nested : BaseActivity.class.getDeclaredClasses()) {
			if (nested.getSimpleName().equals("SettingsManager"))
				settingsManager = nested;
		}
		assertTrue(settingsManager != null);
		Field filter =
				settingsManager.getDeclaredField("profileSettingsFilter");
		assertFalse(Modifier.isStatic(filter.getModifiers()));
		assertTrue(Modifier.isPrivate(filter.getModifiers()));
		assertTrue(Modifier.isFinal(filter.getModifiers()));

		for (Field field :
				ProfileSettingsFilter.class.getDeclaredFields()) {
			assertFalse(Modifier.isStatic(field.getModifiers()));
			assertTrue(Modifier.isPrivate(field.getModifiers()));
			assertTrue(Modifier.isFinal(field.getModifiers()));
		}
		for (Field field : Settings.class.getDeclaredFields()) {
			assertFalse(
					"Settings exposes the mutable profile rule array",
					field.getName().equals("PROFILE_SETTINGS"));
		}
	}

	@Test
	public void settingsPersistenceIsStatelessAndSettingsOwned()
			throws Exception {
		Class<?> settingsManager = null;
		for (Class<?> nested : BaseActivity.class.getDeclaredClasses()) {
			if (nested.getSimpleName().equals("SettingsManager"))
				settingsManager = nested;
		}
		assertTrue(settingsManager != null);
		Field store =
				settingsManager.getDeclaredField("settingsFileStore");
		assertFalse(Modifier.isStatic(store.getModifiers()));
		assertTrue(Modifier.isPrivate(store.getModifiers()));
		assertTrue(Modifier.isFinal(store.getModifiers()));

		assertTrue(
				Modifier.isFinal(
						SettingsFileStore.class.getModifiers()));
		assertEquals(
				0,
				SettingsFileStore.class
						.getDeclaredFields().length);
		for (Field field : settingsManager.getDeclaredFields()) {
			assertFalse(
					"SettingsManager retains a dead save executor",
					field.getName().equals("saveSettingsTask"));
		}
	}

	@Test
	public void audiobookTimingCacheIsImmutableAndMatcherOwned()
			throws Exception {
		Field cache =
				WordTimingAudiobookMatcher.class.getDeclaredField(
						"timingCache");
		assertFalse(Modifier.isStatic(cache.getModifiers()));
		assertTrue(Modifier.isPrivate(cache.getModifiers()));
		assertTrue(Modifier.isFinal(cache.getModifiers()));

		assertTrue(
				Modifier.isFinal(
						AudiobookTimingCache.class
								.getModifiers()));
		assertEquals(
				0,
				AudiobookTimingCache.class
						.getDeclaredFields().length);
		for (Field field :
				AudiobookTimingCache.Entry.class
						.getDeclaredFields()) {
			assertFalse(Modifier.isStatic(field.getModifiers()));
			assertTrue(Modifier.isPrivate(field.getModifiers()));
			assertTrue(Modifier.isFinal(field.getModifiers()));
		}
	}

	@Test
	public void progressDisplayIsSynchronousAndStateless()
			throws Exception {
		for (String name : new String[]{
				"mProgressNumber",
				"mProgressPercent",
				"mContext"}) {
			Field field =
					ProgressDialog.class.getDeclaredField(name);
			assertFalse(Modifier.isStatic(field.getModifiers()));
			assertTrue(Modifier.isPrivate(field.getModifiers()));
			if (name.equals("mContext"))
				assertTrue(
						Modifier.isFinal(field.getModifiers()));
		}
		for (Field field :
				ProgressDialog.class.getDeclaredFields()) {
			assertFalse(
					"ProgressDialog retains an async update handler",
					field.getName().equals(
							"mViewUpdateHandler"));
		}
		assertTrue(
				Modifier.isFinal(
						ProgressDisplayState.class
								.getModifiers()));
		assertEquals(
				0,
				ProgressDisplayState.class
						.getDeclaredFields().length);
		for (Field field :
				ProgressDisplayState.Snapshot.class
						.getDeclaredFields()) {
			assertFalse(Modifier.isStatic(field.getModifiers()));
			assertTrue(Modifier.isPrivate(field.getModifiers()));
			assertTrue(Modifier.isFinal(field.getModifiers()));
		}
	}

	@Test
	public void engineProgressUsesIdentityOwnedUiGenerations()
			throws Exception {
		Field progressState =
				Engine.class.getDeclaredField("progressUiState");
		assertFalse(Modifier.isStatic(
				progressState.getModifiers()));
		assertTrue(Modifier.isPrivate(
				progressState.getModifiers()));
		assertTrue(Modifier.isFinal(
				progressState.getModifiers()));
		for (Field field : Engine.class.getDeclaredFields()) {
			assertFalse(
					"Engine retains numeric progress generations",
					field.getName().equals("nextProgressId"));
			assertFalse(
					"Engine retains parallel progress visibility",
					field.getName().equals("progressShown"));
		}

		for (Field field :
				ProgressUiState.class.getDeclaredFields()) {
			assertFalse(Modifier.isStatic(field.getModifiers()));
			assertTrue(Modifier.isPrivate(field.getModifiers()));
		}
		for (Class<?> nested :
				ProgressUiState.class.getDeclaredClasses()) {
			for (Field field : nested.getDeclaredFields()) {
				assertFalse(Modifier.isStatic(
						field.getModifiers()));
				assertTrue(Modifier.isPrivate(
						field.getModifiers()));
				assertTrue(Modifier.isFinal(
						field.getModifiers()));
			}
		}

		Method cancel =
				Engine.DelayedProgress.class.getDeclaredMethod(
						"cancel");
		assertTrue(Modifier.isSynchronized(
				cancel.getModifiers()));
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
