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

import org.coolreader.crengine.DocumentSource;

/**
 * Owns the Activity-scoped external document open identity.
 *
 * Intent processing may bind one pending external source before load starts.
 * Resume and cloud-sync decisions read that identity without racing a parallel
 * mutable field. Activity destruction permanently closes the owner.
 */
final class ExternalDocumentState {
	private DocumentSource source;
	private boolean closed;

	synchronized void clear() {
		if (!closed)
			source = null;
	}

	synchronized DocumentSource set(DocumentSource next) {
		if (closed)
			return null;
		source = next;
		return source;
	}

	synchronized DocumentSource get() {
		return closed ? null : source;
	}

	synchronized boolean isPresent() {
		return !closed && source != null;
	}

	synchronized DocumentSource close() {
		if (closed)
			return null;
		closed = true;
		DocumentSource previous = source;
		source = null;
		return previous;
	}

	synchronized boolean isClosed() {
		return closed;
	}
}
