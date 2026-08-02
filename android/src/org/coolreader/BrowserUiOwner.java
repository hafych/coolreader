/*
 * CoolReader for Android
 * Copyright (C) 2026 CoolReader Next contributors
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 2
 * of the License, or (at your option) any later version.
 */

package org.coolreader;

import android.view.View;

import org.coolreader.crengine.BrowserViewLayout;
import org.coolreader.crengine.CRToolBar;
import org.coolreader.crengine.FileBrowser;

/**
 * Owns the Activity file-browser shell (browser, chrome and frame).
 *
 * Install publishes a fully built quartet. Destroy closes ownership and
 * returns the browser for explicit onClose cleanup.
 */
final class BrowserUiOwner {
	private FileBrowser browser;
	private View titleBar;
	private CRToolBar toolBar;
	private BrowserViewLayout frame;
	private boolean closed;

	synchronized boolean install(
			FileBrowser browser,
			View titleBar,
			CRToolBar toolBar,
			BrowserViewLayout frame) {
		if (closed
				|| browser == null
				|| titleBar == null
				|| toolBar == null
				|| frame == null
				|| this.browser != null)
			return false;
		this.browser = browser;
		this.titleBar = titleBar;
		this.toolBar = toolBar;
		this.frame = frame;
		return true;
	}

	synchronized FileBrowser browser() {
		return closed ? null : browser;
	}

	synchronized View titleBar() {
		return closed ? null : titleBar;
	}

	synchronized CRToolBar toolBar() {
		return closed ? null : toolBar;
	}

	synchronized BrowserViewLayout frame() {
		return closed ? null : frame;
	}

	synchronized boolean isPresent() {
		return !closed && frame != null;
	}

	synchronized FileBrowser close() {
		if (closed)
			return null;
		closed = true;
		FileBrowser previous = browser;
		browser = null;
		titleBar = null;
		toolBar = null;
		frame = null;
		return previous;
	}

	synchronized boolean isClosed() {
		return closed;
	}
}
