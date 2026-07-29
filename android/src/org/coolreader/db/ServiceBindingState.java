/*
 * CoolReader for Android
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 2
 * of the License, or (at your option) any later version.
 */

package org.coolreader.db;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Owns one exact platform-service registration and its pending callbacks.
 */
final class ServiceBindingState<T> {
	static final class Registration {
		private final ArrayList<Runnable> callbacks = new ArrayList<>();

		private Registration() {
		}
	}

	static final class BindRequest {
		private final Registration registration;
		private final boolean startBinding;
		private final Runnable immediateCallback;

		private BindRequest(
				Registration registration,
				boolean startBinding,
				Runnable immediateCallback) {
			this.registration = registration;
			this.startBinding = startBinding;
			this.immediateCallback = immediateCallback;
		}

		Registration getRegistration() {
			return registration;
		}

		boolean shouldStartBinding() {
			return startBinding;
		}

		Runnable getImmediateCallback() {
			return immediateCallback;
		}
	}

	private Registration current;
	private T binder;

	synchronized BindRequest requestBind(Runnable callback) {
		if (binder != null)
			return new BindRequest(current, false, callback);
		if (current != null) {
			if (callback != null)
				current.callbacks.add(callback);
			return new BindRequest(current, false, null);
		}
		current = new Registration();
		if (callback != null)
			current.callbacks.add(callback);
		return new BindRequest(current, true, null);
	}

	synchronized boolean isCurrent(Registration registration) {
		return registration != null && current == registration;
	}

	synchronized List<Runnable> connected(
			Registration registration,
			T connectedBinder) {
		if (!isCurrent(registration) || connectedBinder == null)
			return Collections.emptyList();
		binder = connectedBinder;
		List<Runnable> callbacks =
				new ArrayList<>(registration.callbacks);
		registration.callbacks.clear();
		return callbacks;
	}

	synchronized boolean disconnected(Registration registration) {
		if (!isCurrent(registration))
			return false;
		binder = null;
		return true;
	}

	synchronized boolean bindingFailed(Registration registration) {
		if (!isCurrent(registration))
			return false;
		registration.callbacks.clear();
		current = null;
		binder = null;
		return true;
	}

	synchronized Registration unbind() {
		Registration registration = current;
		if (registration != null)
			registration.callbacks.clear();
		current = null;
		binder = null;
		return registration;
	}

	synchronized T getBinder() {
		return binder;
	}

	synchronized boolean isConnected(
			Registration registration,
			T connectedBinder) {
		return isCurrent(registration)
				&& binder != null
				&& binder == connectedBinder;
	}
}
