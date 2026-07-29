/*
 * CoolReader for Android
 * Copyright (C) 2026 CoolReader Next contributors
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 2
 * of the License, or (at your option) any later version.
 */

package org.coolreader.crengine;

final class ProgressUiState {
	private Token current;
	private Token visible;
	private boolean closed;

	synchronized Token requestShow() {
		if (closed)
			return null;
		current = new Token();
		return current;
	}

	synchronized Token requestHideAll() {
		if (closed)
			return null;
		current = new Token();
		return current;
	}

	synchronized OwnedHide requestOwnedHide(Token owner) {
		if (closed || owner == null || current != owner)
			return null;
		Token request = new Token();
		current = request;
		return new OwnedHide(request, owner);
	}

	synchronized boolean isCurrent(Token request) {
		return !closed && current == request;
	}

	synchronized boolean markVisible(Token request) {
		if (!isCurrent(request))
			return false;
		visible = request;
		return true;
	}

	synchronized void markShowFailed(Token request) {
		if (current == request) {
			current = null;
			visible = null;
		}
	}

	synchronized boolean markDismissed(Token owner) {
		if (closed || visible != owner)
			return false;
		visible = null;
		if (current == owner)
			current = null;
		return true;
	}

	synchronized boolean applyHideAll(Token request) {
		if (!isCurrent(request))
			return false;
		current = null;
		visible = null;
		return true;
	}

	synchronized boolean applyOwnedHide(OwnedHide hide) {
		if (hide == null || !isCurrent(hide.request))
			return false;
		current = null;
		if (visible != hide.owner)
			return false;
		visible = null;
		return true;
	}

	synchronized boolean isVisible() {
		return visible != null;
	}

	synchronized boolean close() {
		if (closed)
			return false;
		closed = true;
		current = null;
		visible = null;
		return true;
	}

	static final class Token {
		private Token() {
		}
	}

	static final class OwnedHide {
		private final Token request;
		private final Token owner;

		private OwnedHide(Token request, Token owner) {
			this.request = request;
			this.owner = owner;
		}
	}
}
