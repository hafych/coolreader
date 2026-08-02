/*
 * CoolReader for Android
 * Copyright (C) 2021 Aleksey Chernov <valexlin@gmail.com>
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

package org.coolreader.sync2;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import org.coolreader.crengine.Log;

import java.util.List;

public class SyncServiceAccessor {
	private final static String TAG = "sync2acc";
	private final Context mContext;
	private final SyncServiceBindingState bindingState =
			new SyncServiceBindingState();
	private final Object mLocker = new Object();

	public interface Callback {
		void run(SyncServiceAccessor acc);
	}

	public SyncServiceAccessor(Context context) {
		mContext = context.getApplicationContext();
	}

	public void bind(final SyncServiceBinder.Callback boundCallback) {
		SyncServiceBinder ready;
		synchronized (mLocker) {
			if (bindingState.isReady()) {
				Log.v(TAG, "SyncService is already bound");
				ready = bindingState.getBinder();
			} else {
				ready = null;
				if (boundCallback != null)
					bindingState.addPending(boundCallback);
				if (!bindingState.isBindCalled()) {
					bindingState.setBindCalled(true);
					if (mContext.bindService(
							new Intent(mContext, SyncService.class),
							mServiceConnection,
							Context.BIND_AUTO_CREATE)) {
						bindingState.setServiceBound(true);
						Log.v(TAG, "binding SyncService in progress...");
					} else {
						Log.e(TAG, "cannot bind SyncService");
					}
				}
			}
		}
		if (ready != null && boundCallback != null)
			boundCallback.run(ready);
	}

	public void unbind() {
		Log.v(TAG, "unbinding SyncService");
		boolean shouldUnbind;
		synchronized (mLocker) {
			shouldUnbind = bindingState.isServiceBound();
			bindingState.unbind();
		}
		if (shouldUnbind)
			mContext.unbindService(mServiceConnection);
	}

	public boolean isServiceBound() {
		synchronized (mLocker) {
			return bindingState.isServiceBound();
		}
	}

	private final ServiceConnection mServiceConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			List<SyncServiceBinder.Callback> callbacks;
			SyncServiceBinder binder;
			synchronized (mLocker) {
				binder = (SyncServiceBinder) service;
				bindingState.setBinder(binder);
				Log.i(TAG, "connected to SyncService");
				callbacks = bindingState.takePending();
			}
			for (SyncServiceBinder.Callback callback : callbacks)
				callback.run(binder);
		}

		public void onServiceDisconnected(ComponentName className) {
			synchronized (mLocker) {
				bindingState.connectionLost();
			}
			Log.i(TAG, "Connection to the SyncService has been lost (abnormal termination)");
		}
	};

}
