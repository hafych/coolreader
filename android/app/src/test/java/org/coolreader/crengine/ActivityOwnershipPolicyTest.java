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

	private static void assertRetainsNoActivity(Class<?> type) {
		for (Field field : type.getDeclaredFields()) {
			assertFalse(
					type.getSimpleName() + " retains Activity field "
							+ field.getName(),
					Activity.class.isAssignableFrom(field.getType()));
		}
	}
}
