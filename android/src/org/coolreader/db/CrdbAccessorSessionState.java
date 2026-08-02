/*
 * CoolReader for Android
 * Copyright (C) 2026 CoolReader Next contributors
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 2
 * of the License, or (at your option) any later version.
 */

package org.coolreader.db;

import android.content.ServiceConnection;

import org.coolreader.crengine.MountPathCorrector;

/**
 * Owns the CRDB accessor live binding registration and path corrector.
 *
 * Bind/unbind and connect paths share one synchronized owner so a concurrent
 * unbind cannot leave a half-published connection. Close permanently drops
 * both handles.
 */
final class CrdbAccessorSessionState {
	static final class Binding {
		private final ServiceBindingState.Registration registration;
		private final ServiceConnection connection;

		Binding(
				ServiceBindingState.Registration registration,
				ServiceConnection connection) {
			this.registration = registration;
			this.connection = connection;
		}

		ServiceBindingState.Registration registration() {
			return registration;
		}

		ServiceConnection connection() {
			return connection;
		}

		boolean matches(ServiceBindingState.Registration other) {
			return registration == other;
		}
	}

	private Binding currentBinding;
	private MountPathCorrector pathCorrector;
	private boolean closed;

	CrdbAccessorSessionState(MountPathCorrector pathCorrector) {
		this.pathCorrector = pathCorrector;
	}

	synchronized void setPathCorrector(MountPathCorrector pathCorrector) {
		if (closed)
			return;
		this.pathCorrector = pathCorrector;
	}

	synchronized MountPathCorrector getPathCorrector() {
		return closed ? null : pathCorrector;
	}

	synchronized void setBinding(Binding binding) {
		if (closed)
			return;
		currentBinding = binding;
	}

	synchronized Binding getBinding() {
		return closed ? null : currentBinding;
	}

	/**
	 * Clears and returns the current binding when it matches registration.
	 */
	synchronized Binding takeIfMatches(
			ServiceBindingState.Registration registration) {
		if (closed || currentBinding == null)
			return null;
		if (!currentBinding.matches(registration))
			return null;
		Binding previous = currentBinding;
		currentBinding = null;
		return previous;
	}

	/**
	 * Clears the binding only when it matches registration; returns true if
	 * a matching binding was cleared (caller should unbind platform).
	 */
	synchronized boolean clearIfMatches(
			ServiceBindingState.Registration registration) {
		return takeIfMatches(registration) != null;
	}

	/**
	 * Clears any binding without permanent close (unbind path).
	 */
	synchronized Binding takeBinding() {
		if (closed)
			return null;
		Binding previous = currentBinding;
		currentBinding = null;
		return previous;
	}

	synchronized Binding close() {
		if (closed)
			return null;
		closed = true;
		Binding previous = currentBinding;
		currentBinding = null;
		pathCorrector = null;
		return previous;
	}

	synchronized boolean isClosed() {
		return closed;
	}
}
