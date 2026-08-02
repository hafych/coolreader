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

package org.coolreader.db;

import java.util.List;

import org.coolreader.crengine.L;
import org.coolreader.crengine.Logger;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

/**
 * Thread to run background tasks inside.
 * @author Vadim Lopatin
 */
public class ServiceThread extends Thread {

	public static final Logger log = L.create("st");

	private final ServiceThreadState threadState =
			new ServiceThreadState();
	
	public ServiceThread(String name) {
		super(name);
	}

	/**
	 * Post task for execution. 
	 * @param task is runnable to call
	 */
	public void post(Runnable task) {
		if (threadState.enqueueIfStopped(task)) {
			log.w("Thread is not yet started, just adding to queue " + task);
			return;
		}
		Handler handler = threadState.getHandler();
		if (handler != null) {
			drainQueued(handler);
			handler.post(task);
		}
	}
	
	/**
	 * Post task for execution at front of queue. 
	 * @param task is runnable to call
	 */
	public void postAtFrontOfQueue(Runnable task) {
		if (threadState.enqueueIfStopped(task))
			return;
		Handler handler = threadState.getHandler();
		if (handler != null) {
			drainQueued(handler);
			handler.postAtFrontOfQueue(task);
		}
	}
	
	/**
	 * Post task for execution with delay. 
	 * @param task is runnable to call
	 */
	public void postDelayed(Runnable task, long delayMillis) {
		if (threadState.enqueueIfStopped(task))
			return;
		Handler handler = threadState.getHandler();
		if (handler != null) {
			drainQueued(handler);
			handler.postDelayed(task, delayMillis);
		}
	}
	
	private void drainQueued(Handler handler) {
		List<Runnable> queued = threadState.drainQueue();
		for (Runnable t : queued) {
			log.w("Executing queued task " + t);
			handler.post(t);
		}
	}
	
	public boolean waitForCompletion(long timeout) {
		final Object lock = new Object();
		Handler handler = threadState.getHandler();
		if (handler == null)
			return false;
		synchronized (lock) {
			handler.post(() -> {
				synchronized (lock) {
					lock.notify();
				}
			});
			try {
				lock.wait(timeout);
				return true;
			} catch (InterruptedException e) {
				L.i("Waiting is interrupted");
			}
		}
		return false;
	}
	
	public void stop(final long timeout) {
		L.i("Stop is called. Not supported.");
		waitForCompletion(timeout);
		Handler handler = threadState.getHandler();
		if (handler != null)
			handler.getLooper().quit();
	}
	
	public boolean isStopped() {
		return threadState.isStopped();
	}

	@Override
	public void run() {
		log.i("Running service thread");
		Looper.prepare();
		threadState.setHandler(new Handler() {
			public void handleMessage( Message message )
			{
				log.d("message: " + message);
			}
		});
		log.i("Service thread handler is created");
		Handler handler = threadState.getHandler();
		if (handler != null)
			drainQueued(handler);
		threadState.setStopped(false);
		Looper.loop();
		threadState.clearHandler();
		threadState.setStopped(true);
		log.i("Exiting background thread");
	}
}
