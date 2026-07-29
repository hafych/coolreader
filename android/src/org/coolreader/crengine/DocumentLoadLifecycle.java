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
 * Owns the latest end-to-end document-open request for one Activity/Reader.
 * The interaction identity separately owns delayed work against the selected
 * document and survives completion only after that request was published.
 */
public final class DocumentLoadLifecycle {
	private Request current;
	private Interaction interaction = new Interaction();
	private boolean closed;
	private boolean published;

	public synchronized Request replace() {
		if (closed)
			return null;
		current = new Request();
		interaction = new Interaction();
		published = false;
		return current;
	}

	public synchronized boolean isActive(Request request) {
		return !closed && request != null && current == request;
	}

	public synchronized Interaction interaction() {
		return closed ? null : interaction;
	}

	public synchronized boolean isInteractionActive(
			Interaction candidate) {
		return !closed && candidate != null
				&& interaction == candidate;
	}

	public synchronized boolean complete(Request request) {
		if (!isActive(request))
			return false;
		if (!published)
			interaction = new Interaction();
		current = null;
		published = false;
		return true;
	}

	public synchronized void cancel() {
		current = null;
		if (!closed)
			interaction = new Interaction();
		published = false;
	}

	public synchronized boolean markPublished(Request request) {
		if (!isActive(request))
			return false;
		published = true;
		return true;
	}

	public synchronized boolean cancelPending() {
		if (closed)
			return false;
		interaction = new Interaction();
		if (current == null || published)
			return false;
		current = null;
		published = false;
		return true;
	}

	public synchronized boolean close() {
		if (closed)
			return false;
		closed = true;
		current = null;
		interaction = null;
		published = false;
		return true;
	}

	public synchronized boolean isClosed() {
		return closed;
	}

	public static final class Request {
		private Request() {
		}
	}

	public static final class Interaction {
		private Interaction() {
		}
	}
}
