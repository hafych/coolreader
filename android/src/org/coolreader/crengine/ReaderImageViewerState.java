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
 * snapshots at that boundary and exact viewer identity for publication and
 * teardown. The fully constructed viewer and its initial geometry become
 * visible in one synchronized transition.
 */
final class ReaderImageViewerState<T> {
	private T current;
	private ImageInfo image;
	private boolean closed;

	synchronized boolean startIfIdle(
			T viewer, ImageInfo initialImage) {
		if (closed || current != null
				|| viewer == null || initialImage == null)
			return false;
		current = viewer;
		image = new ImageInfo(initialImage);
		return true;
	}

	synchronized T current() {
		return closed ? null : current;
	}

	synchronized boolean isActive(T viewer) {
		return !closed
				&& viewer != null
				&& current == viewer;
	}

	synchronized ImageInfo snapshot(T viewer) {
		if (!isActive(viewer))
			return null;
		return new ImageInfo(image);
	}

	synchronized ImageInfo snapshotForBuffer(
			T viewer, int width, int height) {
		if (!isActive(viewer))
			return null;
		ImageInfo updated = new ImageInfo(image);
		updated.bufWidth = Math.max(1, width);
		updated.bufHeight = Math.max(1, height);
		image = updated;
		return new ImageInfo(updated);
	}

	synchronized boolean update(
			T viewer, ImageInfo updatedImage) {
		if (!isActive(viewer) || updatedImage == null)
			return false;
		if (image.equals(updatedImage))
			return false;
		image = new ImageInfo(updatedImage);
		return true;
	}

	synchronized boolean finish(T viewer) {
		if (!isActive(viewer))
			return false;
		current = null;
		image = null;
		return true;
	}

	synchronized T close() {
		if (closed)
			return null;
		closed = true;
		T stopped = current;
		current = null;
		image = null;
		return stopped;
	}
}
