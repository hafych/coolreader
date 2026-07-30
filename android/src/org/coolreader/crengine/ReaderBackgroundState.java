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
 * Owns one reader background snapshot and its render/replacement boundary.
 *
 * <p>Rendering runs while the state lock is held. Replacement and close can
 * therefore return the previous bitmap only after no renderer can still use
 * it. The caller remains responsible for releasing the returned resource.</p>
 */
final class ReaderBackgroundState<TTexture, TBitmap> {
	interface Renderer<TTexture, TBitmap> {
		void render(Snapshot<TTexture, TBitmap> snapshot);
	}

	static final class Snapshot<TTexture, TBitmap> {
		private final TTexture texture;
		private final TBitmap bitmap;
		private final boolean tiled;
		private final int color;

		private Snapshot(
				TTexture texture,
				TBitmap bitmap,
				boolean tiled,
				int color) {
			this.texture = texture;
			this.bitmap = bitmap;
			this.tiled = tiled;
			this.color = color;
		}

		TTexture texture() {
			return texture;
		}

		TBitmap bitmap() {
			return bitmap;
		}

		boolean isTiled() {
			return tiled;
		}

		int color() {
			return color;
		}
	}

	static final class Publication<TBitmap> {
		private final boolean accepted;
		private final TBitmap releasable;

		private Publication(
				boolean accepted,
				TBitmap releasable) {
			this.accepted = accepted;
			this.releasable = releasable;
		}

		boolean isAccepted() {
			return accepted;
		}

		TBitmap releasable() {
			return releasable;
		}
	}

	private Snapshot<TTexture, TBitmap> current;
	private boolean closed;

	ReaderBackgroundState(
			TTexture texture,
			TBitmap bitmap,
			boolean tiled,
			int color) {
		current = new Snapshot<>(
				texture, bitmap, tiled, color);
	}

	synchronized boolean needsReplacement(
			TTexture texture,
			boolean tiled,
			int color) {
		return !closed
				&& (!sameTexture(current.texture, texture)
				|| current.tiled != tiled
				|| current.color != color);
	}

	synchronized Publication<TBitmap> replace(
			TTexture texture,
			TBitmap bitmap,
			boolean tiled,
			int color) {
		if (closed
				|| sameTexture(current.texture, texture)
						&& current.tiled == tiled
						&& current.color == color) {
			return new Publication<>(
					false,
					current != null
							&& current.bitmap == bitmap
									? null : bitmap);
		}
		TBitmap previous = current.bitmap;
		current = new Snapshot<>(
				texture, bitmap, tiled, color);
		return new Publication<>(
				true,
				previous != bitmap ? previous : null);
	}

	synchronized boolean render(
			Renderer<TTexture, TBitmap> renderer) {
		if (renderer == null)
			throw new IllegalArgumentException(
					"renderer must not be null");
		if (closed)
			return false;
		renderer.render(current);
		return true;
	}

	synchronized TBitmap close() {
		if (closed)
			return null;
		closed = true;
		TBitmap bitmap = current.bitmap;
		current = null;
		return bitmap;
	}

	private static boolean sameTexture(
			Object first, Object second) {
		return first == second
				|| first != null && first.equals(second);
	}
}
