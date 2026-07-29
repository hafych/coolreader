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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class NookEpdControllerBindingsTest {
	@Test
	public void disabledBindingDoesNotResolveVendorClasses() throws Exception {
		NookEpdControllerBindings bindings =
				NookEpdControllerBindings.load(
						false,
						false,
						name -> {
							throw new AssertionError(
									"disabled controller resolved " + name);
						});

		assertFalse(bindings.isAvailable());
		assertFalse(bindings.requiresController());
		assertNull(bindings.createController(new Object()));
	}

	@Test
	public void legacyControllerInvokesStaticVendorMethod() throws Exception {
		LegacyController.reset();
		NookEpdControllerBindings bindings =
				NookEpdControllerBindings.load(
						true,
						false,
						NookEpdControllerBindingsTest::resolveLegacy);

		assertTrue(bindings.isAvailable());
		assertFalse(bindings.requiresController());
		bindings.setMode(
				null,
				N2EpdController.REGION_APP_3,
				N2EpdController.WAVE_GU,
				N2EpdController.MODE_ONESHOT_ALL);

		assertEquals("CoolReader", LegacyController.app);
		assertEquals(LegacyRegion.APP_3, LegacyController.region);
		assertEquals(LegacyWave.GU, LegacyController.params.wave);
		assertEquals(LegacyMode.ONESHOT_ALL, LegacyController.mode);
	}

	@Test
	public void nook120ControllerKeepsHostAndInvokesInstanceMethod()
			throws Exception {
		Nook120Controller.last = null;
		NookEpdControllerBindings bindings =
				NookEpdControllerBindings.load(
						true,
						true,
						NookEpdControllerBindingsTest::resolveNook120);
		Object host = new Object();

		Object controller = bindings.createController(host);
		bindings.setMode(
				controller,
				N2EpdController.REGION_APP_4,
				N2EpdController.WAVE_GL16,
				N2EpdController.MODE_ACTIVE_ALL);

		assertTrue(bindings.isAvailable());
		assertTrue(bindings.requiresController());
		assertSame(host, Nook120Controller.last.host);
		assertEquals("CoolReader", Nook120Controller.last.app);
		assertEquals(Nook120Region.APP_4, Nook120Controller.last.region);
		assertEquals(Nook120Wave.GL16, Nook120Controller.last.params.wave);
		assertEquals(Nook120Mode.ACTIVE_ALL, Nook120Controller.last.mode);
	}

	private static Class<?> resolveLegacy(String name)
			throws ClassNotFoundException {
		switch (name) {
			case "android.hardware.EpdController":
				return LegacyController.class;
			case "android.hardware.EpdController$Wave":
				return LegacyWave.class;
			case "android.hardware.EpdController$Mode":
				return LegacyMode.class;
			case "android.hardware.EpdController$Region":
				return LegacyRegion.class;
			case "android.hardware.EpdController$RegionParams":
				return LegacyRegionParams.class;
			default:
				throw new ClassNotFoundException(name);
		}
	}

	private static Class<?> resolveNook120(String name)
			throws ClassNotFoundException {
		switch (name) {
			case "android.hardware.EpdController":
				return Nook120Controller.class;
			case "android.hardware.EpdRegionParams$Wave":
				return Nook120Wave.class;
			case "android.hardware.EpdController$Mode":
				return Nook120Mode.class;
			case "android.hardware.EpdController$Region":
				return Nook120Region.class;
			case "android.hardware.EpdRegionParams":
				return Nook120RegionParams.class;
			default:
				throw new ClassNotFoundException(name);
		}
	}

	public enum LegacyWave {
		GC, GU, DU, A2, GL16, AUTO
	}

	public enum LegacyRegion {
		APP_1, APP_2, APP_3, APP_4
	}

	public enum LegacyMode {
		BLINK, ACTIVE, ONESHOT, CLEAR, ACTIVE_ALL, ONESHOT_ALL, CLEAR_ALL
	}

	public static final class LegacyRegionParams {
		final LegacyWave wave;

		public LegacyRegionParams(
				int left,
				int top,
				int right,
				int bottom,
				LegacyWave wave) {
			this.wave = wave;
		}
	}

	public static final class LegacyController {
		static String app;
		static LegacyRegion region;
		static LegacyRegionParams params;
		static LegacyMode mode;

		static void reset() {
			app = null;
			region = null;
			params = null;
			mode = null;
		}

		public static void setRegion(
				String app,
				LegacyRegion region,
				LegacyRegionParams params,
				LegacyMode mode) {
			LegacyController.app = app;
			LegacyController.region = region;
			LegacyController.params = params;
			LegacyController.mode = mode;
		}
	}

	public enum Nook120Wave {
		GC, GU, DU, A2, GL16, AUTO
	}

	public enum Nook120Region {
		APP_1, APP_2, APP_3, APP_4
	}

	public enum Nook120Mode {
		BLINK, ACTIVE, ONESHOT, CLEAR, ACTIVE_ALL, ONESHOT_ALL, CLEAR_ALL
	}

	public static final class Nook120RegionParams {
		final Nook120Wave wave;

		public Nook120RegionParams(
				int left,
				int top,
				int right,
				int bottom,
				Nook120Wave wave) {
			this.wave = wave;
		}
	}

	public static final class Nook120Controller {
		static Nook120Controller last;
		final Object host;
		String app;
		Nook120Region region;
		Nook120RegionParams params;
		Nook120Mode mode;

		public Nook120Controller(Object host) {
			this.host = host;
			last = this;
		}

		public void setRegion(
				String app,
				Nook120Region region,
				Nook120RegionParams params,
				Nook120Mode mode) {
			this.app = app;
			this.region = region;
			this.params = params;
			this.mode = mode;
		}
	}
}
