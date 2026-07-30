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
 * Owns one background settings application for a document interaction.
 *
 * <p>The request snapshots only immutable document metadata. It deliberately
 * does not retain {@link BookInfo}, because stream reconciliation can replace
 * that identity without replacing the native document interaction.</p>
 */
final class ReaderSettingsApplyRequest {
	private final DocumentLoadLifecycle.Interaction interaction;
	private final String bookLanguage;

	private ReaderSettingsApplyRequest(
			DocumentLoadLifecycle.Interaction interaction,
			String bookLanguage) {
		this.interaction = interaction;
		this.bookLanguage = bookLanguage;
	}

	static ReaderSettingsApplyRequest capture(
			BookInfo bookInfo,
			DocumentLoadLifecycle lifecycle) {
		if (lifecycle == null)
			return null;
		return fromInteraction(
				bookInfo, lifecycle.interaction());
	}

	static ReaderSettingsApplyRequest fromInteraction(
			BookInfo bookInfo,
			DocumentLoadLifecycle.Interaction interaction) {
		if (interaction == null)
			return null;
		FileInfo fileInfo =
				bookInfo != null ? bookInfo.getFileInfo() : null;
		return fromSnapshot(
				fileInfo != null ? fileInfo.getLanguage() : null,
				interaction);
	}

	static ReaderSettingsApplyRequest fromSnapshot(
			String bookLanguage,
			DocumentLoadLifecycle.Interaction interaction) {
		return interaction != null
				? new ReaderSettingsApplyRequest(
						interaction, bookLanguage)
				: null;
	}

	boolean isCurrent(DocumentLoadLifecycle lifecycle) {
		return lifecycle != null
				&& lifecycle.isInteractionActive(interaction);
	}

	String bookLanguage(DocumentLoadLifecycle lifecycle) {
		return isCurrent(lifecycle) ? bookLanguage : null;
	}

	ReaderRenderRequest renderRequest(
			BookInfo currentBook,
			DocumentLoadLifecycle lifecycle) {
		return isCurrent(lifecycle)
				? ReaderRenderRequest.fromInteraction(
						currentBook, interaction)
				: null;
	}
}
