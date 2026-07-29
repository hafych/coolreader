/*
 * CoolReader for Android
 * Copyright (C) 2012 Vadim Lopatin <coolreader.org@gmail.com>
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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;


public final class VMRuntimeHack {
	private final Object runtime;
	private final Method trackAllocation;
	private final Method trackFree;
	private long totalSize;
	
	public synchronized boolean trackAlloc(long size) {
		if (runtime == null || size < 0)
			return false;
		if (!invoke(trackAllocation, size))
			return false;
		totalSize += size;
		return true;
	}

	public synchronized boolean trackFree(long size) {
		if (runtime == null || size < 0)
			return false;
		if (!invoke(trackFree, size))
			return false;
		totalSize -= size;
		return true;
	}

	synchronized long trackedSize() {
		return totalSize;
	}

	private boolean invoke(Method method, long size) {
		try {
			Object res = method.invoke(runtime, Long.valueOf(size));
			return (res instanceof Boolean) ? (Boolean)res : true;
		} catch (IllegalArgumentException e) {
			return false;
		} catch (IllegalAccessException e) {
			return false;
		} catch (InvocationTargetException e) {
			return false;
		}
	}

	public VMRuntimeHack() {
		Object resolvedRuntime = null;
		Method resolvedTrackAllocation = null;
		Method resolvedTrackFree = null;
		if (DeviceInfo.USE_BITMAP_MEMORY_HACK) {
			try {
				Class<?> cl = Class.forName("dalvik.system.VMRuntime");
				Method getRt = cl.getMethod("getRuntime");
				resolvedRuntime = getRt.invoke(null);
				resolvedTrackAllocation =
						cl.getMethod("trackExternalAllocation", long.class);
				resolvedTrackFree =
						cl.getMethod("trackExternalFree", long.class);
			} catch (ReflectiveOperationException
					| IllegalArgumentException
					| SecurityException e) {
				Log.i("cr3", "VMRuntime hack does not work: "
						+ e.getClass().getSimpleName());
				resolvedRuntime = null;
				resolvedTrackAllocation = null;
				resolvedTrackFree = null;
			}
		}
		runtime = resolvedRuntime;
		trackAllocation = resolvedTrackAllocation;
		trackFree = resolvedTrackFree;
	}

	VMRuntimeHack(
			Object runtime,
			Method trackAllocation,
			Method trackFree) {
		boolean allMissing =
				runtime == null && trackAllocation == null && trackFree == null;
		boolean allPresent =
				runtime != null && trackAllocation != null && trackFree != null;
		if (!allMissing && !allPresent)
			throw new IllegalArgumentException(
					"VMRuntime bindings must be either complete or absent");
		this.runtime = runtime;
		this.trackAllocation = trackAllocation;
		this.trackFree = trackFree;
	}
}
