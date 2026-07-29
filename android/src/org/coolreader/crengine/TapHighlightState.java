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

final class TapHighlightState {
	private Object current;
	private Show visible;
	private boolean closed;

	synchronized Show requestShow(
			TapZoneGeometry.Bounds bounds,
			int color) {
		if (bounds == null)
			throw new IllegalArgumentException("bounds are required");
		if (closed || bounds.isEmpty())
			return null;
		Show show = new Show(bounds, color);
		current = show;
		return show;
	}

	synchronized Hide requestHideAll() {
		if (closed)
			return null;
		Hide hide = new Hide();
		current = hide;
		return hide;
	}

	synchronized Hide requestOwnedHide(Show owner) {
		if (closed || owner == null || current != owner)
			return null;
		Hide hide = new Hide();
		current = hide;
		return hide;
	}

	synchronized boolean isCurrent(Show show) {
		return !closed && show != null && current == show;
	}

	synchronized Transition applyShow(Show show) {
		if (!isCurrent(show))
			return null;
		Transition transition =
				new Transition(visible, show);
		visible = show;
		return transition;
	}

	synchronized Transition applyHide(Hide hide) {
		if (closed || hide == null || current != hide)
			return null;
		current = null;
		Transition transition =
				new Transition(visible, null);
		visible = null;
		return transition;
	}

	synchronized boolean isVisible(Show show) {
		return !closed && show != null && visible == show;
	}

	synchronized void invalidate() {
		if (closed)
			return;
		current = null;
		visible = null;
	}

	synchronized boolean close() {
		if (closed)
			return false;
		closed = true;
		current = null;
		visible = null;
		return true;
	}

	static final class Show {
		private final TapZoneGeometry.Bounds bounds;
		private final int color;

		private Show(
				TapZoneGeometry.Bounds bounds,
				int color) {
			this.bounds = bounds;
			this.color = color;
		}

		TapZoneGeometry.Bounds bounds() {
			return bounds;
		}

		int color() {
			return color;
		}
	}

	static final class Hide {
		private Hide() {
		}
	}

	static final class Transition {
		private final Show previous;
		private final Show current;

		private Transition(Show previous, Show current) {
			this.previous = previous;
			this.current = current;
		}

		Show previous() {
			return previous;
		}

		Show current() {
			return current;
		}

		boolean hasVisualChange() {
			return previous != current;
		}
	}
}
