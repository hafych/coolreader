/*
 * CoolReader for Android
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 2
 * of the License, or (at your option) any later version.
 */

package org.coolreader.crengine;

/**
 * One-shot callback whose delegate can be released before delivery.
 */
final class DetachableRunnable implements Runnable {
	private Runnable delegate;

	DetachableRunnable(Runnable delegate) {
		if (delegate == null)
			throw new IllegalArgumentException(
					"delegate must not be null");
		this.delegate = delegate;
	}

	synchronized boolean detach() {
		if (delegate == null)
			return false;
		delegate = null;
		return true;
	}

	@Override
	public void run() {
		Runnable task;
		synchronized (this) {
			task = delegate;
			delegate = null;
		}
		if (task != null)
			task.run();
	}
}
