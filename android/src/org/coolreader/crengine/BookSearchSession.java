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
 * Owns preview requests and the single terminal action of one book-search
 * dialog.
 */
final class BookSearchSession {
	private final CloseableTaskGate previewRequests =
			new CloseableTaskGate();
	private boolean closed;

	synchronized Preview replacePreview() {
		if (closed)
			return null;
		CloseableTaskGate.Token owner = previewRequests.replace();
		return owner != null ? new Preview(owner) : null;
	}

	synchronized boolean isPreviewActive(Preview preview) {
		return !closed && preview != null
				&& previewRequests.isActive(preview.owner);
	}

	synchronized boolean completePreview(Preview preview) {
		return !closed && preview != null
				&& previewRequests.complete(preview.owner);
	}

	synchronized boolean submit() {
		return close();
	}

	synchronized boolean cancel() {
		return close();
	}

	synchronized boolean isClosed() {
		return closed;
	}

	private boolean close() {
		if (closed)
			return false;
		closed = true;
		previewRequests.close();
		return true;
	}

	static final class Preview {
		private final CloseableTaskGate.Token owner;

		private Preview(CloseableTaskGate.Token owner) {
			this.owner = owner;
		}
	}
}
