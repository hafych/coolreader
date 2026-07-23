/*
 * CoolReader for Android
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 2
 * of the License, or (at your option) any later version.
 */

package org.coolreader;

import android.app.Application;

import org.coolreader.crengine.CrashPrivacy;

public class CoolReaderApplication extends Application {
	@Override
	public void onCreate() {
		super.onCreate();
		CrashPrivacy.install();
	}
}
