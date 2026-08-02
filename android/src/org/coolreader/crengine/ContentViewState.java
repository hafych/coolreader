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

import android.view.View;

/**
 * Owns Activity content and decor views for one generation.
 *
 * Background updates and system-UI apply share one synchronized owner so
 * concurrent setContentView/theme paths cannot leave a mixed pair. Destroy
 * permanently closes the owner.
 */
final class ContentViewState {
	private View contentView;
	private View decorView;
	private boolean closed;

	synchronized void setContentView(View view) {
		if (closed)
			return;
		contentView = view;
	}

	synchronized View getContentView() {
		return closed ? null : contentView;
	}

	synchronized void setDecorView(View view) {
		if (closed)
			return;
		decorView = view;
	}

	synchronized View getDecorView() {
		return closed ? null : decorView;
	}

	synchronized boolean close() {
		if (closed)
			return false;
		closed = true;
		contentView = null;
		decorView = null;
		return true;
	}

	synchronized boolean isClosed() {
		return closed;
	}
}
