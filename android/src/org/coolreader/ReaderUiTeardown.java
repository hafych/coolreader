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

import org.coolreader.crengine.ReaderView;

import java.util.function.Consumer;

/**
 * CoolReader {@code onDestroy} order for reader UI ownership.
 *
 * <p>{@link ReaderUiOwner#close()} permanently drops accessors, so a later
 * {@code readerUi.view()} lookup is always null. Native teardown must use the
 * identity returned from {@link #closeForDestroy(ReaderUiOwner)}.
 */
final class ReaderUiTeardown {
	private ReaderUiTeardown() {
	}

	/**
	 * Permanently closes ownership and returns the exact view that must
	 * receive {@link ReaderView#destroy()}. After this call,
	 * {@link ReaderUiOwner#view()} is null.
	 */
	static ReaderView closeForDestroy(ReaderUiOwner owner) {
		if (owner == null)
			return null;
		return owner.close();
	}

	/**
	 * Invokes destroy on the closed-out identity from
	 * {@link #closeForDestroy(ReaderUiOwner)}. Never pass a post-close
	 * {@link ReaderUiOwner#view()} result — that skips native teardown.
	 */
	static void destroyClosedOut(
			ReaderView closedOut,
			Consumer<ReaderView> destroy) {
		if (closedOut == null || destroy == null)
			return;
		destroy.accept(closedOut);
	}
}
