/*
 * CoolReader for Android
 * Copyright (C) 2012,2020 Vadim Lopatin <coolreader.org@gmail.com>
 * Copyright (C) 2018,2019,2021 Aleksey Chernov <valexlin@gmail.com>
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

package org.coolreader.db;

import java.util.List;

import org.coolreader.crengine.MountPathCorrector;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import org.coolreader.crengine.Log;

public class CRDBServiceAccessor {
	private final static String TAG = "cr3db";
	private final Context mContext;
	private final Object mLocker = new Object();
	private final ServiceBindingState<CRDBService.LocalBinder> bindingState =
			new ServiceBindingState<>();
	private Binding currentBinding;
	private MountPathCorrector pathCorrector;

	public CRDBService.LocalBinder get() {
		CRDBService.LocalBinder binder = getOrNull();
		if (binder == null)
			throw new RuntimeException("no service");
		return binder;
	}

	public CRDBService.LocalBinder getOrNull() {
		return bindingState.getBinder();
	}
    
	public CRDBServiceAccessor(Context context, MountPathCorrector pathCorrector) {
		mContext = context.getApplicationContext();
		this.pathCorrector = pathCorrector;
	}

	public void setPathCorrector(MountPathCorrector pathCorrector) {
		synchronized (mLocker) {
			this.pathCorrector = pathCorrector;
			CRDBService.LocalBinder binder =
					bindingState.getBinder();
			if (binder != null && pathCorrector != null)
				binder.setPathCorrector(pathCorrector);
		}
	}

	public void bind(final Runnable boundCallback) {
		Runnable immediateCallback;
		synchronized (mLocker) {
			ServiceBindingState.BindRequest request =
					bindingState.requestBind(boundCallback);
			immediateCallback = request.getImmediateCallback();
			if (request.shouldStartBinding()) {
				ServiceBindingState.Registration registration =
						request.getRegistration();
				ServiceConnection connection =
						createServiceConnection(registration);
				currentBinding =
						new Binding(registration, connection);
				boolean bound;
				RuntimeException bindError = null;
				try {
					bound = mContext.bindService(
							new Intent(
									mContext,
									CRDBService.class),
							connection,
							Context.BIND_AUTO_CREATE);
				} catch (RuntimeException e) {
					bindError = e;
					bound = false;
				}
				if (bound) {
					Log.v(
							TAG,
							"binding CRDBService in progress...");
				} else {
					Log.e(
							TAG,
							"cannot bind CRDBService",
							bindError);
					bindingState.bindingFailed(registration);
					currentBinding = null;
				}
			}
		}
		if (immediateCallback != null) {
			Log.v(TAG, "CRDBService is already bound");
			immediateCallback.run();
		}
	}

	public void unbind() {
		Log.v(TAG, "unbinding CRDBService");
		Binding binding;
		synchronized (mLocker) {
			ServiceBindingState.Registration registration =
					bindingState.unbind();
			binding =
					currentBinding != null
									&& currentBinding.registration
											== registration
							? currentBinding
							: null;
			currentBinding = null;
		}
		if (binding != null)
			unbindPlatformConnection(binding.connection);
	}

	private ServiceConnection createServiceConnection(
			ServiceBindingState.Registration registration) {
		return new ServiceConnection() {
			@Override
			public void onServiceConnected(
					ComponentName className,
					IBinder service) {
				CRDBService.LocalBinder binder =
						(CRDBService.LocalBinder) service;
				List<Runnable> callbacks;
				synchronized (mLocker) {
					if (!bindingState.isCurrent(registration))
						return;
					if (pathCorrector != null)
						binder.setPathCorrector(pathCorrector);
					callbacks =
							bindingState.connected(
									registration,
									binder);
				}
				Log.i(TAG, "connected to CRDBService");
				for (Runnable callback : callbacks) {
					if (!bindingState.isConnected(
							registration,
							binder))
						return;
					callback.run();
				}
			}

			@Override
			public void onServiceDisconnected(
					ComponentName className) {
				boolean disconnected;
				synchronized (mLocker) {
					disconnected =
							bindingState.disconnected(
									registration);
				}
				if (disconnected)
					Log.i(TAG, "disconnected from CRDBService");
			}

			@Override
			public void onBindingDied(ComponentName className) {
				closeFailedBinding(
						registration,
						this,
						"CRDBService binding died");
			}

			@Override
			public void onNullBinding(ComponentName className) {
				closeFailedBinding(
						registration,
						this,
						"CRDBService returned a null binding");
			}
		};
	}

	private void closeFailedBinding(
			ServiceBindingState.Registration registration,
			ServiceConnection connection,
			String message) {
		boolean shouldUnbind = false;
		synchronized (mLocker) {
			if (bindingState.bindingFailed(registration)) {
				shouldUnbind =
						currentBinding != null
								&& currentBinding.registration
										== registration;
				if (shouldUnbind)
					currentBinding = null;
			}
		}
		if (shouldUnbind) {
			Log.e(TAG, message);
			unbindPlatformConnection(connection);
		}
	}

	private void unbindPlatformConnection(
			ServiceConnection connection) {
		try {
			mContext.unbindService(connection);
		} catch (IllegalArgumentException e) {
			Log.e(
					TAG,
					"CRDBService connection was already unbound",
					e);
		}
	}

	private static final class Binding {
		private final ServiceBindingState.Registration registration;
		private final ServiceConnection connection;

		private Binding(
				ServiceBindingState.Registration registration,
				ServiceConnection connection) {
			this.registration = registration;
			this.connection = connection;
		}
	}
}
