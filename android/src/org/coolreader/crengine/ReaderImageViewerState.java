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
 * Owns the mutable geometry of one full-screen image-viewer session.
 *
 * Native image operations are serialized by the Engine queue, while gestures
 * update the requested geometry on the GUI thread. This owner provides copied
 * snapshots at that boundary and exact session identity for replacement and
 * teardown.
 */
final class ReaderImageViewerState {
	static final class Session {
		private Session() {
		}
	}

	private Session current;
	private ImageInfo image;
	private boolean closed;

	synchronized Session replace(ImageInfo initialImage) {
		if (closed || initialImage == null)
			return null;
		Session session = new Session();
		current = session;
		image = new ImageInfo(initialImage);
		return session;
	}

	synchronized boolean isActive(Session session) {
		return session != null && current == session && !closed;
	}

	synchronized ImageInfo snapshot(Session session) {
		if (!isActive(session))
			return null;
		return new ImageInfo(image);
	}

	synchronized ImageInfo snapshotForBuffer(
			Session session, int width, int height) {
		if (!isActive(session))
			return null;
		ImageInfo updated = new ImageInfo(image);
		updated.bufWidth = Math.max(1, width);
		updated.bufHeight = Math.max(1, height);
		image = updated;
		return new ImageInfo(updated);
	}

	synchronized boolean update(
			Session session, ImageInfo updatedImage) {
		if (!isActive(session) || updatedImage == null)
			return false;
		if (image.equals(updatedImage))
			return false;
		image = new ImageInfo(updatedImage);
		return true;
	}

	synchronized boolean finish(Session session) {
		if (!isActive(session))
			return false;
		current = null;
		image = null;
		return true;
	}

	synchronized void close() {
		closed = true;
		current = null;
		image = null;
	}
}
