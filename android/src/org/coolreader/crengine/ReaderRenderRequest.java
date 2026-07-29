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
 * Captures the exact book identity and document interaction for one render.
 * A null book is a valid identity for the initial empty reader generation.
 */
final class ReaderRenderRequest {
	private final BookInfo expectedBook;
	private final DocumentLoadLifecycle.Interaction interaction;

	private ReaderRenderRequest(
			BookInfo expectedBook,
			DocumentLoadLifecycle.Interaction interaction) {
		this.expectedBook = expectedBook;
		this.interaction = interaction;
	}

	static ReaderRenderRequest capture(
			BookInfo expectedBook,
			DocumentLoadLifecycle lifecycle) {
		if (lifecycle == null)
			return null;
		DocumentLoadLifecycle.Interaction interaction =
				lifecycle.interaction();
		return fromInteraction(expectedBook, interaction);
	}

	static ReaderRenderRequest fromInteraction(
			BookInfo expectedBook,
			DocumentLoadLifecycle.Interaction interaction) {
		if (interaction == null)
			return null;
		return new ReaderRenderRequest(
				expectedBook, interaction);
	}

	boolean isCurrent(
			BookInfo currentBook,
			DocumentLoadLifecycle lifecycle) {
		return lifecycle != null
				&& currentBook == expectedBook
				&& lifecycle.isInteractionActive(interaction);
	}
}
