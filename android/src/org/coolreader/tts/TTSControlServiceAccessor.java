/*
 * CoolReader for Android
 * Copyright (C) 2020,2021 Aleksey Chernov <valexlin@gmail.com>
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

package org.coolreader.tts;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import org.coolreader.crengine.Log;

import java.util.ArrayList;
import java.util.List;

public class TTSControlServiceAccessor {
	private final static String TAG = "ttssrv";
	private final Context mContext;
	private final Object mLocker = new Object();
	private final ArrayList<TTSControlBinder.Callback> onConnectCallbacks =
			new ArrayList<>();
	private TTSControlBinder mServiceBinder;
	private boolean mBindingRegistered;

	public interface Callback {
		void run(TTSControlServiceAccessor ttsacc);
	}

	public TTSControlServiceAccessor(Context context) {
		mContext = context.getApplicationContext();
	}

	public boolean bind(
			final TTSControlBinder.Callback boundCallback) {
		TTSControlBinder connectedBinder;
		synchronized (mLocker) {
			connectedBinder = mServiceBinder;
			if (connectedBinder == null) {
				if (boundCallback != null)
					onConnectCallbacks.add(boundCallback);
				if (mBindingRegistered)
					return true;
				mBindingRegistered = true;
				boolean bound;
				RuntimeException bindError = null;
				try {
					bound = mContext.bindService(
							new Intent(
									mContext,
									TTSControlService.class),
							mServiceConnection,
							Context.BIND_AUTO_CREATE);
				} catch (RuntimeException e) {
					bindError = e;
					bound = false;
				}
				if (bound) {
					Log.v(
							TAG,
							"binding TTSControlService in progress...");
				} else {
					Log.e(
							TAG,
							"cannot bind TTSControlService",
							bindError);
					mBindingRegistered = false;
					onConnectCallbacks.clear();
				}
				return bound;
			}
		}
		if (boundCallback != null) {
				Log.v(TAG, "TTSControlService is already bound");
			boundCallback.run(connectedBinder);
		}
		return true;
	}

	public void unbind() {
		Log.v(TAG, "unbinding TTSControlService");
		boolean shouldUnbind;
		synchronized (mLocker) {
			shouldUnbind = mBindingRegistered;
			mBindingRegistered = false;
			mServiceBinder = null;
			onConnectCallbacks.clear();
		}
		if (shouldUnbind)
			mContext.unbindService(mServiceConnection);
	}

	private final ServiceConnection mServiceConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			List<TTSControlBinder.Callback> callbacks;
			TTSControlBinder binder = (TTSControlBinder) service;
			synchronized (mLocker) {
				if (!mBindingRegistered)
					return;
				mServiceBinder = binder;
				callbacks = new ArrayList<>(onConnectCallbacks);
				onConnectCallbacks.clear();
			}
			Log.i(TAG, "connected to TTSControlService");
			for (TTSControlBinder.Callback callback : callbacks)
				callback.run(binder);
		}

		public void onServiceDisconnected(ComponentName className) {
			synchronized (mLocker) {
				mServiceBinder = null;
			}
			Log.i(TAG, "Connection to the TTSControlService has been lost");
		}
	};

}
