/*
 * CoolReader for Android
 * Copyright (C) 2012,2014 Vadim Lopatin <coolreader.org@gmail.com>
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

package org.coolreader.plugins;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.coolreader.crengine.FileInfo;
import org.coolreader.plugins.litres.LitresPlugin;

import android.content.Context;
import android.content.SharedPreferences;

public class OnlineStorePluginManager {
	private static final ConcurrentMap<String, OnlineStoreWrapper> pluginMap =
			new ConcurrentHashMap<>();

	public static OnlineStoreWrapper getPlugin(Context context, String path) {
		if (context == null)
			return null;
		if (!path.startsWith(FileInfo.ONLINE_CATALOG_PLUGIN_PREFIX))
			path = FileInfo.ONLINE_CATALOG_PLUGIN_PREFIX + path;
		int pos = path.indexOf(":");
		String packageName = path.substring(pos + 1);
		OnlineStoreWrapper wrapper = pluginMap.get(packageName);
		if (wrapper == null) {
			Context applicationContext = context.getApplicationContext();
			if (applicationContext == null)
				return null;
			SharedPreferences preferences =
					applicationContext.getSharedPreferences(
							FileInfo.ONLINE_CATALOG_PLUGIN_PREFIX,
							Context.MODE_PRIVATE);
			if (LitresPlugin.PACKAGE_NAME.equals(packageName))
				wrapper = new OnlineStoreWrapper(
						new LitresPlugin(
								applicationContext,
								preferences));
			if (wrapper != null) {
				OnlineStoreWrapper existing =
						pluginMap.putIfAbsent(packageName, wrapper);
				if (existing != null)
					wrapper = existing;
			}
		}
		return wrapper;
	}
	public static final String PLUGIN_PKG_LITRES = "org.coolreader.plugins.litres";
}
