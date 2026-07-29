/*
 * CoolReader for Android
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 2
 * of the License, or (at your option) any later version.
 */

package org.coolreader.crengine;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * Immutable reflection bindings for the proprietary Nook EPD API.
 *
 * The vendor classes are absent from normal Android SDKs, so resolution stays
 * behind a narrow injectable interface and belongs to one screen-controller
 * generation rather than a mutable process-wide cache.
 */
final class NookEpdControllerBindings {
	interface ClassResolver {
		Class<?> resolve(String name) throws ClassNotFoundException;
	}

	private final Method setRegion;
	private final Constructor<?> regionParamsConstructor;
	private final Constructor<?> controllerConstructor;
	private final Object[] waves;
	private final Object[] regions;
	private final Object[] modes;

	private NookEpdControllerBindings(
			Method setRegion,
			Constructor<?> regionParamsConstructor,
			Constructor<?> controllerConstructor,
			Object[] waves,
			Object[] regions,
			Object[] modes) {
		this.setRegion = setRegion;
		this.regionParamsConstructor = regionParamsConstructor;
		this.controllerConstructor = controllerConstructor;
		this.waves = waves;
		this.regions = regions;
		this.modes = modes;
	}

	static NookEpdControllerBindings unavailable() {
		return new NookEpdControllerBindings(
				null,
				null,
				null,
				new Object[0],
				new Object[0],
				new Object[0]);
	}

	static NookEpdControllerBindings load(
			boolean enabled,
			boolean requiresController,
			ClassResolver resolver) throws ReflectiveOperationException {
		if (!enabled)
			return unavailable();

		Class<?> controllerClass =
				resolver.resolve("android.hardware.EpdController");
		Class<?> waveClass = resolver.resolve(
				requiresController
						? "android.hardware.EpdRegionParams$Wave"
						: "android.hardware.EpdController$Wave");
		Class<?> modeClass =
				resolver.resolve("android.hardware.EpdController$Mode");
		Class<?> regionClass =
				resolver.resolve("android.hardware.EpdController$Region");
		Class<?> regionParamsClass = resolver.resolve(
				requiresController
						? "android.hardware.EpdRegionParams"
						: "android.hardware.EpdController$RegionParams");

		Object[] waves = enumConstants(waveClass);
		Object[] regions = enumConstants(regionClass);
		Object[] modes = enumConstants(modeClass);
		Constructor<?> regionParamsConstructor =
				regionParamsClass.getConstructor(
						Integer.TYPE,
						Integer.TYPE,
						Integer.TYPE,
						Integer.TYPE,
						waveClass);
		Method setRegion = controllerClass.getMethod(
				"setRegion",
				String.class,
				regionClass,
				regionParamsClass,
				modeClass);
		Constructor<?> controllerConstructor = null;
		if (requiresController) {
			Constructor<?>[] constructors = controllerClass.getConstructors();
			if (constructors.length == 0)
				throw new NoSuchMethodException(
						controllerClass.getName() + " has no public constructor");
			controllerConstructor = constructors[0];
		}
		return new NookEpdControllerBindings(
				setRegion,
				regionParamsConstructor,
				controllerConstructor,
				waves,
				regions,
				modes);
	}

	boolean isAvailable() {
		return setRegion != null;
	}

	boolean requiresController() {
		return controllerConstructor != null;
	}

	Object createController(Object activity) throws ReflectiveOperationException {
		if (controllerConstructor == null)
			return null;
		return controllerConstructor.newInstance(activity);
	}

	void setMode(Object controller, int region, int wave, int mode)
			throws ReflectiveOperationException {
		Object regionParams = regionParamsConstructor.newInstance(
				0,
				0,
				600,
				800,
				waves[wave]);
		setRegion.invoke(
				controller,
				"CoolReader",
				regions[region],
				regionParams,
				modes[mode]);
	}

	private static Object[] enumConstants(Class<?> type) {
		Object[] values = type.getEnumConstants();
		if (values == null)
			throw new IllegalArgumentException(type.getName() + " is not an enum");
		return values.clone();
	}
}
