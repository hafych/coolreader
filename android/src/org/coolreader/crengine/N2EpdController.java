/*
 * CoolReader for Android
 * Copyright (C) 2011 dairyknight <dairyknight@gmail.com>
 * Copyright (C) 2011 a_lone
 * Copyright (C) 2012 klush
 * Copyright (C) 2011,2012 Vadim Lopatin <coolreader.org@gmail.com>
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.coolreader.crengine;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;

/**
 * Nook Touch EPD controller interface wrapper.
 * This class is created by DairyKnight for Nook Touch screen support in FBReaderJ.
 * @author DairyKnight <dairyknight@gmail.com>
 * http://forum.xda-developers.com/showthread.php?t=1183173
 */

public class N2EpdController {
	public static final int REGION_APP_1 = 0;
	public static final int REGION_APP_2 = 1;
	public static final int REGION_APP_3 = 2;
	public static final int REGION_APP_4 = 3;
	
	public static final int WAVE_GC = 0;
	public static final int WAVE_GU = 1;
	public static final int WAVE_DU = 2;
	public static final int WAVE_A2 = 3;
	public static final int WAVE_GL16 = 4;
	public static final int WAVE_AUTO = 5;
	
	public static final int MODE_BLINK = 0;
	public static final int MODE_ACTIVE = 1;
	public static final int MODE_ONESHOT = 2;
	public static final int MODE_CLEAR = 3;
	public static final int MODE_ACTIVE_ALL = 4;
	public static final int MODE_ONESHOT_ALL = 5;
	public static final int MODE_CLEAR_ALL = 6;
	
	private final NookEpdControllerBindings bindings;
	private Object mEpdController;

	public N2EpdController() {
		bindings = loadBindings();
	}

	private static NookEpdControllerBindings loadBindings() {
		try {
			return NookEpdControllerBindings.load(
					DeviceInfo.EINK_NOOK,
					DeviceInfo.EINK_NOOK_120,
					Class::forName);
		} catch (Exception e) {
			Log.e("cr3", "Failed to initialize EPD refresh", e);
			return NookEpdControllerBindings.unavailable();
		}
	}

	public void setMode(
			Context context, int region, int wave, int mode) {
		if (bindings.isAvailable()) {
			try {
				if (bindings.requiresController() && mEpdController == null) {
					Activity activity = findActivity(context);
					if (activity == null) {
						Log.e("cr3", "Cannot create EPD controller without Activity");
						return;
					}
					mEpdController = bindings.createController(activity);
				}
				bindings.setMode(mEpdController, region, wave, mode);
			} catch (Exception e) {
				Log.e("cr3", "Failed to set EPD mode", e);
			}
		}
	}

	private static Activity findActivity(Context context) {
		Context current = context;
		while (current instanceof ContextWrapper) {
			if (current instanceof Activity)
				return (Activity) current;
			Context base = ((ContextWrapper) current).getBaseContext();
			if (base == current)
				break;
			current = base;
		}
		return current instanceof Activity ? (Activity) current : null;
	}
}
