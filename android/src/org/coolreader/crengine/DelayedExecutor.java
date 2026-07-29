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

import android.os.Handler;

/**
 * Schedulable cancelable replaceable task.
 * Can schedule execution of runnable.
 * When previously scheduled runnable was not yet executed, it's being canceled and being replaced with new one.
 */
public final class DelayedExecutor {

	public static final Logger log = L.create("dt", Log.INFO);
	
	private final boolean isBackground;
	private Handler handler;
	private final ReplaceableTaskSlot tasks =
			new ReplaceableTaskSlot();
	private final String name;

	private Handler getHandler() {
		if (handler != null)
			return handler;
		if (isBackground)
			handler = BackgroundThread.getBackgroundHandler();
		else
			handler = BackgroundThread.getGUIHandler();
		if (handler == null)
			throw new RuntimeException("Cannot get handler");
		return handler;
	}
	
	public static DelayedExecutor createBackground(String name) {
		return new DelayedExecutor(true, name);
	}
	
	public static DelayedExecutor createGUI(String name) {
		return new DelayedExecutor(false, name);
	}
	
	/**
	 * Run ASAP.
	 * @param task is task to execute.
	 */
	public void post(final Runnable task) {
		postDelayed(task, 0L);
	}

	/**
	 * Run delayed.
	 * @param task is task to execute delayed.
	 * @param delay is delay, milliseconds.
	 */
	public synchronized void postDelayed(
			final Runnable task,
			final long delay) {
		Handler target = getHandler();
		Runnable loggedTask = new Runnable() {
			@Override
			public void run() {
				try {
					log.v("Running task " + toString());
					task.run();
					log.v("Done task " + toString());
				} catch (Exception e) {
					log.e("Exception while executing task", e);
				}
			}

			@Override
			public String toString() {
				return name + " " + task.hashCode();
			}
		};
		ReplaceableTaskSlot.Replacement replacement =
				tasks.replace(loggedTask);
		if (replacement.previous() != null) {
			log.d("Cancelling pending task "
					+ replacement.previous());
			target.removeCallbacks(replacement.previous());
		}
		boolean accepted;
		if (delay > 0) {
			log.d("Posting delayed task "
					+ replacement.current() + " delay=" + delay);
			accepted = target.postDelayed(
					replacement.current(), delay);
		} else {
			log.d("Posting task " + replacement.current());
			accepted = target.post(replacement.current());
		}
		if (!accepted)
			tasks.cancel();
	}

	public synchronized void cancel() {
		Runnable canceled = tasks.cancel();
		if (canceled != null) {
			log.d("Cancelling pending task " + canceled);
			getHandler().removeCallbacks(canceled);
		}
	}
	
	private DelayedExecutor(boolean isBackground, String name) {
		this.isBackground = isBackground;
		this.name = name;
	}
}
