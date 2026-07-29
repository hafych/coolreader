/*
 * CoolReader for Android
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 2
 * of the License, or (at your option) any later version.
 */

package org.coolreader.tts;

/**
 * Owns the latest asynchronous TextToSpeech initialization attempt.
 */
final class TtsInitializationState {
	private Request current;
	private boolean closed;

	synchronized Request replace(String engine) {
		if (closed)
			return null;
		current = new Request(engine);
		return current;
	}

	synchronized boolean isActive(Request request) {
		return !closed && request != null && current == request;
	}

	synchronized boolean complete(Request request) {
		if (!isActive(request))
			return false;
		current = null;
		return true;
	}

	synchronized boolean close() {
		if (closed)
			return false;
		closed = true;
		current = null;
		return true;
	}

	synchronized boolean isClosed() {
		return closed;
	}

	static final class Request {
		private final String engine;

		private Request(String engine) {
			this.engine = engine;
		}

		String engine() {
			return engine;
		}
	}
}
