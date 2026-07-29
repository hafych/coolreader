/*
 * CoolReader for Android
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 2
 * of the License, or (at your option) any later version.
 */

package org.coolreader.crengine;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Owns the latest Activity-level TTS initialization and its detachable
 * terminal callbacks.
 */
public final class TtsInitializationSession {
	private Request current;
	private CallbackOwner currentCallbacks;
	private boolean closed;

	public synchronized Replacement replace(
			ServiceLifecycle lifecycle,
			String engine,
			Runnable successCallback,
			Runnable failureCallback) {
		if (closed
				|| lifecycle == null
				|| !lifecycle.isActive())
			return null;
		Cancellation cancellation =
				current != null
						? currentCallbacks.detachCancellation(
								current.lifecycle)
						: null;
		current = new Request(lifecycle, engine);
		currentCallbacks = new CallbackOwner(
				successCallback,
				failureCallback);
		return new Replacement(current, cancellation);
	}

	public synchronized boolean isActive(Request request) {
		return !closed
				&& request != null
				&& current == request;
	}

	public synchronized Completion complete(Request request) {
		if (!isActive(request))
			return null;
		CallbackOwner callbacks = currentCallbacks;
		current = null;
		currentCallbacks = null;
		return callbacks.detachCompletion();
	}

	public synchronized Cancellation cancel() {
		if (closed || current == null)
			return null;
		Request canceled = current;
		CallbackOwner callbacks = currentCallbacks;
		current = null;
		currentCallbacks = null;
		return callbacks.detachCancellation(
				canceled.lifecycle);
	}

	public synchronized boolean close() {
		if (closed)
			return false;
		closed = true;
		if (currentCallbacks != null)
			currentCallbacks.clear();
		current = null;
		currentCallbacks = null;
		return true;
	}

	public synchronized boolean isClosed() {
		return closed;
	}

	public enum Outcome {
		CREATED,
		FAILED,
		TIMED_OUT,
	}

	public static final class Request {
		private final ServiceLifecycle lifecycle;
		private final String engine;

		private Request(
				ServiceLifecycle lifecycle,
				String engine) {
			this.lifecycle = lifecycle;
			this.engine = engine;
		}

		public ServiceLifecycle getLifecycle() {
			return lifecycle;
		}

		public String getEngine() {
			return engine;
		}
	}

	public static final class Replacement {
		private final Request current;
		private final Cancellation cancellation;

		private Replacement(
				Request current,
				Cancellation cancellation) {
			this.current = current;
			this.cancellation = cancellation;
		}

		public Request getCurrent() {
			return current;
		}

		public Cancellation getCancellation() {
			return cancellation;
		}
	}

	public static final class Cancellation {
		private final ServiceLifecycle lifecycle;
		private final Runnable callback;
		private final AtomicBoolean claimed =
				new AtomicBoolean();

		private Cancellation(
				ServiceLifecycle lifecycle,
				Runnable callback) {
			this.lifecycle = lifecycle;
			this.callback = callback;
		}

		public ServiceLifecycle getLifecycle() {
			return lifecycle;
		}

		public boolean run() {
			if (!claimed.compareAndSet(false, true))
				return false;
			if (callback != null)
				callback.run();
			return true;
		}
	}

	public static final class Completion {
		private final Runnable successCallback;
		private final Runnable failureCallback;
		private final AtomicBoolean claimed =
				new AtomicBoolean();

		private Completion(
				Runnable successCallback,
				Runnable failureCallback) {
			this.successCallback = successCallback;
			this.failureCallback = failureCallback;
		}

		public boolean runSuccess() {
			return run(successCallback);
		}

		public boolean runFailure() {
			return run(failureCallback);
		}

		private boolean run(Runnable callback) {
			if (!claimed.compareAndSet(false, true))
				return false;
			if (callback != null)
				callback.run();
			return true;
		}
	}

	private static final class CallbackOwner {
		private Runnable successCallback;
		private Runnable failureCallback;

		private CallbackOwner(
				Runnable successCallback,
				Runnable failureCallback) {
			this.successCallback = successCallback;
			this.failureCallback = failureCallback;
		}

		private Completion detachCompletion() {
			Completion completion = new Completion(
					successCallback,
					failureCallback);
			clear();
			return completion;
		}

		private Cancellation detachCancellation(
				ServiceLifecycle lifecycle) {
			Runnable callback = failureCallback;
			clear();
			return callback != null
					? new Cancellation(lifecycle, callback)
					: null;
		}

		private void clear() {
			successCallback = null;
			failureCallback = null;
		}
	}
}
